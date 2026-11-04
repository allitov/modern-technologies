# Plan contract: lab-5-impl-list-orders

> Disk source of truth. Re-read before each decision and after any compaction.

**Goal:** Реализовать метод `listOrders` в `pvz`-модуле (задание `../../tasks/lab-5/lab-5-impl-list-orders.md`): пустой фильтр → все заказы; непустой → без учёта регистра подстрока по `order_code` / `phone` / `parcel_article` **в SQL-запросе** (bind-параметры, без склеек с пользовательским вводом — инъекция `' OR '1'='1` даёт пустой список); NULL-поля `phone`/`parcel_article`/`cell` читаются как `''`; сортировка «свежие сверху» `ORDER BY created_at DESC, id DESC`; данные не изменяются. Фильтрация оформлена гибко (решение человека): value-object `OrderFilter` + перегрузка `listOrders(OrderFilter)`, требуемый лабой `listOrders(String)` остаётся и делегирует. Попутно (решение человека) чинится латентный дефект маппера `findByCode`. Тесты **не пишутся** (требование задания) — критерий: существующий `gradlew :pvz:test` зелёный + соответствие спекам ниже.
**active:** — (все задачи выполнены)

## open_questions

- [x] Q1: Механизм фильтрации — оставить `String` или более гибкий? — answer: **OrderFilter + перегрузка**. `listOrders(String filter)` сохраняется (контроллер `OrderController.java:54,155` и контракт лабы не ломаются), добавляется `listOrders(OrderFilter)`; репозиторий — два тупых статических SQL-метода (все / по подстроке), выбор — в сервисе.
- [x] Q2: Чинить ли попутно `findByCode` (тот же дефект маппера: колонки `order_code`/`parcel_article` не совпадают с параметрами конструктора `Order`) — answer: **Да**, тем же списком колонок (общая константа `ORDER_COLUMNS`).

## backlog

- [x] **Task1** — `OrderRepository`: список колонок + два запроса на чтение + фикс `findByCode`
    - **agent:** coder
    - **inputs:**
        - Файл: `../../pvz/src/main/java/io/allitov/pvz/repository/OrderRepository.java` (JDBI SqlObject-интерфейс, `@RegisterConstructorMapper(Order.class)` на уровне класса, строка 14; сейчас: `findByCode`, `insert`, `updateCode`, `deleteByStatuses`, `countByStatuses`; стиль `@SqlQuery` + `@Bind("name")`).
        - Модель: `../../pvz/src/main/java/io/allitov/pvz/model/Order.java` — Lombok `@Data @Builder @AllArgsConstructor`, порядок полей = порядок параметров конструктора: `id(long), code, phone, article, cell, status, createdAt(Instant)`. **Order.java не менять.**
        - Документированный дефект (``opencode-reviews/pvz-add-order-fix-order.code-review.md:41``): конструктор `Order` ожидает `code`/`article`, колонки — `order_code`/`parcel_article` → JDBI `UNMATCHED_CONSTRUCTOR_PARAMETER` при первом реальном чтении. Лечится алиасами колонок в SELECT.
        - Схема: `../../pvz/src/main/resources/db/migration/V1__create_orders.sql` — `orders(id, order_code unique not null, phone, parcel_article, cell, status, created_at timestamptz, issued_at)`.
        - **Добавить константу и методы (дословно):**
            ```java
            String ORDER_COLUMNS = "id, order_code as code, coalesce(phone, '') as phone, "
                    + "coalesce(parcel_article, '') as article, coalesce(cell, '') as cell, "
                    + "status, created_at as \"createdAt\"";
            ```
            *(константа интерфейса = compile-time constant, допустима в конкатенации аннотации; алиасы чинят маппер, `coalesce` — NULL→`''`; `\"createdAt\"` — явный алиас в точное имя параметра конструктора, без ставки на поведение snake_case в JDBI; лишняя колонка `issued_at` не выбирается — `select *` не используется)*
            ```java
            @SqlQuery(ORDER_COLUMNS + " from orders order by created_at desc, id desc")
            List<Order> findAll();
            ```
            ```java
            @SqlQuery(ORDER_COLUMNS + " from orders where order_code ilike :search "
                    + "or phone ilike :search or parcel_article ilike :search "
                    + "order by created_at desc, id desc")
            List<Order> findBySearchTerm(@Bind("search") String search);
            ```
        - **`findByCode` переписать на тот же список колонок:**
            ```java
            @SqlQuery(ORDER_COLUMNS + " from orders where order_code = :code")
            Optional<Order> findByCode(@Bind("code") String code);
            ```
        - **Запрещено:** логика в репозитории (ветвления, склейка `%…%`, статусные литералы, try/catch, логирование) — «Слой репозитория только сохраняет значение в БД» (`tasks/lab-5-impl-list-orders.md:19`); прецедент соседних планов: репо = тупой параметризованный SQL.
    - **deps:** []
    - **acceptance:** `gradlew :pvz:test` зелёный (тесты не тронуты); в `OrderRepository` — константа `ORDER_COLUMNS` + методы `findAll`/`findBySearchTerm` с SQL дословно как выше; `findByCode` использует `ORDER_COLUMNS`; в репо нет `%`-склейки, литералов статусов, ветвлений и try/catch; `git status` не показывает изменений вне `OrderRepository.java`.
    - **description:** Добавить в `OrderRepository` константу `ORDER_COLUMNS` (алиасы под конструктор `Order` + COALESCE для nullable-полей) и два читающих метода `findAll()` / `findBySearchTerm(@Bind("search") String)` (ILIKE по трём колонкам, сортировка `created_at desc, id desc`); перевести `findByCode` на `ORDER_COLUMNS`, устраняя задокументированный `UNMATCHED_CONSTRUCTOR_PARAMETER`. Ничего больше не менять.
    - **touched:** `../../pvz/src/main/java/io/allitov/pvz/repository/OrderRepository.java` (+14/−1; ревью: `../../opencode-reviews/lab-5/lab-5-task1-order-repository.code-review.md` — PASS; регрессия: 14 тестов GREEN; покрытия изменённой ветки нет — ручная проверка после Task2)
