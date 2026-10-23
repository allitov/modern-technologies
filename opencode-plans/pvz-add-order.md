# Plan contract: pvz-add-order

> Disk source of truth. Re-read before each decision and after any compaction.

**Goal:** Реализовать `OrderService.addOrder(String contact, String cell): String` (сейчас заглушка
`return ""` в `OrderServiceImpl.java:53-56`): по regex определить, телефон контакт или артикул (ровно одно из
полей `phone` / `parcel_article` получает значение, второе — `NULL`); в одной Spring-транзакции вставить строку
заказа с временным уникальным кодом (30 hex-символов UUID), получить сгенерированный `id`, сформировать код
`PVZ-%06d` из `id`, обновить `order_code` и закоммитить; вернуть финальный код. SQL живёт только в репозитории;
классификация, формат кода и граница транзакции — в сервисе. Нарушение ограничений БД оборачивается в кастомное
исключение `OrderCreationException`, которое контроллер превращает в flash-сообщение об ошибке (без HTTP 500).
Постусловие в БД: заказ со статусом `ready`, уникальным кодом вида `PVZ-000042`, заполненным ровно одним из
`phone` / `parcel_article`, ячейкой `cell` (или `NULL`).

**active:** — (все задачи выполнены)

## open_questions

- [x] Q1: Механизм транзакции для атомарности INSERT→UPDATE? — answer: **Spring `@Transactional` на методе
  `OrderServiceImpl.addOrder`**; репозиторий — два чистых SQL-метода; коммит при выходе из метода.
- [x] Q2: Временное значение `order_code` (UNIQUE NOT NULL, ≤ 30 символов)? — answer: **30 hex-символов UUID**
  (`UUID.randomUUID().toString().replace("-", "")` → substring до 30; ~120 бит энтропии).
- [x] Q3: Regex телефона? — answer: **порт семантики pvz-lab5.md / si/db.cpp**: одна regex
  `^(?=(?:\D*\d){10})[\d+\-() ]+$` (все символы из набора «цифры, `+`, `-`, пробел, `(`, `)`» И цифр ≥ 10;
  артикул из одних цифр с ≥ 10 знаками классифицируется как телефон — совпадает с референсом).
- [x] Q4: Что писать в незаполненное из полей `phone` / `parcel_article`? — answer: **`NULL`** (как в Javadoc
  `Order.java:32,38`; комментарии DDL V1:15-16 противоречат — НЕ менять миграцию, расхождение принято).
- [x] Q5: Обработка исключения БД при нарушении ограничений? — answer: **обернуть в кастомное исключение
  `OrderCreationException`** (runtime), контроллер ловит его в `add()` и показывает ошибку через flash
  (`msg`/`msgType=err`) с редиректом, вместо голого 500.

## backlog

