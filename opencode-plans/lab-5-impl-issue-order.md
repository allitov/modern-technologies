# Plan contract: lab-5-impl-issue-order

> Disk source of truth. Re-read before each decision and after any compaction.

**Goal:** Реализовать метод `boolean issueOrder(String orderCode)` в
`pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java` (сейчас — заглушка
`return false;`, строки 33–36) по спецификации `tasks/lab-5-impl-issue-order.md`: по коду заказа
(«PVZ-000001») получить из БД текущий статус; если заказ не найден или его статус не `ready` —
вернуть `false`; иначе одним запросом перевести заказ в статус `issued` и записать дату выдачи
(`issued_at`), вернув `true`. Постусловия: изменены `status` и `issued_at`, поле `cell` не
трогается. Все запросы — параметризованные (named bind-параметры JDBI, склейка SQL-строк
запрещена). Слой репозитория логики не содержит — только сохраняет переданные значения. Тесты НЕ
писать (явное требование задания, строки 17 файла-задания). `cd pvz && gradlew.bat compileJava` —
зелёный.

**active:** — (t1, t2 закрыты)

## open_questions

- [x] Q1: Чем получать текущий статус в сервисе? — answer: РЕШЕНО пользователем:
      переиспользовать существующий `OrderRepository.findByCode` (`Optional<Order>`,
      `OrderRepository.java:40-41`) — статус уже в сущности, нулевой дифф на чтение.
- [x] Q2: Откуда брать дату выдачи `issued_at`? — answer: РЕШЕНО пользователем: `Instant.now()` в
      сервисе, передать параметром в UPDATE — репозиторий максимально «тупой» (только сохраняет
      переданное), JDBI 3.52.1 биндит `Instant` из коробки.

## Контекст (разведка wave 1 + wave 2)

- **Стаб:** `OrderServiceImpl.java:33-36` — `issueOrder` → `return false;`; интерфейс
  `OrderService.java:11` — `boolean issueOrder(String orderCode)`.
- **Персистенция:** JDBI 3 SqlObject — `OrderRepository` это интерфейс с `@SqlQuery`/`@SqlUpdate`
  и именованными биндами; создаётся как `jdbi.onDemand(OrderRepository.class)`
  (`JdbiRepositoryConfig.java:11-14`); `JdbiConfig` оборачивает DataSource в
  `TransactionAwareDataSourceProxy` → Spring `@Transactional` на методе сервиса объединяет все
  вызовы репозитория в одну транзакцию (рабочие прецеденты: `deleteClosedOrders` :74, `addOrder`
  :80).
- **Схема** (`pvz/src/main/resources/db/migration/V1__create_orders.sql`, единственная миграция):
  `orders(order_code varchar(30) unique not null, cell varchar(30), status varchar(20) not null
  default 'ready', created_at timestamptz not null default now(), issued_at timestamptz null)` —
  **новая миграция не нужна**, колонки на месте.
- **Статусы в БД** — нижний регистр; канонический паттерн получения ключа:
  `OrderStatus.ISSUED.name().toLowerCase(Locale.ROOT)` (`OrderServiceImpl.java:26-29`); enum
  `io.allitov.pvz.model.OrderStatus` (READY/ISSUED/CANCELLED), `Order.status` — String.
- **Чтение по коду:** `@SqlQuery(ORDER_COLUMNS + " from orders where order_code = :code") Optional<Order>
  findByCode(@Bind("code") String code)` (`OrderRepository.java:40-41`) — параметризован,
  сегодня не используется сервисом.
- **Обновление:** единственный @SqlUpdate-прецедент — `updateCode(id, code)`
  (`OrderRepository.java:51-52`, void). **Метода обновления статуса в репозитории нет.**
  Конвенция файла: только именованные bind-параметры, строковых литералов статусов в аннотациях
  нет (grep — 0 совпадений).
- **Вызывающий код:** `OrderController.issue()` (`OrderController.java:143-165`,
  `POST /orders/{code}/issue`) ждёт `boolean`; пустой код отсекает сам; при `false` сам смотрит
  статус и показывает flash «Заказ нельзя выдать (статус: …)». Значит исключения для «не найден /
  не ready» бросать НЕЛЬЗЯ — только `false`.
- **Тесты:** grep «issue» по `pvz/src/test` — 0 совпадений; тесты на issueOrder отсутствуют, новые
  писать запрещено заданием. Регрессию ничего не ломает.
- **Сборка:** Gradle, `cd pvz && gradlew.bat compileJava` (принятый в предыдущих контрактах
  критерий приёмки).

## Архитектурное решение (design)

1. **Репозиторий (t1)** — один новый «тупой» метод сохранения значений (логики нет: никакого
   `and status = 'ready'` в WHERE — проверку делает сервис, как требует задание):
   ```java
   /**
    * Обновляет статус и дату выдачи заказа по его коду.
    *
    * @param code     код заказа
    * @param status   новый статус (ключ БД, нижний регистр)
    * @param issuedAt дата выдачи
    * @return число обновлённых строк (0 или 1)
    */
   @SqlUpdate("update orders set status = :status, issued_at = :issuedAt where order_code = :code")
   int updateStatusAndIssuedAt(@Bind("code") String code, @Bind("status") String status,
           @Bind("issuedAt") Instant issuedAt);
   ```
   `int`, а не void: сервис возвращает `updated > 0`; для уникального `order_code` это 0 или 1.
