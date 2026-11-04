# Code review: lab-5 t1 — OrderRepository.updateStatusAndIssuedAt

- **Дата:** 2026-10-26
- **Ревьюер:** code-reviewer (сессия ses_e89686edaffeR5zSc7CaCZwM2v)
- **Вердикт: PASS**
- Примечание: у сессии ревьюера были отключены shell и запись файлов — отчёт сохранён оркестратором
  дословно по тексту ревьюера; вместо `git diff` использована grep-инспекция репозитория.

---

## ВЕРДИКТ: **PASS**

Нарушений по существу кода нет.

### Проверка (метод)

Shell-команды в сессии запрещены, поэтому `git diff`/`git status` не выполнены. Проверка инспекцией
содержимого: файл прочитан целиком; grep по маркерам (`updateStatusAndIssuedAt`, `issued_at`,
`issuedAt`, `import java.time.Instant`) по всему репозиторию — попадания только в целевом файле и
markdown-документах (план/задача).

### Критерии — все PASS

1. **Ровно один новый метод** — `OrderRepository.java:55-65`, вставлен между `updateCode` (:52-53) и
   `deleteByStatuses` (:67-68). Соседние методы и их оформление не тронуты. Единственное попадание
   метода в `src` — `OrderRepository.java:64`; в сервисе/контроллере вызовов нет (t2/t3 вне scope t1).
2. **SQL — точная строка** — `OrderRepository.java:63`:
   `@SqlUpdate("update orders set status = :status, issued_at = :issuedAt where order_code = :code")`
   — одиночный литерал, без конкатенации и text block; named binds `:code`/`:status`/`:issuedAt` все
   покрыты `@Bind`; литералов статусов в аннотации нет; колонки `cell` в SQL нет.
3. **Типы** — возвращаемый `int` (0 или 1 строка), сигнатура
   `(String code, String status, Instant issuedAt)` — точно как в acceptance плана
   (`lab-5-impl-issue-order.md:104-106`).
4. **Javadoc** — русский, `@param` ×3 + `@return` (:55-62); стиль совпадает с соседями (named binds,
   перенос параметров как в `insert` :49-50).
5. **Импорт** — `import java.time.Instant;` в `OrderRepository.java:11`.

### Логика

- Колонка `issued_at` реально существует: `V1__create_orders.sql:9` (`timestamptz`, nullable);
  `Instant` ↔ `timestamptz` — штатный биндинг JDBI/PG. Обновление параметризовано — инъекция
  исключена.
- Признаков правки миграций/сервиса/контроллера/JdbiConfig/build-файлов/тестов нет: в `../../pvz/src`
  `Instant` встречается только в преждесуществующем `Order.java:7,57` и целевом репозитории; файлов
  миграции ровно один (V1), содержимое совпадает с задокументированным исходным состоянием
  (`lab-5-impl-list-orders.md:21`).

### Вопрос на проверку (не блокирует PASS)

- **[tooling]** Критерий «никакие другие файлы не изменены» подтверждён grep-инспекцией, а не
  `git diff` — shell недоступен. Рекомендуется прогнать `git diff --stat` гуману.
- **[tooling]** Acceptance «`gradlew.bat compileJava` BUILD SUCCESSFUL» не проверен ревьюером
  (shell недоступен). Статически: сигнатура и аннотации соответствуют JDBI 3 SqlObject API, все
  символы импортированы — препятствий к компиляции не видно. Компиляцию подтвердили независимо:
  coder (`BUILD SUCCESSFUL in 27s`) и регрессионный прогон тестера (compileJava UP-TO-DATE + 9/9
  тестов зелёные).