- [x] **t1** — Реализовать addOrder: исключение + SQL-методы репозитория + сервис (@Transactional) + catch в контроллере
    - **agent:** coder
    - **inputs:**
        - **Точка правки 1 — `pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java`** (57 строк):
          `@Service @RequiredArgsConstructor class OrderServiceImpl implements OrderService` (package-private),
          поле `private final OrderRepository orderRepository;` (:16), заглушка
          `addOrder` (:53-56) — заменить тело. Туда же добавить: приватный статический
          `Pattern LOOKS_LIKE_PHONE = Pattern.compile("^(?=(?:\\D*\\d){10})[\\d+\\-() ]+$")` и приватный хелпер
          `looksLikePhone(String)` → `contact != null && LOOKS_LIKE_PHONE.matcher(contact).matches()`.
          Алгоритм `addOrder`: (1) `boolean phone = looksLikePhone(contact.trim())` (контроллер уже триммит —
          см. ниже; trim повторно не обязателен); (2) временный код:
          `UUID.randomUUID().toString().replace("-", "").substring(0, 30)`; (3) `long id` из
          `orderRepository.insert(tempCode, phone ? contact : null, phone ? null : contact, cell)`; (4)
          `String code = String.format("PVZ-%06d", id)`; (5) `orderRepository.updateCode(id, code)`; (6)
          `return code;`. Метод аннотировать `@Transactional` (`org.springframework.transaction.annotation`).
          **Важно:** метод остаётся package-private — Spring 6+/Boot 4.1 (proxyTargetClass=CGLIB) советирует
          protected/package-visible методы; транзакция открывается, т.к. вызов идёт через прокси-бин из
          контроллера. Ничего не делать public (конвенция: контракт — в интерфейсе).
        - **Точка правки 2 — `pvz/src/main/java/io/allitov/pvz/service/OrderService.java:24`**: добавить Javadoc
          на русском над `addOrder` (конвенция pvz-lab5.md:44): описание, `@param contact` (непустой, длина ≥ 5,
          телефон или артикул), `@param cell` (код ячейки, может быть пустым, ≤ 30), `@return` сгенерированный
          код вида `PVZ-000042`, `@throws OrderCreationException` при ошибке БД. Предусловия — на вызывающем
          (контроллер), в сервисе НЕ дублировать валидацию длины.
        - **Точка правки 3 — `pvz/src/main/java/io/allitov/pvz/repository/OrderRepository.java`** (16 строк,
          Jdbi SqlObject, `@RegisterConstructorMapper(Order.class)`): добавить два метода (SQL — только здесь):
            ```java
            @SqlUpdate("insert into orders (order_code, phone, parcel_article, cell) "
                    + "values (:tempCode, :phone, :parcelArticle, :cell)")
            @GetGeneratedKeys("id")
            long insert(@Bind("tempCode") String tempCode, @Bind("phone") String phone,
                    @Bind("parcelArticle") String parcelArticle, @Bind("cell") String cell);

            @SqlUpdate("update orders set order_code = :code where id = :id")
            void updateCode(@Bind("id") long id, @Bind("code") String code);
            ```
          `@GetGeneratedKeys("id")` + Postgres-плагин (установлен в `JdbiConfig.java:29`) даёт `returning id`;
          `id` в INSERT не передавать (колонка `generated always as identity`, DDL
          `V1__create_orders.sql:2`). Если `@GetGeneratedKeys` не сработает — fallback:
          `@SqlQuery("insert ... returning id") long insert(...)`. Импорты из `org.jdbi.v3.sqlobject.statement`.
        - **Новый файл 4 — `pvz/src/main/java/io/allitov/pvz/service/OrderCreationException.java`**:
          `public class OrderCreationException extends RuntimeException`, конструктор
          `(String message, Throwable cause)`, Javadoc на русском.
        - **Точка правки 5 — `pvz/src/main/java/io/allitov/pvz/web/OrderController.java`**, метод `add()`
          (:95-119): обернуть `orderService.addOrder(trimmed, cell)` (:115) в
          `try { ... } catch (OrderCreationException e)`: flash-атрибуты
          `msg` = «Не удалось принять заказ: ошибка базы данных» (точную формулировку — в стиле существующих
          текстов формы, pvz-lab5.md:23) и `msgType` = `"err"`, затем `redirect` на GET-маршрут формы приёмки
          (см. обработчик формы в том же контроллере, :33-94). Не менять валидационные ветки (:102-114).
        - **Инфраструктура (проверено, правок не требует):** Jdbi-бин на
          `TransactionAwareDataSourceProxy` (`JdbiConfig.java:26`) — onDemand-вызовы репозитория внутри
          Spring-транзакции присоединяются к ней (JDBI §11.4.1: соединение с autocommit=false от
          DataSourceUtils, коммит у владельца — Spring); `JdbcTransactionManager` автоконфигурируется
          Boot 4.1 (`DataSourceTransactionManagerAutoConfiguration`, classpath по
          `spring-boot-starter-data-jdbc`, `build.gradle.kts:26`). Транзакция коммитится при выходе из
          `addOrder` (постусловие «зафиксировать изменения»); при исключении — откат.
        - **Схема БД** (`pvz/src/main/resources/db/migration/V1__create_orders.sql:1-10`): `id bigint
          generated always as identity pk`; `order_code varchar(30) unique not null`; `phone varchar(20)`;
          `parcel_article varchar(50)`; `cell varchar(30)`; `status varchar(20) not null default 'ready'`
          (в INSERT не передавать — возьмётся default); `created_at timestamptz default now()`.
        - **Стиль:** Java 25, Lombok, Javadoc на русском; SQL — только в `OrderRepository`; никаких
          SQL-строк в сервисе.
        - **Не трогать:** миграцию V1 (уже применена), зависимость `jdbi3-spring5` (мёртвый груз, вне скоупа),
          остальные 7 методов-заглушек сервиса.
    - **deps:** []
    - **acceptance:** `cd pvz && gradlew.bat compileJava test-compile` — успешен; в ответе coder перечисляет
      изменённые файлы и подтверждает: (а) `return ""` из заглушки удалён, метод возвращает
      `String.format("PVZ-%06d", id)`; (б) единственный класс с SQL — `OrderRepository`; (в) незаполненное из
      phone/parcel_article передаётся как `null`; (г) `@Transactional` стоит на `addOrder`; (д) контроллер
      ловит `OrderCreationException` и делает redirect с flash-ошибкой, а не 500. Имя теста, который покроет
      ветки, coder называет из t2 (тесты пишутся следующим шагом).
    - **description:** Выполнить правки 1-5 из inputs: создать `OrderCreationException`; добавить в
      `OrderRepository` методы `insert` (@SqlUpdate + @GetGeneratedKeys("id")) и `updateCode`; реализовать
      `OrderServiceImpl.addOrder` (regex-классификация, временный код из UUID, `@Transactional`, формат
      `PVZ-%06d`, обновление кода, возврат кода, оборачивание `JdbiException` в `OrderCreationException`);
      добавить Javadoc в интерфейс; обернуть вызов в `OrderController.add()` try/catch с flash-ошибкой.
    - **touched:**
        - `pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java`
        - `pvz/src/main/java/io/allitov/pvz/service/OrderService.java`
        - `pvz/src/main/java/io/allitov/pvz/repository/OrderRepository.java`
        - `pvz/src/main/java/io/allitov/pvz/service/OrderCreationException.java` (новый файл)
        - `pvz/src/main/java/io/allitov/pvz/web/OrderController.java`
    - **статус (оркестратор):** приёмка выполнена — `gradlew.bat compileJava compileTestJava` BUILD
      SUCCESSFUL (задачи `test-compile` в Gradle 9.8.1 нет, использован аналог `compileTestJava`);
      ревью PASS — `opencode-reviews/pvz-add-order-t1.code-review.md`. Отклонения от плана: (1) метод
      `addOrder` в имплементации `public`, а не package-private — Java запрещает сужать видимость при
      `@Override` public-метода интерфейса; транзакционность через прокси не нарушена (подтверждено
      ревью); (2) сообщение исключения на английском (правила проекта), Javadoc — на русском.
      Follow-up из ревью: F1 → t3, F2 → учтено в t2.
