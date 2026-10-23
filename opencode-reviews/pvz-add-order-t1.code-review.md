# Code review: t1 «pvz-add-order» — реализация OrderService.addOrder

> Отчёт code-reviewer (агентская сессия ses_ea6d66fd6ffe6zVdtltPf2tQqQ); сохранён оркестратором —
> запись в `opencode-reviews/` из песочницы ревьюера была заблокирована. Вердикт: **PASS**.

**Дата:** 2026-10-20. **Область:** незакоммиченные изменения (4 изменённых + 1 новый файл, +82/−3), план-контракт `opencode-plans/pvz-add-order.md`, задача t1. **Файлы:** `OrderServiceImpl.java`, `OrderService.java`, `OrderRepository.java`, `OrderCreationException.java` (новый), `OrderController.java`.

**Методика:** файла `java-rules.md` в репозитории нет — правила сканировались по план-контракту (acceptance t1 + «не трогать»-список + стиль) и стандартным правилам Java/Spring/JDBI. Прогон `gradlew.bat compileJava` не выполнен — запуск gradle запрещён разрешениями песочницы; компилируемость подтверждена статически (все импорты/типы существуют в заявленных зависимостях: jdbi3-sqlobject 3.52.1 `statement.SqlUpdate/GetGeneratedKeys`, jdbi3-core `JdbiException`, spring-tx через `spring-boot-starter-data-jdbc`). Итоговая сборка подтверждена кодером: `gradlew.bat compileJava compileTestJava` — BUILD SUCCESSFUL.

## Проверка acceptance-критериев

1. **Заглушка удалена, возврат `PVZ-%06d` — PASS.** `OrderServiceImpl.java:62-73`: `return ""` удалён; `String.format("PVZ-%06d", id)` (:67) → `updateCode` (:68) → `return code` (:69). Временный код `UUID...replace("-","").substring(0,30)` (:64) — 30 hex, ровно влезает в `order_code varchar(30)` (V1:3). Ответ на Q2.
2. **SQL только в репозитории — PASS.** SQL-строки только в `OrderRepository.java:18-19,24`; grep по `service/**` и контроллеру — чисто. Классификация/формат/граница транзакции — в сервисе.
3. **NULL для незаполненного — PASS.** `OrderServiceImpl.java:66`: `phone ? contact : null, phone ? null : contact` — ровно один не-null. Regex `^(?=(?:\D*\d){10})[\d+\-() ]+$` (:21) — точный порт референса (Q3), `.matches()` якорит всю строку; артикул из ≥10 цифр → телефон (принято планом сознательно).
4. **`@Transactional` — PASS, отклонение по видимости корректно.** Импорт `org.springframework.transaction.annotation` (:11), аннотация на `addOrder` (:61). `public` вместо package-private — единственный компилируемый вариант: `OrderService.addOrder` public в интерфейсе (`OrderService.java:39`), Java запрещает сужать видимость. Транзакционность не ломается: public-методы — канонический случай proxy-based `@Transactional` (Boot 4.1, CGLIB); контроллер держит интерфейс `OrderService` (`OrderController.java:33`) → вызов через прокси; самовызовов внутри `OrderServiceImpl` нет. Откат при `OrderCreationException` (RuntimeException — политика по умолчанию); JDBI-репозиторий присоединяется к Spring-транзакции через `TransactionAwareDataSourceProxy` (`JdbiConfig.java:26`).
5. **Контроллер — PASS (с находкой F1).** `OrderController.java:118-125`: catch `OrderCreationException` → flash `msg`/`msgType=err` → `redirect:/orders/new` (GET-маршрут существует, :74-79); без HTTP 500. Валидационные ветки (:106-117) — контекстные строки, не изменялись.
6. **Общее качество — PASS.** Javadoc на русском (интерфейс `OrderService.java:24-38`; исключение `OrderCreationException.java:3-19`; Javadoc контроллера обновлён, :88-90). Cause сохранён (`OrderServiceImpl.java:71`, `OrderCreationException.java:21`). INSERT без `id` (identity, V1:2) и без `status` (default `'ready'`, V1:7); `@GetGeneratedKeys("id")` + Postgres-плагин → `returning id`, `long` ≈ `bigint`. «Не трогать» соблюдено: `git status`/`git diff HEAD` — ровно 5 java-файлов; V1, `build.gradle.kts` (включая `jdbi3-spring5`), 7 остальных заглушек (:25-58 — только контекст), валидация контроллера, шаблоны — нетронуты.

## Находки

**F1. [Вопрос на проверку / follow-up, не блокирует t1] Flash-ошибка БД не видна пользователю.** `OrderController.java:122-124` → `order-add.html` (вне диффа): шаблон формы не рендерит `msg`/`msgType` (только `contactError`/`cellError`, :15,20; для сравнения `orders.html:11-13` рендерит статус-бар). При ошибке БД пользователь молча получает пустую форму. Имплементатор сделал ровно то, что предписывает точка правки 5 плана (шаблон не входил ни в точки правки t1, ни в «не трогать») — это пробел самого плана. **Исправление:** микрозадача t3 — добавить в `order-add.html` рендеринг `msg`/`msgType` по образцу `orders.html:11-13` (или выводить ошибку как `contactError`).

**F2. [Неблокирующее] Пустая `cell` сохранится как `''`, а не `NULL`.** `OrderServiceImpl.java:66` / `OrderController.java:101` — точное следование рецепту t1, не дефект. Но интеграционный тест t2 ожидает `cell IS NULL` при «без ячейки» — тестеру вызывать `addOrder(contact, null)` напрямую или согласовать нормализацию (`cell.isBlank() ? null : cell`). Синхронизировано с t2: тест вызывает сервис напрямую с `null`.

**F3. [К сведению] Сборка не прогонялась ревьюером** — gradle заблокирован песочницей; статическая проверка синтаксиса/импортов/сигнатур ошибок не нашла. Финальная сборка подтверждена кодером (`compileJava compileTestJava` — BUILD SUCCESSFUL); полный прогон тестов — за t2.

## VERDICT: **PASS**

Все 6 пунктов acceptance t1 выполнены; отклонение «public вместо package-private» технически обязательное и транзакционность не нарушает. Ошибок в изменённых строках не найдено. F1 — вне диффа и вне точек правки t1, передано как follow-up (t3); F2 — синхронизировано с t2.
