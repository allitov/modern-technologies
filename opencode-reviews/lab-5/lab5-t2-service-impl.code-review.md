# Code review: lab-5 t2 — реализация issueOrder в OrderServiceImpl

- **Дата:** 2026-10-26
- **Ревьюер:** code-reviewer (сессия ses_e89624114ffezSXof2parq7b0c)
- **Вердикт: PASS**
- Примечание: у сессии ревьюера были отключены shell-запуск и запись файлов — отчёт сохранён
  оркестратором дословно по тексту ревьюера.

---

# Code review: lab-5, задача t2 — реализация `issueOrder` в `OrderServiceImpl`

**Вердикт: PASS**

**Diff:** staged-изменения (`git add -A` → `git diff --staged`), файлы задачи t2:
- `../../pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java` (реализация `issueOrder`)
- `../../pvz/src/main/java/io/allitov/pvz/service/OrderService.java` (Javadoc в интерфейсе)

В staged также попали `OrderRepository.java` (задача t1 — отревьюена ранее, здесь использована
только для проверки постусловия), plan/review/task markdown-файлы (артефакты процесса).
Продакшн-код t1 повторно не ревьюился.

## Проверка критериев PASS

### 1. Алгоритм точно по контракту — OK
`OrderServiceImpl.java:37-52`:
- `findByCode(orderCode)` → `found.isEmpty()` → `return false;` (:38-41) ✓
- статус ≠ `OrderStatus.READY.name().toLowerCase(Locale.ROOT)` → `return false;` (:42-45) ✓
- иначе один UPDATE через параметризованный метод репозитория
  `updateStatusAndIssuedAt(orderCode, ISSUED-ключ, Instant.now())` → `return updated > 0;` (:46-51) ✓

Склейки SQL строками в новом коде нет: сервис SQL не содержит, запрос — аннотированный
`@SqlUpdate` репозитория (`OrderRepository.java:63`, проверено по факту:
`"update orders set status = :status, issued_at = :issuedAt where order_code = :code"`).

Совпадение сигнатур проверено по реальным файлам:
- `Optional<Order> findByCode(@Bind("code") String code)` — `OrderRepository.java:41-42` ✓
- `int updateStatusAndIssuedAt(String, String, Instant)` — `OrderRepository.java:63-65`;
  вызов с 3 аргументами совместим ✓
- `Order.getStatus()` → `String` (Lombok `@Data`, `Order.java:18,52`) — `.equals(ready)` корректен ✓

### 2. Транзакционность и обработка ошибок — OK
- `@Transactional` на `issueOrder` — `OrderServiceImpl.java:36` ✓
- try/catch вокруг `JdbiException` в `issueOrder` отсутствует ✓ (существующий try/catch в
  `addOrder` :100-107 — предсуществующий, контрактом не затрагивается)
- Для «не найден / не ready» исключения не бросаются — только `return false` ✓ (согласуется с
  `OrderController.issue`, который по `false` сам показывает flash-сообщение — план, «Контекст»)

### 3. Ключи статусов — OK
Только `OrderStatus.READY.name().toLowerCase(Locale.ROOT)` (:42) и
`OrderStatus.ISSUED.name().toLowerCase(Locale.ROOT)` (:48) — тот же паттерн, что в
предсуществующем `CLOSED_STATUSES` (:28-31). Grep по `OrderServiceImpl.java` на литералы
`"ready"|"issued"|"cancelled"` — 0 совпадений. `{@code ready}`/`{@code issued}` в новом Javadoc
интерфейса — документация ключей БД в стиле соседнего `listByStatus` (`OrderService.java:50`),
к логике отношения не имеет.

### 4. Слово «cell» — OK
Grep по `pvz/.../service/**`: `cell` только в предсуществующем `addOrder`
(`OrderService.java:73,78`, `OrderServiceImpl.java:97,101`) — в новые строки t2 не попадает.
Новый Javadoc формулирует по-русски: «Поле ячейки хранения не изменяется» (`OrderService.java:16`).

