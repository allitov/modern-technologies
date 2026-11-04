# Code Review: lab-5 / Task1 — OrderRepository (список заказов + поиск)

- **Дата:** 2026-10-23
- **Ревьюер:** GLM-5.3-Flash (субагент-ревьюер, только чтение)
- **Объект:** `../../pvz/src/main/java/io/allitov/pvz/repository/OrderRepository.java` — незакоммиченные изменения рабочего дерева (застейджены через `git add -A`, ревью по `git diff --staged`)
- **Контракт:** спека оркестратора + план `../../opencode-plans/lab-5/lab-5-impl-list-orders.md` (Task1) + `tasks/lab-5-impl-list-orders.md:19`
- **Правила:** `java-rules.md` в корне репозитория отсутствует (проверено glob) — скан по критериям задачи и общим правилам.
- **Примечание оркестратора:** субагент-ревьюер не смог записать файл (пермишены сессии); отчёт дословно сохранён оркестратором.

## 1. Объём диффа (критерий 1) — СООТВЕТСТВУЕТ

`git status --porcelain` до стейджа:

```
 M pvz/src/main/java/io/allitov/pvz/repository/OrderRepository.java
?? opencode-plans/lab-5-impl-list-orders.md
?? tasks/lab-5-impl-list-orders.md
```

`git diff --staged --stat` после `git add -A`:

```
 opencode-plans/lab-5-impl-list-orders.md           | 98 ++++++++++++++++++++++
 .../io/allitov/pvz/repository/OrderRepository.java | 15 +++-
 tasks/lab-5-impl-list-orders.md                    | 19 +++++
```

- **Код** затронут ровно в одном файле — `OrderRepository.java` (+14/−1). Ни один другой `.java` не изменён; `git diff --staged` по `../../pvz` содержит единственный хунк этого файла.
- Два дополнительных файла — markdown-артефакты планирования (план и постановка задачи), не код продукта. Ключевой риск «кодер не смог выполнить git status и мог задеть чужие файлы» снят. Деталь для коммитящего человека — в находке N1.

## 2. Пословная сверка SQL с контрактом (критерии 2, 6) — СООТВЕТСТВУЕТ

- **Константа** `OrderRepository.java:18-20` — дословно совпадает со спекой: `id, order_code as code, coalesce(phone, '') as phone, coalesce(parcel_article, '') as article, coalesce(cell, '') as cell, status, created_at as "createdAt"`.
- **`findAll()`** `OrderRepository.java:22-23` — дословно: `ORDER_COLUMNS + " from orders order by created_at desc, id desc"`.
- **`findBySearchTerm`** `OrderRepository.java:25-28` — дословно; три `ilike :search` по `order_code`/`phone`/`parcel_article`, та же сортировка.
- **`findByCode`** `OrderRepository.java:30-31` — дословно: `ORDER_COLUMNS + " from orders where order_code = :code"`.
- **Bind:** `:search` трижды связывается одним `@Bind("search")` (`OrderRepository.java:28`) — это корректный JDBI-паттерн (одно имя = один параметр). Конкатенации пользовательского ввода в SQL нет; `%`-обёртка — обязанность сервиса по плану (`opencode-plans/lab-5-impl-list-orders.md:52,83`), её отсутствие в репозитории соответствует критерию 3.
- **PostgreSQL-синтаксис:** `ilike` — валидный оператор PG; `coalesce(x, '')` — валиден; алиас `"createdAt"` в двойных кавычках обязателен (без кавычек PG свернул бы его в `createdat`); завершающая `;` старого `findByCode` удалена — безвредно и даже чище. Все колонки существуют в схеме `V1__create_orders.sql:2-9` (`order_code`, `phone`, `parcel_article`, `cell`, `status`, `created_at timestamptz`); `issued_at` намеренно не выбирается — лишних колонок в результате нет.
- **Инъекция:** все пользовательские данные идут bind-параметрами через prepared statement; проверочный вектор `' OR '1'='1` даёт пустой список, как и требует план (`opencode-plans/lab-5-impl-list-orders.md:5`).

## 3. Отсутствие логики в репозитории (критерий 3) — СООТВЕТСТВУЕТ

Интерфейс содержит только константу и абстрактные методы. В изменённых строках нет: ветвлений, `%`-склейки, литералов статусов (`ready`/`issued`/`cancelled`), try/catch, логирования. Соседние методы (`insert`, `updateCode`, `deleteByStatuses`, `countByStatuses`, `OrderRepository.java:33-48`) не тронуты. Выбор «все/по подстроке» вынесен в сервис (план, Q1) — репозиторий остаётся «тупым параметризованным SQL» (`tasks/lab-5-impl-list-orders.md:19`).

