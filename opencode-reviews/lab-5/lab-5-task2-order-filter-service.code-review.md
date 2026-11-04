# Code Review: lab-5 / Task2 — OrderFilter + перегрузка listOrders

- **Дата:** 2026-10-23
- **Ревьюер:** GLM-5.3-Flash (субагент-ревьюер, только чтение)
- **Объект:** незакоммиченные изменения рабочего дерева относительно `HEAD` (Task1 предварительно застейджен, Task2 — рабочее дерево); дифф снят командой `git diff HEAD -- pvz/` после `git add` трёх файлов Task2.
- **Контракт:** спека оркестратора + план `../../opencode-plans/lab-5/lab-5-impl-list-orders.md` (Task2).
- **Примечание оркестратора:** субагент-ревьюер не смог записать файл (пермишены сессии); отчёт дословно сохранён оркестратором.

**Вердикт: PASS**

## Что ревьюилось
Незакоммиченные изменения рабочего дерева относительно `HEAD` (Task1 предварительно застейджен, Task2 — рабочее дерево). Дифф снят командой `git diff HEAD -- pvz/` после `git add` трёх файлов Task2.

## 1. Скоуп диффа — PASS
- `../../pvz/src/main/java/io/allitov/pvz/model/OrderFilter.java` — новый файл (26 строк).
- `../../pvz/src/main/java/io/allitov/pvz/service/OrderService.java` — только javadoc + перегрузка + импорт (`OrderService.java:5,18-46`).
- `../../pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java` — только импорт и замена заглушки `listOrders` (`OrderServiceImpl.java:4,48-59`).
- `../../pvz/src/main/java/io/allitov/pvz/repository/OrderRepository.java` — дифф идентичен Task1 и был застейджен до начала Task2 (статус `M ` в worktree без модификаций); Task2 его не менял.
- Отсутствуют изменения: `web/OrderController.java`, `model/Order.java`, `src/test/**` (подтверждено `git status --porcelain` и `git diff HEAD --stat`). Стабы `issueOrder`/`cancelOrder`/`buildReport`/`listByStatus` не тронуты (`OrderServiceImpl.java:33-46,61-64`).

## 2. `OrderFilter` — PASS
- `OrderFilter.java:17-18` — `@Value @Builder`; класс final, поле `search` private final → неизменяемость соблюдена (поле типа `String` — мутабельных полей нет).
- `OrderFilter.java:21-23` — фабрика `of(String)`: `null`/`isBlank()` → `null`, иначе `trim()`. Точно по спеке.
- Русская javadoc на классе и поле присутствует (`OrderFilter.java:6-16`).
- `new OrderFilter(...)` компилируем: конструктор, генерируемый Lombok для связки `@Value @Builder`, доступен из статической фабрики, объявленной внутри самого класса. Java-версия (25, `build.gradle.kts:13`) покрывает `String.isBlank()`.

## 3. Семантика имплементации — PASS
- `OrderServiceImpl.java:49-51` — `listOrders(String)` делегирует в `listOrders(OrderFilter.of(filter))`.
- `OrderServiceImpl.java:53-59` — `search == null` → `orderRepository.findAll()`; иначе `findBySearchTerm("%" + search + "%")`.
- Нормализация: `null` / `""` / `"   "` → `of` даёт `search == null` → `findAll()`; непустой после trim → поиск с `%`-обёрткой. Соответствует критерию 3.
- Инъекция исключена: в сервисе нет SQL-строк; единственная конкатенация — обёртка `%…%` над нормализованной строкой, сам параметр уходит через `@Bind("search")` в `findBySearchTerm` (ILIKE). Это ровно та `%`-обёртка, которую разрешает требование лабы. SQL — только в репозитории.

## 4. Javadoc — PASS
- `OrderService.java:18-31` (`listOrders(String)`) и `OrderService.java:33-46` (`listOrders(OrderFilter)`) — русская, с `@param`/`@return`, стиль зеркалит `addOrder` (`OrderService.java:54-69`: абзацы `<p>`, описание контракта). Контракт соответствует поведению: null/пробельные → все заказы; поиск подстроки без учёта регистра (ILIKE) по коду/телефону/артикулу; «свежие сверху» совпадает с `order by created_at desc, id desc` в репозитории (Task1). Ссылка `{@link OrderFilter#of(String)}` валидна (импорт есть, `OrderService.java:5`).

## 5. Запрещённое — PASS
- В `listOrders` (обе перегрузки) нет try/catch `JdbiException` (остался только в `addOrder`, `OrderServiceImpl.java:87` — вне диффа), нет логирования, нет `@Transactional`, нет custom RowMapper, нет SQL в сервисе. Оформление зеркально `countClosedOrders` (только `@Override`).

## 6. Общие дефекты — PASS
- Перегрузка не создаёт двусмысленности: вызыватели в web-слое типизированы (`OrderController.java:50,54` — параметр `String filter`; `OrderController.java:155` — литерал `""`) и резолвятся в `listOrders(String)` без правок; `listOrders("")` через нормализацию даст `findAll()`, что соответствует прежнему контракту «пустая строка = все заказы».
- Null-safety: `filter` в перегрузке `listOrders(OrderFilter)` формально может быть `null` (NPE на `OrderServiceImpl.java:55`), но ни один существующий вызыватель так не делает, javadoc этого случая не обещает — вне фактических границ кода. Не дефект.
- Информационно (не fail, не блокирует): спецсимволы LIKE (`%`, `_`) во вводе пользователем не экранируются — это соответствует заявленной спеке лабы (только `%`-обёртка) и не является инъекцией (параметризованный bind).

## Замечания
- **Вопрос на проверку (некритично)** [pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java:53-59]: поведение при прямом вызове `listOrders((OrderFilter) null)` — NPE. Контракт намеренно не определён; при желании можно добавить `Objects.requireNonNullElse`-нормализацию или явный javadoc `@throws`, но требований нет — не влияет на вердикт.

## Замечания по инфраструктуре ревью
- Компиляция `.\gradlew compileJava` не выполнена (пермишены окружения); корректность Lombok-связки подтверждена статически. Регрессию прогнал тестер отдельно: 14 тестов GREEN (`--rerun`).
- Изменения вне `pvz` (`../../opencode-plans/lab-5/lab-5-impl-list-orders.md`, `../tasks/lab-5/lab-5-impl-list-orders.md`, отчёт Task1) — артефакты рабочего процесса, код не затрагивают.

---

**Итоговый вердикт: PASS** — все шесть критериев выполнены; дифф ограничен заявленными тремя файлами (плюс предзастейдженный Task1 в репозитории), семантика, javadoc и запреты соответствуют требованиям; блокирующих дефектов не найдено.
