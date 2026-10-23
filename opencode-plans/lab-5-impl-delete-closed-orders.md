# Plan contract: lab-5-impl-delete-closed-orders

> Disk source of truth. Re-read before each decision and after any compaction.

**Goal:** В модуле `pvz` реализован метод `int deleteClosedOrders()` сервиса заказов: одним DELETE-запросом из таблицы `orders` удаляются все заказы со статусами `issued` и `cancelled`, метод возвращает **число удалённых строк** (не `size()`), изменения фиксируются транзакцией; заказы `ready` не затрагиваются. Логика («какие статусы закрыты») живёт в сервисе, репозиторий остаётся тупым параметризованным запросом. Тесты **не пишутся** (по ТЗ); существующие тесты остаются зелёными. Веб-слой не трогается. `countClosedOrders()` остаётся заглушкой (решение человека: строго по ТЗ).

**active:** —

## open_questions

- [x] Q1: Контроллер cleanup() вызывает deleteClosedOrders() только при countClosedOrders() > 0 (сейчас заглушка 0) — реализовать ли заодно countClosedOrders()? — answer: **Нет, только deleteClosedOrders(), строго по ТЗ** (решено пользователем 2026-10-23).

## Context (findings wave 1+2)

- Проект: `pvz` — **отдельный Gradle-билд** (свой `pvz/settings.gradle.kts`, `pvz/gradlew.bat`); корневой settings включает только `todo-app`. Команды — из каталога `pvz/`: `.\gradlew.bat classes`, `.\gradlew.bat test`. Java 25, Spring Boot + JDBI 3.52.1 (`jdbi3-core/sqlobject/postgres/spring5`) + PostgreSQL + Flyway.
- Контракт уже объявлен: `pvz/src/main/java/io/allitov/pvz/service/OrderService.java:23` — `int deleteClosedOrders();` (без javadoc). Заглушка: `pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java:55-58` (`return 0`).
- Потребитель уже подключён (НЕ ТРОГАТЬ): `OrderController.java:223-235`, `POST /orders/cleanup` → `countClosedOrders()` (:225) → при >0 `deleteClosedOrders()` (:231).
- Репозиторий `pvz/src/main/java/io/allitov/pvz/repository/OrderRepository.java:13` — JDBI SqlObject: `@RegisterConstructorMapper(Order.class)` (:12), методы `findByCode` (:15), `insert` (:18-24), `updateCode` (:26-27); стиль — `@SqlUpdate` + `@Bind("name")` + `:name` в SQL; импорты `org.jdbi.v3.sqlobject.{config.RegisterConstructorMapper, customizer.Bind, statement.{GetGeneratedKeys, SqlQuery, SqlUpdate}}`. Метода DELETE нет. `@BindList` в модуле ещё не использовался, но поддерживается JDBI 3.52.1 из коробки (`org.jdbi.v3.sqlobject.customizer.BindList`, плейсхолдер `<statuses>`).
- Метод `@SqlUpdate`, объявленный как `int`, возвращает **число затронутых строк** — это аналог `affected_rows()` из подсказки ТЗ (в JDBI нет `size()`; `.size()` в `OrderController.java:63` — легитимный счётчик списка, не трогать).
- Транзакции: декларативно `@Transactional` на методе сервиса (образец `addOrder` — `OrderServiceImpl.java:60-73`); JDBI создан через `TransactionAwareDataSourceProxy` (`JdbiConfig.java:25-32`), on-demand хендлы участвуют в Spring-транзакции; регистрация репозитория — `jdbi.onDemand(OrderRepository.class)` (`JdbiRepositoryConfig.java:11-14`) — для нового метода существующего репозитория **ничего добавлять не нужно**.
- Статусы: в БД строки в нижнем регистре (`V1__create_orders.sql:7` — `status varchar(20) not null default 'ready'`); enum `OrderStatus` (`pvz/.../model/OrderStatus.java:16`): `READY/ISSUED/CANCELLED`, db-ключ = имя константы в нижнем регистре, отдельного геттера нет → `OrderStatus.ISSUED.name().toLowerCase(Locale.ROOT)`. `Order.status` — `String` (`Order.java:52`).
- Существующие тесты cleanup-методы не трогают (unit `OrderServiceImplTest` — только `AddOrder`; integration — Testcontainers, `@Testcontainers(disabledWithoutDocker = true)`, без Docker пропускаются). Реализация заглушек их не ломает. Тесты по ТЗ не пишем и файлы тестов не меняем.

## backlog