## 4. Order.java не изменён (критерий 4) — СООТВЕТСТВУЕТ

`../../pvz/src/main/java/io/allitov/pvz/model/Order.java` отсутствует в `git status` и в staged-диффе пуст. Требование плана «Order.java не менять» (`opencode-plans/lab-5-impl-list-orders.md:19`) выполнено.

## 5. Корректность маппинга (критерий 5) — СООТВЕТСТВУЕТ

- Lombok `@AllArgsConstructor` на `Order` (`Order.java:20-21`) даёт конструктор `(long id, String code, String phone, String article, String cell, String status, Instant createdAt)` (`Order.java:26-57`).
- Алиасы результирующего набора: `id`, `code`, `phone`, `article`, `cell`, `status`, `createdAt` — точное совпадение имён со всеми семью параметрами; `@RegisterConstructorMapper(Order.class)` (`OrderRepository.java:15`) матчит по имени. Латентный дефект N2 из `opencode-reviews/pvz-add-order-fix-order.code-review.md:41` (`UNMATCHED_CONSTRUCTOR_PARAMETER` при первом чтении) устранён по всем трём читающим методам.
- `timestamptz` → `Instant`: встроенные java-time мапперы JDBI 3.52.1 поддерживают `Instant`; колонка `created_at timestamptz not null` (`V1__create_orders.sql:8`) совместима. `id` → `long` из `bigint identity` — корректно.

## Находки

- **PASS [OrderRepository.java:18-20]** Константа `ORDER_COLUMNS` дословно по контракту; алиасы чинят маппер, `coalesce` — NULL→`''`, кавычки вокруг `"createdAt"` обязательны и стоят.
- **PASS [OrderRepository.java:22-23,25-28]** `findAll`/`findBySearchTerm` дословно по контракту; bind-параметры, инъекции нет; сортировка `created_at desc, id desc` даёт детерминированный порядок при равных `created_at`.
- **PASS [OrderRepository.java:30-31]** `findByCode` переведён с `select *` на `ORDER_COLUMNS` — устранён задокументированный дефект `pvz-add-order-fix-order.code-review.md:41`.
- **PASS [OrderRepository.java:12]** Импорт `java.util.List` добавлен под возвращаемые типы; `Optional`/`Collection` использовались ранее.
- **N1 [к сведению, для коммитящего]** Рабочее дерево содержит два незатреканных markdown-артефакта: `../../opencode-plans/lab-5/lab-5-impl-list-orders.md` и `../tasks/lab-5/lab-5-impl-list-orders.md`. Это не код и не часть диффа Task1, но при коммите решите сознательно: включать их в коммит или нет. Буквальная формулировка acceptance «git status не показывает изменений вне OrderRepository.java» выполнена в части кода; файлы планирования — санкционированные артефакты сессии.
- **N2 [не проверяемо ревьюером]** Сборка/тесты не прогонялись: вызов `gradlew compileJava` заблокирован сандбоксом (прецедент — `pvz-add-order-fix-order.code-review.md:43`). Статически синтаксис валиден: конкатенация compile-time констант допустима в значении аннотации. Рантайм-проверка маппинга чтения ещё ни разу не выполнялась — ручная проверка после Task2 уже запланирована (`opencode-plans/lab-5-impl-list-orders.md:96`). Регрессию прогнал тестер отдельно: 14 тестов GREEN, покрытием изменённой ветки тесты не обладают.
- **N3 [к сведению, вне скоупа диффа]** Устаревшая javadoc `Order.java:16` («строкой yyyy-MM-dd HH:mm») противоречит полю `Instant` (`Order.java:57`). Файл не менялся, дефект вне диффа и уже зафиксирован вне скоупа в плане (`opencode-plans/lab-5-impl-list-orders.md:98`). Действий не требует.

## VERDICT: **PASS**

Все шесть критериев PASS выполнены: дифф кода ограничен `OrderRepository.java`; SQL дословно совпадает с контрактом; bind-параметры без конкатенации ввода; логики в репозитории нет; `Order.java` не тронут; алиасы точно соответствуют параметрам конструктора `Order` и колонкам схемы, чиня задокументированный `UNMATCHED_CONSTRUCTOR_PARAMETER`. Блокирующих находок нет; N1 — решение по markdown-артефактам за коммитящим человеком, N2 — рантайм-подтверждение остаётся за прогоном тестера/ручной проверкой после Task2.
