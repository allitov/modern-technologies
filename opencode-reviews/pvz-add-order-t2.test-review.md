# Test review: pvz-add-order / t2 — тесты addOrder

> Отчёт test-reviewer (агентская сессия ses_ea6a6cb55ffe3UcfsEEfvfRSbp); сохранён оркестратором —
> запись из песочницы ревьюера заблокирована. Вердикт: **PASS**.

**Вердикт: PASS**

Ревью по staged-диффу четырёх файлов из inputs t2; правки t1 (продакшн-код) в скоуп не входят. Прогон `gradlew.bat test` — 14/14 (подтверждён отдельным прогоном); статический подсчёт сходится: OrderServiceImplTest 4+1+2=7, интеграция 4, контроллер 2, PvzApplicationTests 1 → **14**.

## Что проверено и подтверждено

### 1. Каждый тест проверяет заявленное поведение, а не эхо моков

- `OrderServiceImplTest.java:54-58` — классификация проверена на **точных позициях** аргументов insert: `argThat(OrderServiceImplTest::isTemporaryCode), eq(contact), isNull(), eq(cell)` (телефон) и `..., isNull(), eq(contact), eq(cell)` (артикул) — ровно сигнатура `OrderRepository.insert(String,String,String,String)` (`OrderRepository.java:21-22`). Перепутанные колонки или сломанный regex (`OrderServiceImpl.java:21,63`) уронили бы verify.
- `OrderServiceImplTest.java:52` — `assertThat(result).isEqualTo("PVZ-000042")` при моке `thenReturn(42L)`: проверяется **продакшн-форматирование** `String.format("PVZ-%06d", id)` (`OrderServiceImpl.java:67`), не тавтология. `:60` — `verify(orderRepository).updateCode(42L, "PVZ-000042")` конкретными значениями.
- `OrderServiceImplTest.java:78-80` — `ArgumentCaptor` + `matches("[0-9a-f]{30}")`: тест упал бы при длине ≠30, верхнем регистре, сохранённых дефисах UUID. Мутационно чувствителен.
- `OrderServiceImplTest.java:83-98` — оба источника `JdbiException` (insert и updateCode) через `@ValueSource(booleans = {false, true})`, ассерт `.isInstanceOf(OrderCreationException.class).hasCause(cause)` — тип и идентичность причины.
- Интеграция: чтение **через DriverManager с кредами контейнера** (`OrderServiceImplIntegrationTest.java:94-105`) — невидимая извне строка доказала бы отсутствие коммита; ассерты status/phone/parcel_article/cell (`:53-56`, `:68-71`) подтверждают постусловие XOR phone/article.
- Контроллер: catch-ветка (`OrderControllerTest.java:41-45`) — `status().is3xxRedirection()` (не 500), `redirectedUrl("/orders/new")`, точные тексты flash `msg`/`msgType=err` — дословно совпадают с `OrderController.java:121-125`; happy-path (`:55-59`) — `redirectedUrl("/")`, `msgType=ok` (`OrderController.java:126-128`).

### 2. Ассерты содержательные

Ни одного теста вида «не бросило — значит ОК»: каждый кейс завершается сравнением значений (`isEqualTo`/`matches`/`isNull`), flash-атрибутами или чтением строки из БД. `assertThat(resultSet.next()).isTrue()` (`:101`) сторожит отсутствие строки; «не более одной строки» гарантировано `order_code UNIQUE` (V1__create_orders.sql:3).

### 3. Изоляция и детерминизм

- `OrderServiceImplIntegrationTest.java:38-41` — `@BeforeEach` → `truncate table orders restart identity` через реальный `Jdbi`: ожидаемые `PVZ-000001`/`PVZ-000002` (`:51`, `:66`, `:88-89`) не зависят от порядка выполнения. Класс не `@Transactional` — отката нет, очистка явная.
- Кейс «артикул» (`:61-72`) сам создаёт предыдущий заказ (id=2) — не полагается на порядок тестов.
- `@Testcontainers(disabledWithoutDocker = true)` в обоих контейнерных классах — без Docker тесты скипаются, а не краснят сборку.