2. **Сервис (t2)** — заполнить стаб, императивный стиль с ранними возвратами (как весь класс):
   - `@Transactional` над методом (SELECT + UPDATE в одной транзакции — атомарность по контракту
     `pvz-lab5.md:39,81`).
   - `findByCode(orderCode)` → пусто → `return false;`.
   - статус из сущности ≠ `OrderStatus.READY.name().toLowerCase(Locale.ROOT)` →
     `return false;`.
   - иначе `orderRepository.updateStatusAndIssuedAt(orderCode,
     OrderStatus.ISSUED.name().toLowerCase(Locale.ROOT), Instant.now())` → `return updated > 0;`.
   - БЕЗ try/catch вокруг `JdbiException`: контракт метода — boolean, `false` только для «не
     найден / не ready»; ошибки БД всплывают (в `addOrder` обёртку в `OrderCreationException`
     диктовал его контракт, здесь — нет).
   - Постусловие «cell не изменяется» выполняется по построению: `cell` в SQL не фигурирует.
3. **Out of scope:** стаб `cancelOrder` (`OrderServiceImpl.java:38-41`) не трогать (нужен
   отдельный метод репозитория — иначе перезапишет `issued_at`); контроллер, шаблоны, миграции,
   `build.gradle.kts`, тесты — не трогать.

## backlog

- [x] **t1** — Репозиторий: метод `updateStatusAndIssuedAt`
    - **agent:** coder
    - **inputs:** `pvz/src/main/java/io/allitov/pvz/repository/OrderRepository.java` (целиком —
      стиль: именованные бинды, литералов нет, ORDER_COLUMNS, прецедент `updateCode` :51-52);
      `pvz/src/main/java/io/allitov/pvz/model/Order.java:7,57` (прецедент `java.time.Instant`);
      сигнатура из секции «Архитектурное решение» п.1 этого контракта; конвенции Javadoc из
      `agent/rules.md` (русский, @param/@return).
    - **deps:** []
    - **acceptance:** В `OrderRepository` существует метод `int updateStatusAndIssuedAt(String
      code, String status, Instant issuedAt)` с `@SqlUpdate("update orders set status = :status,
      issued_at = :issuedAt where order_code = :code")`, параметризованный (никакой склейки SQL),
      с русским Javadoc; `cell` в SQL отсутствует; НИКАКИЕ другие файлы не изменены;
      `cd pvz && gradlew.bat compileJava` — BUILD SUCCESSFUL.
    - **description:** Добавить в интерфейс `OrderRepository` ровно один метод из секции
      «Архитектурное решение» п.1 (с русским Javadoc). Импорт `java.time.Instant`. Больше ничего:
      существующие методы, JdbiConfig, миграции, сервис — не трогать. Javadoc соседних методов не
      менять.
    - **touched:** pvz/src/main/java/io/allitov/pvz/repository/OrderRepository.java (import
      `java.time.Instant` + метод `updateStatusAndIssuedAt`, :55-65)
- [x] **t2** — Сервис: реализация `issueOrder` в `OrderServiceImpl`
    - **agent:** coder
    - **inputs:** `pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java` (стаб
      :33-36, стиль ранних возвратов, паттерн `OrderStatus.X.name().toLowerCase(Locale.ROOT)`
      :26-29, прецеденты `@Transactional` :74/:80); `pvz/src/main/java/io/allitov/pvz/service/OrderService.java:11`;
      `pvz/src/main/java/io/allitov/pvz/repository/OrderRepository.java` (`findByCode` :40-41,
      новый `updateStatusAndIssuedAt` из t1); `pvz/src/main/java/io/allitov/pvz/model/OrderStatus.java`;
      спецификация `tasks/lab-5-impl-issue-order.md`; алгоритм из секции «Архитектурное решение»
      п.2 этого контракта.
    - **deps:** [t1]
    - **acceptance:** `issueOrder` больше не заглушка: заказ не найден → `false`; статус ≠ `ready`
      → `false`; иначе `status` в БД = `issued` и `issued_at` записан, `cell` не изменён; метод
      помечен `@Transactional`; не бросает исключений для «не найден / не ready»; SQL нигде не
      склеивается строками; `cell` не упоминается в новом коде; стаб `cancelOrder` и прочие методы
      не тронуты; `cd pvz && gradlew.bat compileJava` — BUILD SUCCESSFUL.
    - **description:** Заполнить стаб `issueOrder` в `OrderServiceImpl` по алгоритму «Архитектурное
      решение» п.2: `@Transactional`; `findByCode` → early-return `false` на пустом Optional;
      сравнение статуса с `OrderStatus.READY.name().toLowerCase(Locale.ROOT)` → early-return
      `false`; вызов `updateStatusAndIssuedAt(code, ISSUED-ключ, Instant.now())`, вернуть
      `updated > 0`. Без try/catch вокруг JdbiException. Если в интерфейсе `OrderService.issueOrder`
      нет русского Javadoc (@param/@return) — добавить его в интерфейсе (в реализации `@Override`
      без Javadoc). `cancelOrder`, `buildReport` и остальные методы — не трогать.
    - **touched:** pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java (стаб
      `issueOrder` → реализация, :36-52; импорты `java.time.Instant`, `java.util.Optional`);
      pvz/src/main/java/io/allitov/pvz/service/OrderService.java (русский Javadoc к `issueOrder`,
      :11-22)