- [x] **t2** — Тесты addOrder: unit (сервис) + интеграция (Testcontainers, транзакция и коммит)
    - **agent:** tester
    - **inputs:**
        - **Код под тест:** `OrderServiceImpl.addOrder` (реализация из t1, контракт в open_questions и t1);
          `OrderRepository.insert/updateCode`; `OrderCreationException`; `OrderController.add()` (catch-ветка).
        - **Конвенции тестов** (pvz-lab5.md:44,94-95 и дом `todo-app`): JUnit 5 + AssertJ
          (`assertThat(...).isEqualTo(...)`), параметризованные тесты предпочтительны
          (`@ParameterizedTest` + `@ValueSource`/`@MethodSource`), имена методов camelCase-предложения.
          Mockito доступен транзитивно через spring-boot-test стартеры; для Boot 4 — `@MockitoBean` (не
          `@MockBean`). Testcontainers: `testcontainers-postgresql` + `spring-boot-testcontainers` уже в
          `build.gradle.kts:27-29`.
        - **Тест 1 (unit, сервис):** `pvz/src/test/java/io/allitov/pvz/service/impl/OrderServiceImplTest.java`
          — мок `OrderRepository` (Mockito), реальный `OrderServiceImpl`. Кейсы (параметризованно):
            - телефон («+79991234567», «8 (999) 123-45-67») → `insert` вызван с `(anyString(), eq(contact),
              isNull(), eq(cell))`, артикул («ART-123», «ABC123») → `(anyString(), isNull(), eq(contact),
              eq(cell))`;
            - мок `insert` возвращает 42 → возвращён «PVZ-000042» и `updateCode(42, "PVZ-000042")` вызван;
            - временный код из аргументов `insert` — 30 символов из `[0-9a-f]`;
            - `JdbiException` от `insert` или `updateCode` → проброшен `OrderCreationException` с cause.
        - **Тест 2 (интеграция, Spring + Testcontainers):**
          `pvz/src/test/java/io/allitov/pvz/service/impl/OrderServiceImplIT.java` (или *Test — по решению
          тестера, но так, чтобы класс запускался `gradlew.bat test`): `@SpringBootTest` +
          `@ServiceConnection` на `PostgreSQLContainer("postgres:16-alpine")` — реальный бин `OrderService`,
          реальная транзакция и коммит (unit-мок транзакционность НЕ проверяет). В `@BeforeEach`:
          `TRUNCATE orders RESTART IDENTITY` (через чистый JDBI/DataSource) — детерминированные коды.
          Кейсы (pvz-lab5.md:94): приёмка по телефону с ячейкой → код «PVZ-000001», в БД: `status='ready'`,
          `phone` заполнен, `parcel_article IS NULL`, `cell` сохранён; приёмка по артикулу без ячейки →
          «PVZ-000002», `article` заполнен, `phone IS NULL`, `cell IS NULL`; данные видны из отдельного
          соединения (commit реально выполнен); второй заказ на тот же телефон → другой код (уникальность).
        - **Тест 3 (контроллер, catch-ветка):** `@WebMvcTest(OrderController.class)` + `@MockitoBean
          OrderService`, мок бросает `OrderCreationException` на `addOrder` → не 500, redirect на форму,
          flash `msgType=err` (паттерн — pvz-lab5.md:95).
        - **Существующий тест:** `PvzApplicationTests.contextLoads` требует живой Postgres. Если без
          локальной БД он красный — тестер вправе добавить Testcontainers-источник данных (тот же контейнер,
          `@ServiceConnection`), чтобы `gradlew.bat test` был зелёным целиком; сообщить, что сделано.
        - **Прогон:** `cd pvz && gradlew.bat test` (Windows, `gradlew.bat`).
    - **deps:** [t1]
    - **acceptance:** `cd pvz && gradlew.bat test` — BUILD SUCCESSFUL; в ответе tester текстом перечисляет
      запущенные тест-классы/методы и итог прогона (пустой ответ или голое «up to date» = провал задачи).
      Интеграционные assertions подтверждают: PVZ-000001/000002, `status='ready'`, ровно одно из
      phone/parcel_article заполнено (второе NULL), cell сохранён, коммит виден извне.
    - **description:** Написать три тест-класса из inputs (unit сервиса с моком репозитория; Spring+Testcontainers
      интеграция с TRUNCATE RESTART IDENTITY и проверками PVZ-000001/000002, NULL-полей, статуса и коммита;
      @WebMvcTest catch-ветки контроллера), прогнать `gradlew.bat test`, при необходимости починить
      `PvzApplicationTests` через Testcontainers-источник; доложить список тестов и результат.
    - **touched:**
        - `pvz/src/test/java/io/allitov/pvz/service/impl/OrderServiceImplTest.java` (новый)
        - `pvz/src/test/java/io/allitov/pvz/service/impl/OrderServiceImplIntegrationTest.java` (новый;
          суффикс `*Test` — попадает в дефолтную задачу `test`)
        - `pvz/src/test/java/io/allitov/pvz/web/OrderControllerTest.java` (новый)
        - `pvz/src/test/java/io/allitov/pvz/PvzApplicationTests.java` (Testcontainers + @ServiceConnection
          + `@Testcontainers(disabledWithoutDocker=true)`)
        - `pvz/src/main/java/io/allitov/pvz/model/Order.java` (фикс из red-test-раунда: +
          `@AllArgsConstructor` — публичный all-args конструктор для JDBI ConstructorMapper)
    - **статус (оркестратор):** `gradlew.bat test` — 14/14 зелёных (7 unit + 2 контроллер + 4 интеграция +
      contextLoads); ассерты PVZ-000001/000002, `status='ready'`, XOR phone/article, cell, коммит из
      отдельного соединения — подтверждены. Ревью: PASS — `opencode-reviews/pvz-add-order-t2.test-review.md`;
      PASS — `opencode-reviews/pvz-add-order-fix-order.code-review.md`. Red-test-раунд 1/2: интеграция
      вскрыла баг — классовая `@RegisterConstructorMapper(Order.class)` (JDBI 3.52.1 жадно строит
      метаданные при любом вызове прокси) × недоступный конструктор `Order` → `IllegalAccessException`
      на любом `POST /orders` (500 сырым RuntimeException, мимо catch контроллера); fix coder'ом
      (`@AllArgsConstructor`), перепрогон зелёный. Из ревью фикса: `public class Order` был таковым
      с первого коммита — в дифф входит только `@AllArgsConstructor`. Неблокирующие находки ревьюера
      тестов (E2E-проверка OrderCreationException через переполнение parcel_article, trim-кейс
      контроллера) — кандидаты в follow-up; латентный дефект `findByCode` (несовпадение имён параметров
      конструктора и колонок) — вне скоупа плана, доложено человеку.