### 5. Чужой код не тронут — OK
`git diff --staged --stat`: из java-файлов изменены только `OrderRepository.java` (t1),
`OrderService.java`, `OrderServiceImpl.java`. Контроллер, шаблоны, миграции, `../../build.gradle.kts`,
тесты — не изменены. `cancelOrder` (:54-57) и `buildReport` (:59-62) — прежние заглушки,
не затронуты; остальные методы не изменены.

### 6. Javadoc интерфейса — OK
`OrderService.java:11-22`: русский текст, `@param orderCode`, `@return` — стиль идентичен
соседним `listOrders`/`listByStatus`/`addOrder`. В реализации — `@Override` без Javadoc
(`OrderServiceImpl.java:35`) — по контракту t2.

### 7. Постусловие «изменяются только status и issued_at» — OK
SQL (`OrderRepository.java:63`): ровно требуемая строка, `cell` в SQL отсутствует —
постусловие выполнено по построению.

### 8. Сводимость со спецификацией `../../tasks/lab-5/lab-5-impl-issue-order.md` — OK
| Требование | Реализация |
|---|---|
| «получить текущий статус заказа с указанным кодом» | `findByCode` (:38) |
| «не найден или статус уже не ready — вернуть false» | :39-41, :43-45 |
| «перевести в issued и записать дату выдачи» одним запросом | один UPDATE :46-50 |
| «true, если выдан; false — иначе» | `return updated > 0` (:51) |
| «изменены status и issued_at, cell не изменяется» | SQL `OrderRepository.java:63` |
| «параметризованный запрос, SQL не склеивать» | named bind-параметры JDBI |
| «Тесты писать не надо» | тесты не добавлены |
| «репозиторий без логики» | t1 без проверки статуса в WHERE (отревьюено в t1) |

План (t2): `Instant.now()` в сервисе (Q2) ✓ (:49); ранние возвраты в стиле класса ✓;
out-of-scope (`cancelOrder` и пр.) не тронут ✓.

## Находки

### Нарушений нет
Scan `../../.opencode/rules/java-rules.md` по изменённым строкам: `@UtilityClass` (класс не
создавался; `OrderServiceImpl` — предсуществующий Spring `@Service`), английские тексты
исключений (новых исключений нет), логирование (не добавлялось) — не применимы.

### Наблюдение (не FAIL) — «Вопрос на проверку»
- **[OrderServiceImpl.java:38-50] Гонка check-then-act (TOCTOU).** Между `findByCode` и UPDATE
  параллельная транзакция может сменить статус, и UPDATE всё равно перезапишет статус в `issued`
  (в WHERE нет `and status = 'ready'`). `@Transactional` (READ COMMITTED) это не предотвращает.
  Осознанное решение, продиктованное спецификацией и планом (`tasks/...md:8`; план,
  «Архитектурное решение» п.1: «проверку делает сервис, как требует задание»). Нарушением не
  считается; при ужесточении требований атомарности — перенести проверку в WHERE (отдельная задача).

### Не удалось проверить — «Вопрос на проверку»
- **`gradlew.bat compileJava` = BUILD SUCCESSFUL** — запуск отклонён политикой сессии (shell
  permission denied). Статически сверены сигнатуры и импорты (п.1): `java.time.Instant` (:15) и
  `java.util.Optional` (:18) используются, `Locale` (:17), `@Transactional` (:13) —
  предсуществующие; вызовы репозитория совпадают с объявлениями. Код статически консистентен;
  зелёный билд рекомендуется подтвердить локально. (Компиляция подтверждена независимо: coder —
  `BUILD SUCCESSFUL in 7s`; тестер — зелёный `gradlew test`, 9/9.)

## Итог
**PASS.** Реализация точно соответствует контракту t2 и спецификации; чужой код не затронут;
постусловие по SQL выполнено; нарушений правил java-rules.md в изменённых строках нет.
