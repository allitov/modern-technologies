# Plan contract: lab-5-impl-count-closed-orders

> Disk source of truth. Re-read before each decision and after any compaction.

**Goal:** Метод `int countClosedOrders()` в `OrderServiceImpl` перестаёт быть стабом `return 0` и возвращает реальное число заказов в статусах `issued`/`cancelled` из БД. Логика («какие статусы закрыты») живёт в сервисе через уже существующую константу `CLOSED_STATUSES`; репозиторий получает один тупой параметризованный `@SqlQuery`-запрос `count(*) ... where status in (<statuses>)` — зеркальный аналог реализованного ранее `deleteByStatuses`. По ТЗ (`tasks/lab-5-impl-count-closed-orders.md:15`) тесты НЕ пишутся; существующие тесты остаются зелёными; web-слой не трогается (диалог подтверждения уже подключён к методу: `OrderController.java:64,225` → `orders.html:59-60,87-88`). Постусловие: данные не изменены (только `select count(*)`).

**active:** Task3

## open_questions

- [x] Q1: Куда поместить сам запрос `count(*)`, если «слой репозитория только сохраняет в БД, без логики»? — answer: В `OrderRepository` как тупой параметризованный запрос: список статусов приходит параметром, в репозитории нет ни статусных литералов, ни логики выбора. Это прецедент: та же фраза дословно в ТЗ `tasks/lab-5-impl-delete-closed-orders.md:18`, и sibling-метод `deleteByStatuses` сделан именно так (план `opencode-plans/lab-5-impl-delete-closed-orders.md:5,33`, ревью `../../opencode-reviews/lab-5/lab-5-impl-delete-closed-orders.task1.code-review.md` — PASS, критерий «нет статусных литералов в репозитории»).
- [x] Q2: Нужна ли `@Transactional` на read-only count? — answer: Нет. У `deleteClosedOrders` она стоит из-за записи; count — одиночный `select`, стаб аннотаций не имеет, JDBI onDemand-прокси управляет сессией сам.
- [x] Q3: Сломает ли реализация существующие тесты (стаб возвращал 0)? — answer: Нет: grep по `../../pvz/src/test` даёт 2 упоминания `OrderRepository`, оба — mock-поле в `OrderServiceImplTest.java:3,36` (тестируется только `addOrder`); `OrderControllerTest` не вызывает GET `/` и POST `/orders/cleanup`; интеграционные тесты — только AddOrder и skip без Docker. Базлайн последнего прогона: 9 passed / 5 skipped / 0 failed.

Блокирующих вопросов нет — оркестратор может стартовать.

## backlog

