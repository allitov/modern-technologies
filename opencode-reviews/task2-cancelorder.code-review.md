# Code-review: Task2 — реализация `cancelOrder` (lab-5-impl-cancel-order)

**Вердикт: PASS**

**Дата:** 2026-10-26 · **Ревьюер:** code-reviewer (сессия ses_e893fd3ccffeZvRsERDR46slGK) · **База:** `git diff --staged` (изменения застейджены перед ревью)

> Примечание: ревьюер не смог записать этот файл сам (запрет записи в его сессии) — отчёт сохранён оркестратором дословно из ответа ревьюера.

## Объём ревью

Примечание: файл `java-rules.md` в рабочем дереве не обнаружен (glob `**/java-rules.md` — 0 совпадений, внешние каталоги недоступны). Ревью выполнено по чек-листу оркестратора и общим критериям Java-качества.

Дифф (staged, 6 файлов):

| Файл | Изменение | Отнесение |
|---|---|---|
| `pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java` | +15/−1 — стаб `cancelOrder` заменён реализацией | Task2 (целевой) |
| `pvz/src/main/java/io/allitov/pvz/service/OrderService.java` | +12 — Javadoc к `cancelOrder` | Task2 (целевой) |
| `pvz/src/main/java/io/allitov/pvz/repository/OrderRepository.java` | +10 — метод `updateStatus` | Task1, уже отревьюен (`opencode-reviews/task1-updatestatus.code-review.md`) — дефектом не считается |
| `opencode-plans/lab-5-impl-cancel-order.md` | +107 — план (док) | документация, не код |
| `tasks/lab-5-impl-cancel-order.md` | +16 — постановка задачи (док) | документация, не код |
| `opencode-reviews/task1-updatestatus.code-review.md` | +41 — отчёт ревью Task1 (док) | документация, не код |

**Проверка п.3 (scope):** посторонних кодовых изменений нет. Веб-слой (`OrderController.java`, шаблоны) не затронут — в диффе отсутствует. Мёртвого кода не добавлено: стаб `return false;` полностью заменён, новых неиспользуемых импортов/методов нет.

## Проверка п.1 — реализация `cancelOrder`

`OrderServiceImpl.java:54-70` — построчное сопоставление с `issueOrder` (`:35-52`):

| Шаг | `issueOrder` | `cancelOrder` | Совпадение |
|---|---|---|---|
| `@Transactional` | `:36` | `:55` | ✅ |
| `findByCode` + `found.isEmpty() → false` | `:38-41` | `:57-60` | ✅ |
| guard `OrderStatus.READY.name().toLowerCase(Locale.ROOT)` | `:42-45` | `:61-64` | ✅ идентично |
| update | `updateStatusAndIssuedAt(code, "issued", Instant.now())` | `updateStatus(code, "cancelled")` | ✅ различие ровно то, что требует спека |
| результат | `return updated > 0;` | `return updated > 0;` | ✅ |

Ключевые отличия от `issueOrder` — корректные по постусловию:
- **Нет `Instant.now()`** — в `cancelOrder` отсутствует.
- **Нет `issued_at`** — вызывается `orderRepository.updateStatus` (`OrderRepository.java:74-75`), SQL: `update orders set status = :status where order_code = :code` — единственная изменяемая колонка `status`. Постусловие «изменяется ТОЛЬКО `status`; `issued_at` не заполняется» выполняется на уровне SQL.
- Вызов `updateStatus(orderCode, OrderStatus.CANCELLED.name().toLowerCase(Locale.ROOT))` соответствует реальной сигнатуре `int updateStatus(@Bind("code") String, @Bind("status") String)` (`OrderRepository.java:75`) — типы и порядок аргументов верны.
- `"cancelled"` — корректный ключ БД: enum-константа `CANCELLED` существует (`OrderStatus.java:31`); lower-case ключи согласованы с конвенцией проекта (`issueOrder`, `CLOSED_STATUSES` `:28-31`, Javadoc `listByStatus`).
- Импорты (`Optional`, `OrderStatus`, `Locale`, `@Transactional`) уже присутствовали (`:5`, `:13`, `:17`, `:18`) — новых не требуется, как и сказано в плане.