- [x] **t3** — [follow-up из code-review t1, находка F1] Рендеринг flash-ошибки на форме приёмки
    - **agent:** coder
    - **inputs:**
        - **Находка F1** (`opencode-reviews/pvz-add-order-t1.code-review.md`): `OrderController.add()`
          кладёт flash `msg`/`msgType=err`, но шаблон
          `pvz/src/main/resources/templates/order-add.html` их не рендерит (только
          `contactError`/`cellError`) — при ошибке БД пользователь видит пустую форму без сообщения.
        - **Правка:** добавить в `order-add.html` рендеринг `msg`/`msgType` по образцу статус-бара в
          `templates/orders.html:11-13` (Thymeleaf, те же классы/стиль; ветка `msgType=err`).
        - Контроллер и сервис НЕ менять.
    - **deps:** [t1]
    - **acceptance:** `cd pvz && gradlew.bat compileJava processResources` — успешен; code-reviewer
      подтверждает рендеринг flash `msg`/`msgType` в шаблоне и отсутствие правок логики. Покрытия
      unit-тестом нет (Thymeleaf-рендеринг) — пробел покрытия отметить человеку.
    - **description:** Добавить в `order-add.html` вывод flash-сообщения `msg`/`msgType` по образцу
      `orders.html:11-13`; логику не трогать.
    - **touched:**
        - `pvz/src/main/resources/templates/order-add.html` (+4 строки: статус-бар
          `th:if="${msg != null}"` + `th:classappend` `msg-<msgType>` + `th:text` между `<h1>` и формой)
    - **статус (оркестратор):** `gradlew.bat compileJava processResources` — BUILD SUCCESSFUL;
      ревью PASS — `opencode-reviews/pvz-add-order-t3.code-review.md` (имена flash-атрибутов сверены
      с контроллером, классы есть в style.css, ветка `count` из orders.html осознанно не скопирована).
      Пробел покрытия: Thymeleaf-рендеринг автотестом не покрыт — осознанный, отмечен человеку.

## notes

- Источники разведки: `OrderServiceImpl.java:53-56`, `OrderService.java:24`, `OrderRepository.java:10-16`,
  `JdbiConfig.java:24-32`, `JdbiRepositoryConfig.java:11-14`, `V1__create_orders.sql:1-10`,
  `OrderController.java:95-119`, `build.gradle.kts:21-43`, `Order.java:17-56`;
  план-предшественник `opencode-plans/pvz-lab5.md` (:21 looksLikePhone, :37 рецепт addOrder, :44 конвенции,
  :56 валидация в контроллере) — его архитектура `DatabaseImpl` устарела (в дереве service/repository-split),
  рецепт транзакции перенесён в сервис по решению Q1.
- Вне скоупа (сознательно): реализация остальных 7 методов сервиса, правка комментариев V1-миграции
  (противоречат Q4), удаление `jdbi3-spring5`, RowMapper-проблема `findByCode` (не участвует в addOrder).
- Риск: `@Transactional` на package-private методе требует вызова через прокси-бин — выполняется
  (контроллер держит `OrderService`, :32); не вызывать `addOrder` изнутри самого `OrderServiceImpl`.
