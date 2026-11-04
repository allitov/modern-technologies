# Code Review: lab-5-impl-delete-closed-orders / Task1

> Отчёт подготовлен сабагентом `code-reviewer` (session `ses_e984835a3ffe3nm7pP32YSS4tl`), который не имел прав на запись;
> сохранён на диск оркестратором дословно. Дата: 2026-10-23.

**Файл:** `../../pvz/src/main/java/io/allitov/pvz/repository/OrderRepository.java`
**Дата:** 2026-10-23
**Вердикт:** **PASS**

## Объём проверки

- `git diff --staged --stat`: только `OrderRepository.java`, **+5 строк**, удалений нет. (Файл застейджен по регламенту ревью; расстейдживать не нужно — изменения и так были незакоммичены.)
- `git diff --stat` (unstaged): пусто — незастейдженных правок нет.
- Прочее в `git status`: untracked `../../opencode-plans/lab-5/lab-5-impl-delete-closed-orders.md` и `tasks/` — артефакты рабочего процесса (план-контракт, задачи), к коду модуля `pvz` отношения не имеют.

```diff
+import org.jdbi.v3.sqlobject.customizer.BindList;
+import java.util.Collection;
+
+    @SqlUpdate("delete from orders where status in (<statuses>)")
+    int deleteByStatuses(@BindList("statuses") Collection<String> statuses);
+
```

## Проверка по критериям контракта (Task1)

**1. Ровно один новый метод, сигнатура и SQL — ✅ PASS.** `OrderRepository.java:31-32` — метод побайтно совпадает с эталоном из плана: имя `deleteByStatuses`, `int`, `@BindList("statuses") Collection<String>`; плюс ровно два новых импорта — `BindList` (`:6`) и `Collection` (`:11`).

**2. Нет статусных литералов и логики — ✅ PASS.** Файл прочитан полностью (34 строки): `'issued'`/`'cancelled'` отсутствуют, логики выбора статусов нет — «тупой» параметризованный запрос, список приходит параметром.

**3. Ничего больше не изменено — ✅ PASS.** Staged diff — единственный файл, unstaged diff пуст; тесты, веб-слой, сервис, миграции не тронуты; внутри `../../pvz` untracked-файлов нет.

**4. Корректность `@BindList` в JDBI 3.52.1 — ✅ PASS.** Версия подтверждена (`pvz/build.gradle.kts:39-42`, `jdbi3-sqlobject:3.52.1`); пакет `org.jdbi.v3.sqlobject.customizer` верный; компиляция (BUILD SUCCESSFUL) подтверждает резолв. Плейсхолдер корректен: `@BindList` работает с угловым `<name>`, имена `statuses` ↔ `<statuses>` совпадают. `int` у `@SqlUpdate` — число затронутых строк, как требует контракт.

**Замечание (не блокирует):** пустая коллекция при дефолтном `EmptyHandling.VOID` даст `in ()` → SQL-ошибка на PostgreSQL. Вызыватель (Task2) передаёт константу из двух элементов — **некритично**, недостижимо по контракту. Опционально на будущее: `onEmpty = EmptyHandling.NULL`.

**5. Стиль — ✅ PASS.** Импорты в алфавитном порядке (`Bind` < `BindList`, `customizer` < `statement`, `Collection` < `Optional`); метод после `updateCode` с пустой строкой; однострочный `@SqlUpdate` для короткого SQL — как у соседей.

**Косметика (nit, не блокирует):** добавлена пустая строка перед `}` интерфейса (`OrderRepository.java:33`) — допустимо.

## Логическая проверка

- Метод — ровно то, что нужно Task2: `deleteByStatuses(CLOSED_STATUSES)` вернёт число удалённых строк. ✅
- Инвертированных условий/неверных границ нет; `ready` при корректном списке не затрагивается. ✅

| # | Критерий | Вердикт |
|---|----------|---------|
| 1 | Один метод по контракту | PASS |
| 2 | Нет литералов/логики | PASS |
| 3 | Ничего лишнего | PASS |
| 4 | `@BindList` корректен | PASS |
| 5 | Стиль | PASS |

---

## ВЕРДИКТ: PASS

Все 5 критериев контракта выполнены; подтверждённых проблем нет — только два некритичных замечания (пустая коллекция `@BindList` — недостижимо по контракту, и пустая строка перед `}` — косметика).