**Соответствие спеке (критерии приемки):**
- заказ не найден → `false` (`:58-60`) ✅
- статус ≠ `ready` → `false`, изменения не производятся (`:61-64`) ✅
- иначе → UPDATE в `cancelled`, `true` при `updated > 0` (`:65-69`) ✅
- репозиторий — только сохранение, без логики (план прямо запрещает `and status = 'ready'` в SQL; проверка статуса — в сервисе) ✅

Логика `true/false` соответствует спецификации; инвертированных условий и неверных границ нет.

## Проверка п.2 — Javadoc интерфейса

`OrderService.java:25-36`:
- **Стиль** зеркалит Javadoc `issueOrder` (`:11-22`) 1:1 по структуре: сводная строка → `<p>`-блок с условиями → `@param` с примером `PVZ-000042` → `@return` с обеими ветками. Терминология и разметка `{@code ...}` совпадают.
- **Контракт описан корректно:** переводит в `cancelled` (`:26`); отмена только из `ready`, для отсутствующего кода/другого статуса «изменения не производятся» (`:28-30`); «Дата выдачи не заполняется» (`:30`) — постусловие про `issued_at` зафиксировано явно; `@return` (`:33-35`) — `true` при отмене, `false` при отсутствии кода или статусе ≠ `ready`. Расхождений с фактическим поведением реализации нет.

## Проверка п.4 — web-слой и точки вызова

Единственный вызов — `OrderController.java:185`: `if (orderService.cancelOrder(code))` — сигнатура `boolean cancelOrder(String)` совпадает с интерфейсом (`OrderService.java:37`); ветвление по boolean уже существовало. Файл контроллера в диффе отсутствует — изменений не требуется, как и сказано в плане.

## Проверка п.5 — сборка и тесты

- Coder отчитался: `cd pvz && gradlew.bat compileJava` и `gradlew.bat test --tests "*OrderServiceImplTest"` — оба `BUILD SUCCESSFUL`; параллельно тестер повторил регресс-прогон с `--rerun`: 7 тестов, 0 падений — принято как подтверждение.
- Отсутствие тестов на `cancelOrder` (grep по `src/test`: ни одного упоминания `cancel`) — **не дефект**: план (`opencode-plans/lab-5-impl-cancel-order.md:5,102`) прямо фиксирует «тесты писать не надо — прямое требование задачи», дыра покрытия осознанно принята человеком и задокументирована.

## Замечания

Дефектов (FAIL-уровня) не найдено. Одно нефункциональное наблюдение вне требований спеки:

- **[Вопрос на проверку — не блокирует]** `OrderServiceImpl.java:57-68` — между guard-чтением статуса (`findByCode`) и `updateStatus` нет условия `and status = 'ready'` в WHERE, поэтому при гонке двух конкурентных операций (например, `issueOrder` и `cancelOrder` для одного заказа) возможен перевод из уже изменённого статуса (TOCTOU). Это **не отклонение**: план запрещает добавлять условие в SQL (`opencode-plans/lab-5-impl-cancel-order.md:42`), реализация зеркалит существующий паттерн `issueOrder`, и спека описывает ровно последовательность «прочитал → проверил → обновил». Для лабораторного контекста риск номинальный. Передавать в бэклог на усмотрение человека.

## Итог

| Пункт проверки | Результат |
|---|---|
| 1. Реализация зеркалит `issueOrder` | ✅ |
| 2. Javadoc по стилю и контракту | ✅ |
| 3. Scope диффа ограничен (Task1-репозиторий + 2 файла + доки) | ✅ |
| 4. Нет web-изменений / мёртвого кода; логика true/false по спеке | ✅ |
| 5. Сборка/тесты (по отчётам coder + регресс тестера) | ✅ |

**PASS** — реализация `cancelOrder` (`OrderServiceImpl.java:54-70`) и Javadoc (`OrderService.java:25-36`) точно соответствуют спецификации lab-5 и плану Task2; постусловие «изменяется только `status`, `issued_at` не заполняется» гарантируется узким SQL `updateStatus`; побочных изменений нет.
