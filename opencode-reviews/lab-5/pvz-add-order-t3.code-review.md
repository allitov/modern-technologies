# Code review: t3 «pvz-add-order» — рендеринг flash-ошибки на форме приёмки (follow-up F1 из ревью t1)

> Отчёт code-reviewer (агентская сессия ses_ea6837f0effeMnH3JuzelDgylb); сохранён оркестратором —
> запись из песочницы ревьюера заблокирована. Вердикт: **PASS**.

**Дата:** 2026-10-20. **Файл в диффе:** `../../pvz/src/main/resources/templates/order-add.html` (единственный; +4 строки, 0 удалённых — вставка между `<h1>` :9 и `<form class="form-card">` :15). План-контракт `../opencode-plans/lab-5/pvz-add-order.md`, задача t3.

**Методика:** сканирование по план-контракту (acceptance t3: рендеринг flash `msg`/`msgType`, отсутствие правок логики) и по шести пунктам ТЗ. Дифф получен `git diff` по файлу. Сверка с `OrderController.java`, `orders.html:11-14`, `style.css:43-68`. Gradle-прогон (`processResources`) ревьюером не выполнялся (песочница); сборка подтверждена кодером — BUILD SUCCESSFUL.

## Диф (полностью)

```html
<div class="status-bar" th:if="${msg != null}" th:classappend="${msg != null ? 'msg-' + msgType : ''}">
    <span th:text="${msg}">Сообщение</span>
</div>
```

## Проверка по пунктам

1. **Корректность Thymeleaf-разметки — PASS.** `th:if` срабатывает раньше `th:classappend`/`th:text`; при `msg == null` div не рендерится вовсе — доступов к null нет. Тернарник в `th:classappend` посимвольно совпадает с `orders.html:11`. **XSS:** `th:text` экранирует HTML — безопасно (текст вдобавок статический, из контроллера). Несуществующих атрибутов модели нет; `contactError`/`cellError`/`contact`/`cell` не тронуты. Кейс «msg != null, msgType == null» недостижим: все 10 точек `addFlashAttribute("msg", ...)` в `OrderController` парные с `msgType` (:122-123, :126-127, :146-147, :151-152, :160-163, :181-182, :186-187, :189-190, :227-228, :232-233).
2. **Сверка с контроллером — PASS.** Catch-ветка `OrderCreationException` (`OrderController.java:121-125`): `addFlashAttribute("msg", "Не удалось принять заказ: ошибка базы данных")` + `addFlashAttribute("msgType", "err")` + `redirect:/orders/new` — имена flash-атрибутов совпадают точно; Spring мержит input-FlashMap в модель перед рендером. GET `/orders/new` → `showAddForm()` (:74-79) кладёт только `contact`/`cell` — ничего не затирает, `count` не требует.
3. **Сверка с `orders.html:11-13` и `style.css` — PASS.** Классы существуют: `.status-bar` (:44-50), `.msg-ok` (:52-56), `.msg-warn` (:58-62), `.status-bar.msg-err` (:64-68 — фон `--color-cancelled`, при `msgType=err` бар красный, как на списке). Поведение консистентно.
4. **Отклонение: ветка `msg == null` с `count` не скопирована — PASS, обосновано.** `count` кладёт только `list()` (`OrderController.java:63`); на `/orders/new` его нет — прямое копирование дало бы постоянную полосу «Готово • Заказов: » с пустым значением. Перенос `th:if` с внутреннего `<span>` на внешний `<div>` — минимальный корректный способ скрыть бар целиком при отсутствии flash.
5. **Минимальность диффа — PASS.** `git status --porcelain`: единственный unstaged-модифицированный файл — `order-add.html`; правки t1/t2 staged и уже покрыты ревью. Java-логика не тронута; внутри шаблона ровно 4 добавленные строки.
6. **Стиль проекта — PASS.** Отступ 4 пробела, структура «заголовок → статус-бар → контент» как в `orders.html:9-14`.

## Находки

Блокирующих дефектов нет. Неблокирующие:
- **[Пробел покрытия — передать человеку]** Рендеринг flash в `order-add.html` не покрыт автотестом (Thymeleaf-рендеринг вне скоупа `@WebMvcTest`); зафиксировано в acceptance t3 как осознанный пробел. E2E-проверка «ошибка БД → красный бар» остаётся ручной (кандидат в follow-up: `@WebMvcTest` с flash-атрибутами + assert DOM).
- **[К сведению]** Сборка ревьюером не прогонялась (песочница, только read-only git); разметка идентична работающему `orders.html:11-12`.
- **[К сведению]** Git-предупреждение «LF will be replaced by CRLF» — нормализация autocrlf на Windows; в diff ровно 4 добавленные строки.

## VERDICT: **PASS**

F1 закрыт корректно: flash `msg`/`msgType` рендерится на форме приёмки тем же механизмом и стилями, что на списке заказов; логика и прочие файлы не тронуты; отклонение от образца обосновано.