- [x] **Task1** — метод `countByStatuses` в `OrderRepository`
    - **agent:** coder
    - **inputs:**
        - Файл: `../../pvz/src/main/java/io/allitov/pvz/repository/OrderRepository.java` (34 строки, JDBI 3 SqlObject интерфейс, прокси создаётся в `config/JdbiRepositoryConfig.java:11-14` через `jdbi.onDemand(...)` — добавление метода НЕ требует правок конфига).
        - Образец для зеркалирования — существующий `deleteByStatuses` (`OrderRepository.java:31-32`):
          ```java
          @SqlUpdate("delete from orders where status in (<statuses>)")
          int deleteByStatuses(@BindList("statuses") Collection<String> statuses);
          ```
        - Добавить ПОСЛЕ `deleteByStatuses` ровно один метод (по аналогии с ревью sibling-задачи: метод ставится после предшественника, аннотация одной строкой):
          ```java
          @SqlQuery("select count(*) from orders where status in (<statuses>)")
          int countByStatuses(@BindList("statuses") Collection<String> statuses);
          ```
        - Импорты: `org.jdbi.v3.sqlobject.statement.SqlQuery` и `...customizer.BindList` УЖЕ импортированы (`findByCode` использует `@SqlQuery`, `deleteByStatuses` — `@BindList`) — новых импортов не нужно.
        - Техническая база: JDBI 3.52.1 (`pvz/build.gradle.kts:39-42`) маппит одиночную колонку на примитив `int` нативно (встроенные column mapper'ы; прецедент примитива — `long insert(...)` с `@GetGeneratedKeys` в том же файле, `:20-26`). В PG `count(*)` возвращает bigint — маппинг в `int` корректен.
        - Запрещено: строковые литералы `'issued'`/`'cancelled'`/`'ready'` в репозитории, логика выбора статусов, правки других методов (в т.ч. косметика вроде отсутствующей `;` в `updateCode:28-29` — не трогать).
    - **deps:** []
    - **acceptance:** `.\gradlew.bat classes` (из каталога `../../pvz`) проходит; в `OrderRepository` ровно один новый метод с сигнатурой и SQL точно как в контракте; в файле нет ни одного нового строкового литерала со статусами; diff ограничен одним файлом.
    - **description:** Добавить в интерфейс `OrderRepository` JDBI SqlObject-метод `countByStatuses` — параметризованный `select count(*)` по списку статусов. Больше ничего не менять.
    - **touched:** pvz/src/main/java/io/allitov/pvz/repository/OrderRepository.java (+2 строки: метод `countByStatuses` после `deleteByStatuses`; ревью — opencode-reviews/lab-5-impl-count-closed-orders.task1.code-review.md, PASS)
- [x] **Task2** — реализовать `countClosedOrders()` в `OrderServiceImpl`
    - **agent:** coder
    - **inputs:**
        - Файл: `../../pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java`.
        - Заменить стаб `:57-60` (`@Override public int countClosedOrders() { return 0; }`) на делегацию:
          ```java
          @Override
          public int countClosedOrders() {
              return orderRepository.countByStatuses(CLOSED_STATUSES);
          }
          ```
        - Константа уже есть (`:25-28`): `private static final List<String> CLOSED_STATUSES = List.of(OrderStatus.ISSUED.name().toLowerCase(Locale.ROOT), OrderStatus.CANCELLED.name().toLowerCase(Locale.ROOT));` — значения `issued`/`cancelled` совпадают с ключами БД (`V1__create_orders.sql:7`: `status varchar(20) not null default 'ready'`; enum `model/OrderStatus.java:21-31`).
        - Образец стиля — соседний `deleteClosedOrders()` (`:62-66`): `@Override` + одна строка делегации; НО без `@Transactional` (read-only одиночный select — решение зафиксировано в open_questions Q2).
        - Запрещено: строковые литералы `"issued"`/`"cancelled"` в сервисе (только через enum/`CLOSED_STATUSES`), try/catch оборачивание `JdbiException` (прецедент обёртки есть только в `addOrder` для своей ошибки — сюда не переносить), логирование (в классе логов нет, sibling их не имеет), правки остальных стабов (`issueOrder`, `cancelOrder`, `buildReport`, `listOrders`, `listByStatus`) и web-слоя (`OrderController.java:64,223-235` и `templates/orders.html` уже подключены к методу — НЕ ТРОГАТЬ).
        - Тесты по ТЗ не пишутся и не изменяются.
    - **deps:** [Task1]
    - **acceptance:** `.\gradlew.bat classes` проходит; `.\gradlew.bat test` зелёный (базлайн 9 passed / 5 skipped / 0 failed без Docker — интеграционные скипаются сами); в методе нет статусных литералов, используется `CLOSED_STATUSES`; диф — один файл, один метод.
    - **description:** Реализовать `countClosedOrders()` как тонкую делегацию в `orderRepository.countByStatuses(CLOSED_STATUSES)`, убрав стаб.
    - **touched:** pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java (1 строка: стаб `return 0` → `return orderRepository.countByStatuses(CLOSED_STATUSES);`; ревью — opencode-reviews/lab-5-impl-count-closed-orders.task2.code-review.md, PASS; регрессия tester — 9 passed / 5 skipped / 0 failed = базлайн)
- [ ] **Task3** — закоммитить результат
    - ⚠️ **ЗАБЛОКИРОВАНО (2026-10-23):** 3 диспатча (`coder`, вкл. ретрай той же сессии и свежую сессию) — у всех `Permission denied: shell` на первом же `git status`; сам оркестратор shell не имеет и mutating git не выполняет. Изменения Task1/Task2 на диске, проверены субагентом файловыми инструментами. Человеку — выполнить вручную:
        1. `git status` (изменены должны быть ровно 2 кодовых файла ниже; untracked-документация не брать)
        2. `git add pvz/src/main/java/io/allitov/pvz/repository/OrderRepository.java pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java`
        3. `git commit -m "pvz: реализовал countClosedOrders"`
        4. Приёмка: `git log -1 --oneline`; `git show --stat HEAD` (ровно 2 файла); `git status` (кодовые файлы чисты). Пуш — за человеком.
    - **agent:** coder
    - **inputs:**
        - Ветка `pvz`; конвенция коммитов по истории: «pvz: <русское причастие>» (последние: «pvz: реализовал deleteClosedOrders», «pvz: реализовал addOrder»).
        - В коммит входят ровно 2 файла: `../../pvz/src/main/java/io/allitov/pvz/repository/OrderRepository.java`, `pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java`.
    - **deps:** [Task2]
    - **acceptance:** `git log -1 --oneline` показывает коммит «pvz: реализовал countClosedOrders»; `git status` чист; в коммите ровно 2 файла из `touched:` Task1/Task2.
    - **description:** Закоммитить изменения двух файлов сообщением «pvz: реализовал countClosedOrders». Ничего дополнительного не коммитить.
    - **touched:**

## Примечания (не задачи)

- Известный посторонний дефект `findByCode` (несовпадение параметров конструктора `Order` с колонками `order_code`/`parcel_article` — `opencode-reviews/pvz-add-order-fix-order.code-review.md:41`) на count НЕ влияет: запрос возвращает скаляр, маппер бинов не задействован. В объём задачи не входит.
- Ребро «пустой список статусов → `in ()` сломанный SQL» недостижимо: `CLOSED_STATUSES` — фиксированная непустая константа (та же оговорка в ревью sibling-задачи).
- После Task2 функция очистки истории становится полностью работоспособной end-to-end: GET `/` покажет реальный `closedCount` в диалоге подтверждения, POST `/orders/cleanup` перестанет всегда уходить в ветку «Нет выданных или отменённых заказов».
