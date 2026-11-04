# Code review: lab-5, Task1 — реализация `Report buildReport()`

**Дата:** 2026-10-26
**Ревьюер:** GLM-5.3-Flash (read-only, без сборки)
**Вердикт: PASS**

> Примечание оркестратора: ревью выполнено сабагентом `code-reviewer` (сессия ses_e89267ad1ffeWs6Z6ORuxDnG6s),
> вердикт PASS. Запись файла из его песочницы была отклонена permission-системой, поэтому содержимое сохранено
> оркестратором дословно из ответа ревьюера.

## Объём проверки

Модуль `pvz`, Task1 плана `../../opencode-plans/lab-5/lab-5-impl-build-report.md`. Проверены некоммитнутые изменения
рабочего дерева (`git status --porcelain` + `git diff`); изменены ровно два файла, других правок в коде нет:

- `../../pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java`
- `../../pvz/src/main/java/io/allitov/pvz/service/OrderService.java`

Примечание: `java-rules.md` в репозитории отсутствует, поэтому проверка выполнена по контракту плана
(acceptance-критерии) и общим критериям корректности Java/Spring/Jdbi.

## Найденные замечания

Отсутствуют. Ни подтверждённых, ни подозреваемых дефектов в изменённых строках не обнаружено.

## Проверка acceptance-критериев

| Критерий | Статус | Подтверждение |
|---|---|---|
| Нет `return null`; результат через `Report.builder()...build()` из ровно трёх `countByStatuses` | OK | `OrderServiceImpl.java:72–82`: три вызова на :78, :79, :80 — по одному на `ready`/`issued`/`cancelled`; `return null` удалён |
| Статусы как локальные переменные `OrderStatus.X.name().toLowerCase(Locale.ROOT)` | OK | `OrderServiceImpl.java:74–76`; стиль идентичен соседям `issueOrder` (:42) и `cancelOrder` (:61) |
| Без `@Transactional` (read-only) | OK | `OrderServiceImpl.java:72` — аннотации нет, как у read-only соседа `countClosedOrders` (:104–107) |
| Правильные ключи БД | OK | `READY/ISSUED/CANCELLED.name().toLowerCase(Locale.ROOT)` → `ready`/`issued`/`cancelled`; `Locale.ROOT` исключает locale-зависимое приведение регистра. Javadoc `OrderStatus.java:11` подтверждает: ключи БД = имена констант в нижнем регистре |
| Правильные поля builder'а, без перепутывания | OK | `readyCount`←ready, `issuedCount`←issued, `cancelledCount`←cancelled (`OrderServiceImpl.java:78–80`); поля `int readyCount/issuedCount/cancelledCount`, `@Data @Builder` — `Report.java:12–29` |
| Новых импортов нет | OK | Diff не содержит правок импортов; `Report` (:6), `OrderStatus` (:5), `List` (:16), `Locale` (:17) уже были |
| `OrderRepository.java` не изменён | OK | Файл отсутствует в `git status --porcelain`; :80–81 соответствуют спецификации: `@SqlQuery("select count(*) from orders where status in (<statuses>)") int countByStatuses(@BindList("statuses") Collection<String> statuses);` — сигнатура совпадает с вызовами (`List.of(String)` — валидный `Collection<String>`; непустой список, проблема пустого `in ()` в Jdbi `@BindList` не возникает) |
| `src/test` не тронут, тесты не писались | OK | В `git status --porcelain` нет ни одного файла под `../../pvz/src/test` |
| Контроллер не изменён | OK | `web/OrderController.java:210` (`model.addAttribute("report", orderService.buildReport())`) — вне diff'а; шаблон `templates/report.html:12–14` разыменовывает `report.readyCount/issuedCount/cancelledCount`, теперь заполненные → 500 из-за `report == null` устранён |
| Javadoc у `OrderService.buildReport()` | OK | `OrderService.java:39–45`: текст в точности как в плане, корректно оформлен (`{@code}`, `{@link Report}`; `Report` импортирован на :5), по-русски, в стиле остальных методов файла |
| `countClosedOrders()`/`deleteClosedOrders()` без Javadoc | OK | `OrderService.java:77` и :79 — без изменений (вне объёма по плану, намеренно) |
| Форматирование | OK | 4 пробела, пустая строка между методами (:71, :83), продолжение builder-цепочки на 12 пробелах — консистентно |
| Компиляция | OK (со слов имплементатора) | `gradlew compileJava` → BUILD SUCCESSFUL; статически типы сходятся (Lombok `@Builder` генерирует fluent-методы; типы аргументов совпадают) |

## Логическая корректность

- Семантика отчёта совпадает с требованием: три независимых запроса `select count(*) ... where status in (?)`,
  по одному на статус; данные только читаются.
- Thread-safety N/A: метод не имеет состояния, репозиторий — stateless-интерфейс Jdbi.
- Граничные случаи: пустая таблица → счётчики `0` (JDBC `count(*)` возвращает 0, примитив `int` в модели) — поведение корректно.

## Итог

**PASS** — изменения в точности соответствуют контракту Task1; критерии приёмки выполнены; регрессий в затронутых
строках не обнаружено.