- [x] **Task2** — `OrderFilter` + перегрузка в `OrderService` + реализация в `OrderServiceImpl`
    - **agent:** coder
    - **inputs:**
        - Результат Task1: в `OrderRepository` есть `findAll()` и `findBySearchTerm(@Bind("search") String search)` (ILIKE уже регистронезависимый, `%`-обёртка — обязанность сервиса).
        - `../../pvz/src/main/java/io/allitov/pvz/service/OrderService.java` — `List<Order> listOrders(String filter);` (строка 17, без javadoc); стиль javadoc — русский, с `@param`/`@return` (как у `addOrder`, строки 25–40).
        - `../../pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java` — заглушка `listOrders` (строки 48–50, `return List.of();`); класс `@Service @RequiredArgsConstructor`, `private final OrderRepository orderRepository`; читать, как оформлен `countClosedOrders` (аннотации/логирование), и **зеркалить его стиль** для listOrders.
        - **Создать `../../pvz/src/main/java/io/allitov/pvz/model/OrderFilter.java` (дословно):**
            ```java
            @Value
            @Builder
            public class OrderFilter {
                /** подстрока поиска; {@code null} — фильтр не задан (вернуть все заказы). */
                String search;

                /** нормализует вход: null/blank → null (без фильтра), иначе trim. */
                public static OrderFilter of(String search) {
                    return new OrderFilter(search == null || search.isBlank() ? null : search.trim());
                }
            }
            ```
            *(javadoc класса — по-русски, в стиле модели; `@Value`+`@Builder` — готовность расширять: статусы/пагинация добавятся полями без ломки вызывателей)*
        - **`OrderService`:** оставить `listOrders(String filter)`, добавить перегрузку `List<Order> listOrders(OrderFilter filter);` — русская javadoc на обеих (контракт: пустой/blank/null → все; иначе подстрока без учёта регистра по коду/телефону/артикулу; свежие сверху).
        - **`OrderServiceImpl` — заменить заглушку (дословно, вместо строк 48–50):**
            ```java
            @Override
            public List<Order> listOrders(String filter) {
                return listOrders(OrderFilter.of(filter));
            }

            @Override
            public List<Order> listOrders(OrderFilter filter) {
                if (filter.getSearch() == null) {
                    return orderRepository.findAll();
                }
                return orderRepository.findBySearchTerm("%" + filter.getSearch() + "%");
            }
            ```
            (`@Transactional`/прочие аннотации — ровно как у `countClosedOrders`: единообразие с соседним читающим методом; документация планов расходится, консистентность в коде важнее.)
        - **Запрещено:** try/catch `JdbiException` и логирование в listOrders (прецедент обёртки — только в `addOrder` для своей ошибки); правки стабов `issueOrder`/`cancelOrder`/`buildReport`/`listByStatus`; правки web-слоя и `src/test`; SQL-строки в сервисе (SQL — только в `OrderRepository`).
        - Не путать со устаревшим рецептом `opencode-plans/pvz-lab5.md:81` («свой RowMapper, created_at → yyyy-MM-dd»): план-предшественник superseded (``opencode-plans/pvz-add-order.md:217-219``), модель вернулась к `Instant` (коммит `466781f`) — custom RowMapper **не** писать.
    - **deps:** [Task1]
    - **acceptance:** `gradlew :pvz:test` зелёный; семантика по спеке подтверждается кодом: `null`/`""`/`"   "` → `findAll()`, непустой (после trim) → `findBySearchTerm("%"+q+"%")`, инъекция `' OR '1'='1` уходит bind-параметром (в сервисе нет конкатенации фильтра в SQL), сортировка и NULL→`''` — в SQL из Task1; `git status`: изменены только `OrderFilter.java` (новый), `OrderService.java`, `OrderServiceImpl.java`; web-слой и `src/test` не тронуты (вызовы `OrderController.java:54,155` работают без правок).
    - **description:** Создать value-object `OrderFilter` (поиск + нормализация blank→null), добавить в `OrderService` перегрузку `listOrders(OrderFilter)` с русской javadoc, в `OrderServiceImpl` заменить заглушку: String-версия делегирует через `OrderFilter.of`, OrderFilter-версия ветвит `findAll()` / `findBySearchTerm("%"+search+"%")`. Гибкий механизм: будущие критерии (статусы и т.п.) — новые поля `OrderFilter`, вызыватели не ломаются.
    - **touched:** `../../pvz/src/main/java/io/allitov/pvz/model/OrderFilter.java` (новый), `pvz/src/main/java/io/allitov/pvz/service/OrderService.java`, `pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java` (ревью: `../../opencode-reviews/lab-5/lab-5-task2-order-filter-service.code-review.md` — PASS; регрессия: 14 тестов GREEN через `--rerun`; покрытия listOrders нет — по требованию лабы тесты не пишутся)