- [x] **Task1** — репозиторий: параметризованный `@SqlUpdate`-метод удаления по статусам
    - **agent:** coder
    - **inputs:** `pvz/src/main/java/io/allitov/pvz/repository/OrderRepository.java` (полностью, см. Context); стиль проекта — `@SqlUpdate` + bind-плейсхолдеры. Добавить ТОЛЬКО метод:
      ```java
      @SqlUpdate("delete from orders where status in (<statuses>)")
      int deleteByStatuses(@BindList("statuses") Collection<String> statuses);
      ```
      + импорт `org.jdbi.v3.sqlobject.customizer.BindList` и `java.util.Collection`. Именование: если `deleteByStatuses` конфликтует по стилю с соседями (`findByCode`, `insert`, `updateCode`) — допустимо `deleteByStatus`. В репозитории НЕТ статусных литералов `'issued'`/`'cancelled'` и НЕТ логики выбора статусов — список приходит параметром. Если `@BindList` по какой-то причине не компилируется — fallback: два параметра `@Bind("first")/@Bind("second")` с `in (:first, :second)`, но `@BindList` предпочтителен.
    - **deps:** []
    - **acceptance:** `.\gradlew.bat classes` (из каталога `pvz/`) проходит без ошибок; в `OrderRepository` появился ровно один новый метод `int deleteByStatuses(Collection<String>)` с `@SqlUpdate("delete from orders where status in (<statuses>)")`; ни одного нового строкового литерала со статусами в файле.
    - **description:** Добавить в интерфейс `OrderRepository` один тупой параметризованный DELETE-метод (см. inputs). Ничего больше не менять.
    - **touched:** `pvz/src/main/java/io/allitov/pvz/repository/OrderRepository.java` (+2 импорта, +1 метод `deleteByStatuses`; review PASS → `opencode-reviews/lab-5-impl-delete-closed-orders.task1.code-review.md`)
- [x] **Task2** — сервис: реализация `deleteClosedOrders()` (+ запуск существующих тестов)
    - **agent:** coder
    - **inputs:** `pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java` (полностью; заглушка :55-58, образец `addOrder` :60-73 с `@Transactional`, импорты :1-15); `OrderStatus` (`pvz/.../model/OrderStatus.java:16`, db-ключ = `name().toLowerCase()`); результат Task1: `orderRepository.deleteByStatuses(Collection<String>) : int` (число затронутых строк). Заменить тело заглушки:
      - константа уровня класса: `private static final List<String> CLOSED_STATUSES = List.of(OrderStatus.ISSUED.name().toLowerCase(Locale.ROOT), OrderStatus.CANCELLED.name().toLowerCase(Locale.ROOT));`
      - метод: `@Override @Transactional public int deleteClosedOrders() { return orderRepository.deleteByStatuses(CLOSED_STATUSES); }`
      - `@Transactional` уже импортирован в файле; добавить импорт `OrderStatus` и `Locale`, если отсутствуют. НИКАКИХ строковых литералов `"issued"`/`"cancelled"` в сервисе — только через enum. Оборачивание `JdbiException` НЕ добавлять (доменного исключения для этой операции нет, по ТЗ не требуется). `countClosedOrders()` (:50-53) и все прочие заглушки — НЕ трогать. Тестовые файлы НЕ менять и НЕ создавать.
    - **deps:** [Task1]
    - **acceptance:** из каталога `pvz/` команда `.\gradlew.bat test` проходит (unit-тесты зелёные; интеграционные без Docker пропускаются — это норма); `deleteClosedOrders()` больше не `return 0`, а возвращает результат `orderRepository.deleteByStatuses(CLOSED_STATUSES)`; на методе есть `@Transactional`; в сервисе нет литералов `'issued'`/`'cancelled'`.
    - **description:** Реализовать `deleteClosedOrders()` в `OrderServiceImpl` по образцу из inputs; затем прогнать существующие тесты командой `.\gradlew.bat test` (только запуск — тесты не менять) и отчитаться результатом.
    - **touched:** `pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java` (+2 импорта `OrderStatus`/`Locale`, +константа `CLOSED_STATUSES`, тело `deleteClosedOrders()` → `orderRepository.deleteByStatuses(CLOSED_STATUSES)` с `@Transactional`; тесты: 9 passed / 5 skipped / 0 failed; review PASS → `opencode-reviews/lab-5-impl-delete-closed-orders.task2.code-review.md`)

## Verification (после Task2, человеком или orchestrator-ом)

- `pvz/` → `.\gradlew.bat test` — зелёный без Docker (интеграционные skip).
- Функциональная проверка при наличии БД (опционально): `POST /orders/cleanup` удаляет только `issued`/`cancelled`; заказы `ready` остаются. Замечание: через UI фича пока недостижима, т.к. `countClosedOrders()` — заглушка 0 (решение Q1: осознанно, строго по ТЗ).