### 4. Конвенции проекта

- JUnit 5 + AssertJ; `@Nested` на каждый метод продакшна (`AddOrder`/`Add`) — по `.opencode/rules/test-rules.md`.
- `@DisplayName` на русском у каждого теста, включая `contextLoads`.
- Параметризация: классификация ×4 (`@MethodSource`), источники исключения ×2 (`@ValueSource`); провайдер данных после использующего теста.
- `@MockitoBean` (`OrderControllerTest.java:27`, `org.springframework.test.context.bean.override.mockito`) — не `@MockBean`; требование Boot 4 выполнено.
- Интеграционный класс назван `*Test` — выполняется задачей `test` (`build.gradle.kts:45-47`, `useJUnitPlatform()`), как требовал план.
- Имена методов — camelCase-предложения с указанием исхода.

### 5. Покрытие веток addOrder из контракта

| Ветка контракта | Где покрыта |
|---|---|
| Классификация телефон/артикул с точными позициями insert | Unit, параметризованно ×4 (вкл. cell=null) — `OrderServiceImplTest.java:47-69` |
| Формат `PVZ-%06d` + возврат + updateCode | `:52,60`; интеграционно `:51`, `:66` |
| Temp-код 30 символов `[0-9a-f]` | `:71-81` (+ argThat в `:55,58`) |
| JdbiException от insert → OrderCreationException с cause | `:86-94` (failOnUpdate=false) |
| JdbiException от updateCode → OrderCreationException с cause | `:89-91` (failOnUpdate=true) |
| Постусловие БД: status='ready', XOR phone/article, cell, коммит извне | Интеграция `:48-57`, `:61-72`, `:76-80` |
| Уникальность кодов при повторном заказе на тот же телефон | Интеграция `:84-92` |
| Catch-ветка контроллера (не 500, redirect, flash err) | `OrderControllerTest.java:35-48` |
| Happy-path контроллера (redirect на список, flash ok) | `:52-62` |

Все ветки из t2-контракта закрыты, пропусков обязательных кейсов нет.

### 6. Модификация PvzApplicationTests

Адекватна: `contextLoads` реально требует живой Postgres → `@ServiceConnection`-контейнер `postgres:16-alpine`; `disabledWithoutDocker` даёт чистый скип без Docker. Смысл существующего теста сохранён.

## Слабые места (не блокирующие, вне приёмочных критериев t2)

1. **E2E-путь OrderCreationException не покрыт интеграционно** (юнит — на моках). Провоцируется контактом >50 букв → `parcel_article varchar(50)` → JdbiException → OrderCreationException + проверка rollback. Кандидат в follow-up.
2. **Промежуточное состояние temp-кода в БД не наблюдается** (только финальный order_code) — осознанный предел покрытия, контрактом не требуется.
3. **Trim в контроллере не зафиксирован тестом**: кейс с `" ART-123 "` + `verify(orderService).addOrder("ART-123", "A-01")` ловил бы мутацию. Валидационные ветки — вне скоупа t2.
4. Погранично-избыточные verify в контроллер-тесте (`:47,61`) — дублируют strict-stubbing; вреда нет.
5. Косметика: `new JdbiException("database failure") {}` — анонимный подкласс там, где класс инстанцируется напрямую.

## Соответствие запретам рубрики

Комментарии — 0; порядок существующих тестов сохранён; магические литералы — одноразовые/локальные; матчеры в стабах неизбежны, в верификациях — `argThat`/`eq`/`isNull` по плану; одноразовых локалов нет.

## Итог

**PASS.** Тесты t2 соответствуют план-контракту, правилам проекта и рубрике: каждый тест проверяет реальное поведение (мутационно-чувствительные ассерты формата кода, позиций аргументов, видимости коммита из внешнего соединения), все обязательные ветки addOrder покрыты, детерминизм обеспечен TRUNCATE RESTART IDENTITY, конвенции соблюдены. Пункты «Слабые места» — опциональные улучшения для следующих раундов.
