# Ревью: lab-5-impl-count-closed-orders, Task 1 — `countByStatuses` в OrderRepository

- **Проект:** C:\Users\litov\JavaProjects\modern-technologies (ветка `pvz`, несохранённые рабочие изменения)
- **Файл:** `../../pvz/src/main/java/io/allitov/pvz/repository/OrderRepository.java`
- **Дата:** 2026-10-23
- **Метод ревью:** `git status --porcelain`, `git diff` (рабочие изменения, не staged), чтение файла, проверка `../../pvz/build.gradle.kts`.
- **Примечание:** отчёт составлен code-reviewer'ом; сохранён на диск оркестратором (среда ревьюера отклонила запись файла, вердикт и текст — дословно от ревьюера).

## Суть изменения
Diff содержит ровно один хунок — добавление одного метода после `deleteByStatuses`:
```java
@SqlQuery("select count(*) from orders where status in (<statuses>)")
int countByStatuses(@BindList("statuses") Collection<String> statuses);
```

## Проверка по критериям
### 1 — PASS
Ровно один новый метод `countByStatuses` (`OrderRepository.java:34-35`); SQL и сигнатура совпадают с контрактом посимвольно; `@SqlQuery` одной строкой; метод сразу после `deleteByStatuses` (`:31-32`), стиль файла соблюдён.
### 2 — PASS
В diff нет вхождений `'issued'`/`'cancelled'`/`'ready'`; единственный новый литерал — сам SQL; статусы передаются параметром `@BindList`.
### 3 — PASS
Импорты не менялись: `SqlQuery` (`:8`), `BindList` (`:6`), `Collection` (`:11`) были ранее. `JdbiRepositoryConfig` не изменялся.
### 4 — PASS
Хунок — только добавление строк; отсутствие `;` в `updateCode` (`:28`) сохранено, иной косметики нет.
### 5 — PASS
`git status --porcelain`: единственный изменённый кодовый файл — `OrderRepository.java`; staged изменений нет. Untracked `opencode-plans/*.md`, `tasks/*.md` — документация, не код.
### 6 — PASS
JDBI 3.52.1 подтверждена (`pvz/build.gradle.kts:39-42`). `count(*)` (bigint в PG) → примитив int: встроенный маппер JDBI через `ResultSet.getInt`, драйвер PG корректно сужает; NULL невозможен. `@BindList` с `Collection<String>` корректен, паттерн идентичен `deleteByStatuses`.

## Примечания (не блокируют)
- Вопрос на проверку: пустая коллекция в `@BindList` бросает исключение (onEmpty = THROW по умолчанию) — совпадает с поведением `deleteByStatuses`, соответствует контракту (план: ребро недостижимо, `CLOSED_STATUSES` — фиксированная непустая константа), не fail.

## Итоговый вердикт
**PASS** — проблем не найдено. Все 6 критериев выполнены.
