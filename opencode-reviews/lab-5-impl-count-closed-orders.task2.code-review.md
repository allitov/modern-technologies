# Ревью: lab-5-impl-count-closed-orders, Task 2 — `countClosedOrders()` в OrderServiceImpl

- **Проект:** modern-technologies (модуль pvz), ветка `pvz`, несохранённые рабочие изменения
- **Файл:** `pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java`
- **План:** `opencode-plans/lab-5-impl-count-closed-orders.md`, Task2
- **Дата:** 2026-10-23
- **Примечание:** отчёт составлен code-reviewer'ом; сохранён на диск оркестратором (среда ревьюера отклонила запись файла, текст — дословно от ревьюера).

## Diff (рабочее дерево)

```diff
@@ -56,7 +56,7 @@ class OrderServiceImpl implements OrderService {

     @Override
     public int countClosedOrders() {
-        return 0;
+        return orderRepository.countByStatuses(CLOSED_STATUSES);
     }

     @Override
```

Изменение — ровно одна строка; `@Override` и остальные строки метода — контекст (не менялись).

## Обоснование по критериям

**1. Реализация по контракту — PASS.** Метод (строки 57–60) содержит `@Override` (оставшийся от стаба, без изменений) и единственную строку делегации `return orderRepository.countByStatuses(CLOSED_STATUSES);`. Стаб `return 0;` удалён — в текущем файле не встречается. Интерфейс `OrderService.java:21` объявляет `int countClosedOrders()` — сигнатура совпадает, `@Override` валиден.

**2. Нет строковых литералов "issued"/"cancelled" — PASS.** В изменённой строке литералов нет; статусы передаются константой `CLOSED_STATUSES` (строки 25–28), построенной из enum `OrderStatus.ISSUED` / `OrderStatus.CANCELLED` (`OrderStatus.java:26,31`) через `name().toLowerCase(Locale.ROOT)`. По всему файлу литералов "issued"/"cancelled" не обнаружено.

**3. Без @Transactional / try-catch / логирования — PASS.** У `countClosedOrders()` нет `@Transactional` (в отличие от соседнего `deleteClosedOrders`, где она оправдана — write-операция). Внутри метода нет try/catch `JdbiException` и нет логирования — read-only одиночный select согласно решению Q2.

**4. Остальной код не тронут — PASS.** Diff ограничен одной строкой внутри `countClosedOrders()`; `issueOrder`, `cancelOrder`, `buildReport`, `listOrders`, `listByStatus`, `deleteClosedOrders`, `addOrder` и приватный `looksLikePhone` — без изменений. `git status` показывает изменёнными только `OrderRepository.java` (Task1, уже PASS) и `OrderServiceImpl.java`; `OrderController.java` и `templates/orders.html` не изменены (ссылки на `countClosedOrders` в контроллере — строки 64, 225 — существовавший вызывающий код).

**5. Diff ограничен методом, стиль соблюдён — PASS.** Одна строка + контекст. Форматирование идентично образцу `deleteClosedOrders()` (тот же паттерн вызова `orderRepository.<метод>(CLOSED_STATUSES)`).

**6. Типобезопасность / компиляция — PASS.** `OrderRepository.java:35` (Task1): `int countByStatuses(@BindList("statuses") Collection<String> statuses)`. `CLOSED_STATUSES` — `List<String>` (строка 25), совместим с `Collection<String>`; возвращаемый `int` совпадает с интерфейсом. Пустой список невозможен (`List.of` из 2 элементов), поэтому edge-case `@BindList` на пустой коллекции неактуален. Зелёный `gradlew classes` подтверждён кодером; статическая проверка сигнатур это подтверждает.

## Находки

Находок нет (подтверждённых, подозрительных или требующих проверки человеком — ни одной). Требуемых исправлений нет.

## Регрессия (параллельный прогон tester'ом)

`.\gradlew.bat cleanTest test` — BUILD SUCCESSFUL, **9 passed / 5 skipped / 0 failed** — совпадает с базлайном; `OrderServiceImplTest$AddOrder` — 7/7 passed. Регрессий нет.

## Вердикт

**PASS**
