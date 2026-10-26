# Plan contract: lab-5-impl-cancel-order

> Disk source of truth. Re-read before each decision and after any compaction.

**Goal:** Реализовать метод `boolean cancelOrder(String orderCode)` в `pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java` (сейчас — заглушка `return false`, строки 54–57) по спецификации из `tasks/lab-5-impl-cancel-order.md`: получить заказ по коду из БД; вернуть `false`, если заказ не найден или его статус не `ready`; иначе перевести заказ в статус `cancelled` и вернуть `true`. Постусловие: изменяется **только** колонка `status`; `issued_at` не заполняется. Слой репозитория — только сохранение значения, без логики. **Тесты писать не надо** (прямое требование задачи) — покрытие дырой для `cancelOrder` остаётся осознанно принятым человеком.

**Ключевые факты разведки (wave 1+2):**
- Проект: модуль **`pvz/`** — отдельный Gradle-билд (корневой `settings.gradle.kts` включает только `todo-app`). Spring Boot 4.1.1, Java 25, **Jdbi 3.52 SQL-object** + Postgres 16 (Testcontainers в интеграционных тестах, авто-скип без Docker).
- Стаб для замены (OrderServiceImpl.java:54–57): `@Override public boolean cancelOrder(String orderCode) { return false; }` — без `@Transactional`.
- Образец-шаблон: `issueOrder` (OrderServiceImpl.java:35–52) — `@Transactional` → `orderRepository.findByCode(orderCode)` (`Optional`, пусто → `false`) → guard `status.equals(OrderStatus.READY.name().toLowerCase(Locale.ROOT))` → update → `return updated > 0`.
- Статусы в БД — строки нижнего регистра (`ready`/`issued`/`cancelled`); в коде получаются через `OrderStatus.X.name().toLowerCase(Locale.ROOT)`; enum `OrderStatus` в `pvz/src/main/java/io/allitov/pvz/model/OrderStatus.java` (`READY`, `ISSUED`, `CANCELLED`).
- Репозиторий `pvz/src/main/java/io/allitov/pvz/repository/OrderRepository.java` — интерфейс Jdbi (прокси `jdbi.onDemand`, бин в `JdbiRepositoryConfig`); стиль: одиночный `@SqlUpdate("...")`, параметры `@Bind("...")`, `int` = число обновлённых строк. Точка вставки нового метода — сразу после `updateStatusAndIssuedAt` (строка 65, перед `deleteByStatuses`).
- **Вызов `cancelOrder` — единственный:** `pvz/src/main/java/io/allitov/pvz/web/OrderController.java:185` (обработчик `cancel`, POST `/orders/{code}/cancel`, строки 178–193) уже ветвится по boolean (true → flash «Отменён …», false → flash «Отменить можно только заказ „К выдаче"»). Веб-путь полностью подключён — изменения контроллера/шаблонов **не нужны**.
- Сигнатура в интерфейсе `pvz/src/main/java/io/allitov/pvz/service/OrderService.java:25` уже есть (`boolean cancelOrder(String orderCode);`), Javadoc на ней отсутствует (у соседнего `issueOrder` — полный Javadoc; стиль дома — документировать контракт).
- Покрытие: в `src/test` нет ни одного упоминания cancel (grep: 0 совпадений) — ветка без покрытия; по требованию задачи тесты не пишем, флаг для человека.

**active:** — (все задачи закрыты, 2026-10-26)

## open_questions

- [x] Q1: писать статус через новый узкий метод репозитория `updateStatus(code, status)` или переиспользовать `updateStatusAndIssuedAt(code, "cancelled", null)`? — answer: **Новый `updateStatus`** (решение человека, 2026-10-26): UPDATE затрагивает только колонку `status`, что буквально соответствует постусловию; изменение чисто аддитивное, `issueOrder` не затрагивается.

## backlog

- [x] **Task1** — Новый метод `updateStatus` в `OrderRepository`
    - **agent:** coder
    - **inputs:**
        - Файл: `pvz/src/main/java/io/allitov/pvz/repository/OrderRepository.java`.
        - Добавить сразу после `updateStatusAndIssuedAt` (после строки 65, перед `deleteByStatuses`), в точном стиле соседних методов (одиночная строка `@SqlUpdate`, `@Bind`, `int`-возврат):
          ```java
          /**
           * Обновляет только статус заказа по его коду; остальные колонки не изменяются.
           *
           * @param code   код заказа
           * @param status новый статус (ключ БД, нижний регистр)
           * @return число обновлённых строк (0 или 1)
           */
          @SqlUpdate("update orders set status = :status where order_code = :code")
          int updateStatus(@Bind("code") String code, @Bind("status") String status);
          ```
          (Javadoc в стиле `updateStatusAndIssuedAt`, который тоже документирован; формулировку можно слегка отшлифовать, суть — «только статус».)
        - В SQL **не добавлять** условие `and status = 'ready'` — проверку статуса делает сервис (требование спеки: репозиторий только сохраняет значение).
        - Контекст: интерфейс Jdbi SQL-object, прокси создаётся в `pvz/src/main/java/io/allitov/pvz/config/JdbiRepositoryConfig.java:11-14`; никаких правок конфигов не требуется. Изменение чисто аддитивное — существующих вызывов метода нет.
    - **deps:** []
    - **acceptance:** В `OrderRepository` существует метод `int updateStatus(String, String)` с `@SqlUpdate("update orders set status = :status where order_code = :code")`; `cd pvz && gradlew.bat compileJava` проходит без ошибок; никакой другой файл не изменён.
    - **description:** Добавить в `OrderRepository` новый метод `updateStatus` (только колонка `status`), разместить после `updateStatusAndIssuedAt`, стиль — как у соседей.
    - **touched:** `pvz/src/main/java/io/allitov/pvz/repository/OrderRepository.java` (+10 строк: Javadoc + `@SqlUpdate`-метод `updateStatus`); review PASS — `opencode-reviews/task1-updatestatus.code-review.md`
- [x] **Task2** — Реализация `cancelOrder` в `OrderServiceImpl`
    - **agent:** coder
    - **inputs:**
        - Файл: `pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java`; заменить стаб (строки 54–57):
          ```java
          @Override
          public boolean cancelOrder(String orderCode) {
              return false;
          }
          ```
        - Шаблон для зеркалирования — `issueOrder` (строки 35–52, точный код):
          ```java
          @Override
          @Transactional
          public boolean issueOrder(String orderCode) {
              Optional<Order> found = orderRepository.findByCode(orderCode);
              if (found.isEmpty()) {
                  return false;
              }
              String ready = OrderStatus.READY.name().toLowerCase(Locale.ROOT);
              if (!found.get().getStatus().equals(ready)) {
                  return false;
              }
              int updated = orderRepository.updateStatusAndIssuedAt(
                  orderCode,
                  OrderStatus.ISSUED.name().toLowerCase(Locale.ROOT),
                  Instant.now()
              );
              return updated > 0;
          }
          ```
        - Требуемая реализация (зеркало `issueOrder`, но: update — через `updateStatus` из Task1, `issued_at` не трогается):
          ```java
          @Override
          @Transactional
          public boolean cancelOrder(String orderCode) {
              Optional<Order> found = orderRepository.findByCode(orderCode);
              if (found.isEmpty()) {
                  return false;
              }
              String ready = OrderStatus.READY.name().toLowerCase(Locale.ROOT);
              if (!found.get().getStatus().equals(ready)) {
                  return false;
              }
              int updated = orderRepository.updateStatus(
                  orderCode,
                  OrderStatus.CANCELLED.name().toLowerCase(Locale.ROOT)
              );
              return updated > 0;
          }
          ```
          Новых импортов не требуется (`Optional`, `OrderStatus`, `Locale`, `@Transactional` уже импортированы).
        - Также добавить Javadoc к объявлению в интерфейсе `pvz/src/main/java/io/allitov/pvz/service/OrderService.java:25` (сейчас объявление голое) — по образцу Javadoc `issueOrder` (строки 11–22): «Отменяет заказ: переводит заказ в статус `cancelled`; дата выдачи (`issued_at`) не заполняется. Отмена возможна только для заказа в статусе `ready`; для отсутствующего кода или другого статуса изменения не производятся. `@return` `true` при успехе, `false`, если заказ не найден или его статус отличается от `ready`.»
        - Caller (не менять, только знать): `pvz/src/main/java/io/allitov/pvz/web/OrderController.java:185` — единственный вызов, ветвится по boolean; изменений в web-слое не требуется. Вызовов `< 5` — декомпозиция не нужна.
        - Покрытие: тестов на `cancelOrder` нет и **писать их запрещено условием задачи** — после изменения прогнать существующий юнит-тест для регресс-проверки.
        - Сборка: модуль `pvz` — отдельный Gradle-билд; команды выполнять из каталога `pvz/` (на Windows `gradlew.bat`). Интеграционные тесты требуют Docker и авто-скипаются без него.
    - **deps:** [Task1]
    - **acceptance:** Стаб заменён реализацией с `@Transactional`, guard-ом на `ready` и вызовом `orderRepository.updateStatus(...)`; `cd pvz && gradlew.bat compileJava` проходит; `cd pvz && gradlew.bat test --tests "*OrderServiceImplTest"` зелёный (Mockito, без Docker); coder называет, какой тест покрывает изменённую ветку (ожидаемый честный ответ: «никакой — покрытия нет, тесты запрещены задачей» — фиксируется как принятый человеком риск, не как молчаливый зелёный).
    - **description:** Реализовать `cancelOrder` по спеке (SELECT статуса → guard `ready` → UPDATE только статуса в `cancelled`), добавить Javadoc контракта в интерфейс.
    - **touched:** `pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java` (стаб заменён реализацией, +15/−1); `pvz/src/main/java/io/allitov/pvz/service/OrderService.java` (Javadoc к `cancelOrder`, +12); review PASS — `opencode-reviews/task2-cancelorder.code-review.md`
