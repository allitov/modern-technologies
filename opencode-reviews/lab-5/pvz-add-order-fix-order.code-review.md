# Code review: фикс Order.java для t2 «pvz-add-order» (IllegalAccessException в ConstructorMapper)

> Отчёт code-reviewer (агентская сессия ses_ea6a6cb56ffeD36mu2PB9C4U99); сохранён оркестратором —
> запись из песочницы ревьюера заблокирована. Вердикт: **PASS**.

**Дата:** 2026-10-20. **Область:** незакоммиченный дифф `../../pvz/src/main/java/io/allitov/pvz/model/Order.java` (+2 строки: `import lombok.AllArgsConstructor` — :3, `@AllArgsConstructor` — :20). Контекст: план-контракт `../opencode-plans/lab-5/pvz-add-order.md`, задача t2; интеграционные тесты вскрыли, что `addOrder` падал end-to-end (`IllegalAccessException: member is private to package: Order.<init>`, обёрнутый в сырой `RuntimeException`).

**Уточнение скоупа (факты по git, расходится с описанием фикса в постановке):**
- `public class Order` существовал **с первого коммита** `c7bd39f` — в дифф фикса он не входит (в постановке указан как часть диффа — неточно).
- Закоммиченная сегодня часть правки `466781f` «pvz: поправил класс Order» (17:44 +0700) — это `createdAt: String → Instant` (+import `java.time.Instant`), не видимость класса.
- Незакоммиченный (ревьюируемый) дифф — **только `@AllArgsConstructor` + import**, т.е. публикация all-args конструктора. Вердикт относится именно к этому диффу.

**Методика:** `java-rules.md` найден в `../../.opencode/rules/java-rules.md` — правила просканированы. Механика JDBI проверена по исходникам тега **v3.52.1** (совпадает с `build.gradle.kts:39-41`): `ConstructorMapper.java`, `JdbiConstructors.java`, `ConstructorInstanceFactory.java`; семантика Lombok — по официальной документации. Прогон `gradlew.bat` ревьюером не выполнялся (запрещён песочницей); компилируемость и зелёность подтверждены прогоном тестера (14/14), здесь зафиксировано как «не проверяемо ревьюером».

## Проверка вопросов постановки

### 1. Достаточность `public class` + `@AllArgsConstructor` — ПОДТВЕРЖДЕНО

- Место отказа: `ConstructorInstanceFactory` (v3.52.1) держит `private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup()` и зовёт `LOOKUP.unreflectConstructor(constructor)` **без** `setAccessible`/`privateLookupIn`; `IllegalAccessException` оборачивается в сырой `new RuntimeException(e)` — в точности наблюдаемый симптом. Для public-конструктора public-класса (classpath = unnamed module) access-check проходит. Оба условия выполнены (`Order.java:20-21`).
- Критичное ограничение: `JdbiConstructors.findImplicitConstructorFor` (v3.52.1) требует **ровно один** объявленный конструктор (`getDeclaredConstructors().length != 1` → `IllegalArgumentException`). Пост-фикс конструктор ровно один: `@Data` не генерирует свой RequiredArgsConstructor при наличии явной конструктор-аннотации, `@Builder` не генерирует свой package-private all-args при наличии `@XArgsConstructor`. Эмпирическое подтверждение: при `@Data @Builder` отказ был именно `IllegalAccessException` (единственный конструктор выбрался, не прошёл access-check), а не `IllegalArgumentException` о количестве — `@Data` там уже молчал. Итоговая сигнатура: `Order(long, String, String, String, String, String, Instant)` — 7 полей (`Order.java:26-57`).
- Прочие package-private члены, которых касается `@RegisterConstructorMapper`: не требуются. `ConstructorMapper` читает `Parameter` (имена из `-parameters`) и в `isGenericInformationLost` — `getDeclaredFields()` (прав доступа не требует). `lombok.config` в репозитории нет → `@ConstructorProperties` не добавляется. Барьеров доступа, кроме конструктора, не осталось.

### 2. Безопасность диффа — ПОДТВЕРЖДЕНО

- Дубликат сигнатур невозможен: `@Data` при явной конструктор-аннотации не генерирует конструктор; в классе нет `final`/`@NonNull` полей (`Order.java:26-57`).
- `Order.builder()` не ломается: `@Builder` использует all-args конструктор, который теперь публичен; второй конструктор `@Builder` при `@AllArgsConstructor` не добавляет. equals/hashCode/toString/getters/setters не меняются.
- Вызыватели: grep по `pvz/**` (`new Order(`, `Order.builder()`, `Order::new`) — **0 совпадений**. Единственный потребитель конструктора — фреймворк (JDBI). Сломать некого.

### 3. Оправданность против альтернативы (перенос `@RegisterConstructorMapper` на метод `findByCode`) — ОПРАВДАНА

- Перенос аннотации меняет только момент регистрации маппера, но конструктор Order остался бы package-private, и **первый же вызов `findByCode` упал бы с тем же `IllegalAccessException`**: доступность члена от места аннотации не зависит. Альтернатива = отложить тот же крах. Выбранный вариант устраняет корневую причину для всех методов и сохраняет классовую регистрацию (`OrderRepository.java:12`).

### 4. Стиль и конвенция «не делать public» — СООТВЕТСТВУЕТ

- Конвенция из плана касается **метода сервиса** (t1: контракт — в интерфейсе), т.е. API-поверхности сервисного слоя. Публикация конструктора модели, вызываемого только фреймворком, ей не противоречит; класс Order был public изначально (c7bd39f) — как все модели проекта (`Report.java:14`, `OrderStatus.java:16`). Дифф минимален (+2 строки), Lombok-стек соблюдён. Правила `java-rules.md` изменёнными строками не нарушены.

## Находки

- **PASS [Order.java:3,20]** Дифф корректен, минимален, чинит корневую причину; блокирующих находок нет.
- **N1 [к сведению]** Скоуп фикса в постановке описан неточно: `public class Order` — не часть незакоммиченного диффа (был public с `c7bd39f`); закоммиченная `466781f` — `createdAt: String → Instant`.
- **N2 [к сведению, вне скоупа]** `findByCode` после фикса всё ещё не смапит Order полностью: параметры конструктора `code`/`article` не совпадут с колонками `order_code`/`parcel_article` (`Order.java:31,42` против `OrderRepository.java:15`) — JDBI бросит `UNMATCHED_CONSTRUCTOR_PARAMETER` при первом реальном вызове. Известная «RowMapper-проблема findByCode», планом вынесена за скоуп; `addOrder` маппинг Order не выполняет. **Кандидат в follow-up для человека.**
- **N3 [к сведению]** На будущее: добавлять `@NoArgsConstructor` к Order без `@JdbiConstructor` на all-args конструкторе нельзя — JDBI 3.52.1 потребует «exactly one constructor».
- **N4 [не проверяемо ревьюером]** Сборка/тесты в песочнице не прогонялись (gradle заблокирован); runtime-подтверждение — зелёный прогон тестера (14/14).

## VERDICT: **PASS**

Незакоммиченный фикс (`@AllArgsConstructor` + import) корректен, безопасен и оправдан против альтернативы; конвенция «контракт — в интерфейсе» не нарушена. Вопросов, требующих исправления, нет.
