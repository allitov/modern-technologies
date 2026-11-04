# Code review: lab-5-impl-delete-closed-orders — Task2 (сервис `deleteClosedOrders`)

> Отчёт подготовлен сабагентом `code-reviewer` (session `ses_e983ee148ffeGYV1jY2PBU6bv0`), который не имел прав на запись;
> сохранён на диск оркестратором дословно. Дата: 2026-10-23.

**Вердикт: PASS**

- **Объект ревью:** `../../pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java` (изменение не закоммичено, правка в unstaged `git diff`)
- **План-контракт:** `../../opencode-plans/lab-5/lab-5-impl-delete-closed-orders.md`, Task2
- **Зависимость:** Task1 (`OrderRepository.deleteByStatuses`) — застейджен, отревьюен ранее (task1 review PASS)

## Методика

- `git status --porcelain=v1` по репозиторию: в `../../pvz` изменены ровно два файла — `M ` `OrderRepository.java` (staged, Task1, вне скоупа) и ` M` `OrderServiceImpl.java` (unstaged, скоуп Task2). Посторонних правок и untracked-файлов в `pvz/` нет.
- Построчный разбор `git diff -- <OrderServiceImpl.java>`; контекст проверен чтением файла целиком и соседних контрактных файлов: `OrderService.java`, `OrderStatus.java`, staged-дифф `OrderRepository.java`.
- `java-rules.md` в репозитории не найден (glob пуст) — сканирование по критериям контракта из ТЗ ревью.
- Gradle НЕ запускался (по заданию): тесты прогнал coder (`.\gradlew.bat test` — 9 passed / 5 skipped / 0 failed).

## Проверка критериев

1. **Прямой возврат числа удалённых строк — OK.** `OrderServiceImpl.java:64`: `return orderRepository.deleteByStatuses(CLOSED_STATUSES);` — без `.size()` и преобразований. Сигнатура репозитория (staged `OrderRepository.java:31-32`): `int deleteByStatuses(@BindList("statuses") Collection<String> statuses)` + `@SqlUpdate("delete from orders where status in (<statuses>)")`; `@SqlUpdate`→`int` возвращает затронутые строки. `List<String>` совместим с `Collection<String>`; иммутабельность `List.of` не проблема (`@BindList` только итерирует).
2. **Аннотации — OK.** `:61-63` — `@Override` + `@Transactional`, стиль идентичен соседнему `addOrder` (`:67-69`). Импорт `@Transactional` (`:12`) существующий; `@Override` валиден — контракт `OrderService.java:23`.
3. **Нет литералов и обёртки исключения — OK.** Файл прочитан целиком: литералов `"issued"`/`"cancelled"` нет; статусы только `OrderStatus.ISSUED.name().toLowerCase(Locale.ROOT)` / `OrderStatus.CANCELLED.name().toLowerCase(Locale.ROOT)` (`:25-27`). Остальные литералы — pre-existing и вне диффа (`:23` regex, `:74` `"PVZ-%06d"`, `:78` сообщение исключения). В `deleteClosedOrders` нет try/catch; импорт `JdbiException` (`:10`) — существовавший, используется только `addOrder`.
4. **Заглушки и скоуп — OK.** `countClosedOrders()` — по-прежнему `return 0` (`:56-59`); прочие заглушки (`:31-54`) не тронуты (соответствует решению Q1). Unstaged-дифф содержит только ожидаемые правки (+2 импорта, +константа, +`@Transactional`, замена `return 0`); веб-слой, репозиторий и тесты не изменены.
5. **Семантика — OK.** `CLOSED_STATUSES` = `["issued","cancelled"]` в нижнем регистре; javadoc `OrderStatus.java:11-12` подтверждает совпадение db-ключей с именами констант без учёта регистра; в БД значения в нижнем регистре (`status varchar(20) default 'ready'`). `ready` не входит в список. Пограничный случай пустого списка (`in ()`) недостижим — константа уровня класса никогда не пуста.
6. **Импорты — OK.** `OrderStatus` (`:4`) — в алфавитном порядке внутри группы `io.allitov.pvz.model.*` (`Order` < `OrderStatus` < `Report`); `Locale` (`:15`) — `List` < `Locale` < `UUID`; `List` уже был. Стиль согласован с файлом.

## Замечания (не блокеры)

- **Вопрос на проверку (человеку):** функциональная проверка на живой БД (`POST /orders/cleanup` удаляет только `issued`/`cancelled`, `ready` остаётся) по плану опциональна; через UI фича недостижима, т.к. `countClosedOrders()` — заглушка (осознанное решение Q1). Реальное выполнение DELETE покрыто только интеграционно (без Docker — skip).
- **Информационно:** `java-rules.md` отсутствует — правило-скан из него не выполнялся.

## Итог

Все шесть критериев контракта выполнены; изменение минимально и точно соответствует плану Task2.

**Вердикт: PASS**