## Примечания (для человека/оркестратора, не задачи)

- **Ручная проверка маппинга:** по требованию задания тесты не пишутся, а в рантайме чтение `Order` через JDBI ещё ни разу не выполнялось (интеграционный тест читает raw JDBC). После Task2 имеет смысл запустить приложение и проверить вручную: `GET /` (все), `GET /?filter=арт` (регистронезависимо), NULL-поля как `''`, свежие сверху.
- **Коммит** — за человеком (прецедент: shell недоступен субагентам), стиль соседних планов: `pvz: <русское причастие>`.
- Вне скоупа: устаревшая javadoc `Order.createdAt` (говорит «String yyyy-MM-dd HH:mm», поле — `Instant`); фильтр по статусу остаётся в контроллере (перенос в `OrderFilter` — будущая задача).
- **Факт исполнения:** команда из acceptance (`gradlew :pvz:test`) из корня не существует — `pvz` самостоятельный Gradle-билд; эквивалент: `.\gradlew.bat test` из каталога `pvz` (прогнано тестером дважды, 14 тестов GREEN, второй раз с `--rerun` против `UP-TO-DATE`). Ревью-отчёты: `../../opencode-reviews/lab-5/lab-5-task1-order-repository.code-review.md`, `../../opencode-reviews/lab-5/lab-5-task2-order-filter-service.code-review.md` (оба PASS). Ревьюеры делали `git add -A` — изменения в индексе; коммит за человеком (стиль: `pvz: <русское причастие>`); решить, включать ли markdown-артефакты (`opencode-plans/`, `tasks/`, `opencode-reviews/`) в коммит.
