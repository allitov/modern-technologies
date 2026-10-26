# Plan contract: lab-5-impl-build-report

> Disk source of truth. Re-read before each decision and after any compaction.

**Goal:** Реализовать метод `Report buildReport()` в `OrderServiceImpl` (модуль `pvz`): для каждого из трёх статусов —
`ready`, `issued`, `cancelled` — выполнить один запрос счёта к таблице `orders` через уже существующий
`OrderRepository.countByStatuses(Collection<String>)` и вернуть заполненную через `Report.builder()` структуру
`Report` (поля `readyCount`, `issuedCount`, `cancelledCount`). БД только читается (постусловие «данные не изменены»).
Слой репозитория не меняется вовсе. Тесты не пишутся (явный запрет в задаче). Дополнительно закрыть единственный
метод интерфейса `OrderService.buildReport()` без Javadoc — добавить Javadoc в стиле файла (по-русски).
Проверка дееспособности: `/report` перестаёт падать с 500 из-за `report == null` (шаблон `templates/report.html`
разыменовывает `report.readyCount/issuedCount/cancelledCount`).

**active:** — (план выполнен; Task1 закрыт 2026-10-26)

## open_questions

- [x] Q1: Как считать по каждому статусу — переиспользовать `countByStatuses` или добавить `countByStatus(String)` в
  репозиторий? — answer: Переиспользовать `countByStatuses`, вызывая его трижды с одним статусом (`List.of(status)`);
  репозиторий не трогать.
- [x] Q2: Добавить отсутствующий Javadoc к `OrderService.buildReport()`? — answer: Да, добавить по-русски, в стиле
  остальных методов интерфейса.

## backlog

- [x] **Task1** — Реализовать `buildReport()` в `OrderServiceImpl` (+ Javadoc в `OrderService`)
    - **agent:** coder
    - **inputs:**
        - Файл реализации: `pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java`, заглушка на стр. 72–75:
          ```java
          @Override
          public Report buildReport() {
              return null;
          }
          ```
        - Интерфейс: `pvz/src/main/java/io/allitov/pvz/service/OrderService.java`, стр. 39 `Report buildReport();` —
          единственный метод без Javadoc; все остальные методы задокументированы по-русски.
        - Репозиторий (НЕ МЕНЯТЬ): `pvz/src/main/java/io/allitov/pvz/repository/OrderRepository.java`, стр. 80–81:
          `@SqlQuery("select count(*) from orders where status in (<statuses>)")
          int countByStatuses(@BindList("statuses") Collection<String> statuses);`
        - Модель: `pvz/src/main/java/io/allitov/pvz/model/Report.java` — `@Data @Builder`, поля
          `int readyCount, issuedCount, cancelledCount`; `pvz/src/main/java/io/allitov/pvz/model/OrderStatus.java` —
          enum `READY, ISSUED, CANCELLED`; ключи БД — lowercase-имена констант.
        - Стиль класса (обязательно соблюсти): статусы как `OrderStatus.X.name().toLowerCase(Locale.ROOT)` —
          локальная переменная, как в `issueOrder` (стр. 42), а не inline в аргументах; read-only методы БЕЗ
          `@Transactional` (как соседний `countClosedOrders`, стр. 97–100: `return
          orderRepository.countByStatuses(CLOSED_STATUSES);`); 4 пробела, пустая строка между методами.
        - Импорты в `OrderServiceImpl` уже есть: `Report`, `OrderStatus`, `List`, `Locale` — новых импортов не нужно.
        - Целевой вид метода (ориентир, отступы выверить по файлу):
          ```java
          @Override
          public Report buildReport() {
              String ready = OrderStatus.READY.name().toLowerCase(Locale.ROOT);
              String issued = OrderStatus.ISSUED.name().toLowerCase(Locale.ROOT);
              String cancelled = OrderStatus.CANCELLED.name().toLowerCase(Locale.ROOT);
              return Report.builder()
                  .readyCount(orderRepository.countByStatuses(List.of(ready)))
                  .issuedCount(orderRepository.countByStatuses(List.of(issued)))
                  .cancelledCount(orderRepository.countByStatuses(List.of(cancelled)))
                  .build();
          }
          ```
        - Целевой Javadoc в `OrderService` (по-русски, в стиле файла):
          ```java
          /**
           * Формирует сводный отчёт по количеству заказов в каждом из статусов:
           * {@code ready}, {@code issued} и {@code cancelled}.
           *
           * @return заполненная структура {@link Report} с тремя счётчиками
           */
          Report buildReport();
          ```
        - Ограничения задачи: тесты НЕ писать (`src/test` — deny); слой репозитория не менять и не добавлять в него
          логику; запросов к БД ровно три, по одному на статус; данные в БД не изменяются.
    - **deps:** []
    - **acceptance:** `gradlew :pvz:compileJava` проходит без ошибок; в `OrderServiceImpl.buildReport()` нет
      `return null` — метод возвращает `Report.builder()...build()` с тремя значениями из трёх вызовов
      `orderRepository.countByStatuses(...)` (по одному на `ready`/`issued`/`cancelled`); файл
      `OrderRepository.java` не изменён (проверка по diff); `OrderService.buildReport()` имеет Javadoc; ни один файл
      в `src/test` не изменён.
    - **description:** Заменить заглушку `buildReport()` в `OrderServiceImpl` (стр. 72–75) на реализацию: вывести
      три статус-строки через `OrderStatus.X.name().toLowerCase(Locale.ROOT)`, трижды вызвать
      `orderRepository.countByStatuses(List.of(status))` — по одному запросу на статус — и собрать результат через
      `Report.builder()` (поля `readyCount`, `issuedCount`, `cancelledCount`). Добавить Javadoc к объявлению
      `buildReport()` в `OrderService`. Больше ничего не менять.
    - **touched:**
        - `pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java` — заглушка `buildReport()`
          (бывш. :72–75) заменена реализацией (:72–82)
        - `pvz/src/main/java/io/allitov/pvz/service/OrderService.java` — добавлен Javadoc к `buildReport()`
          (:39–45)
    - **result (2026-10-26):** PASS. Компиляция: `pvz` — самостоятельная Gradle-сборка (корневой `settings.gradle.kts`
      включает только `todo-app`), фактическая команда `.\gradlew.bat compileJava` в `pvz\` → BUILD SUCCESSFUL
      (acceptance-формула `:pvz:compileJava` из корня неприменима структурно). Regression (unit-only):
      `.\gradlew.bat test --tests "io.allitov.pvz.service.impl.OrderServiceImplTest" --tests "io.allitov.pvz.web.OrderControllerTest"`
      → 9/9 green. Review: PASS → `opencode-reviews/lab-5-task1-buildreport.code-review.md`. Флаги человеку:
      (1) `buildReport()` не покрыт ни одним тестом (unit/integration/controller — ноль; тесты запрещены задачей) —
      зелёный regression доказывает только отсутствие поломки соседей, не корректность новой ветки;
      (2) `countClosedOrders()` (:77) и `deleteClosedOrders()` (:79) в `OrderService` тоже без Javadoc — вне объёма
      Task1, оставлены как есть.
