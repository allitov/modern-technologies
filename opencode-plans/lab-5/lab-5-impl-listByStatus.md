# Plan contract: lab-5-impl-listByStatus

> Disk source of truth. Re-read before each decision and after any compaction.

**Goal:** В `OrderServiceImpl` реализован метод `listByStatus(String status)` (сейчас заглушка `return List.of()`):
пустая/blank строка → все заказы; непустой статус → только заказы с `status = :status`; поля выборки — как в
`listOrders` (константа `ORDER_COLUMNS`); сортировка «свежие сверху» — `order by created_at desc, id desc`. Данные не
изменяются (read-only). Условная логика (ветвление по пустоте статуса) живёт в сервисе; репозиторий — только SQL,
без логики (требование из `../../tasks/lab-5/lab-5-impl-listByStatus.md`). Контроллер `GET /report` и шаблон `report.html` не
меняются — метод уже вызывается (`OrderController.java:205-212`). Тесты НЕ пишутся (явный запрет в задании; покрытие
метода запланировано позже в t5 лабораторного плана `pvz-lab5.md`).

**active:** done (все задачи выполнены)

## open_questions

- [x] Q1: Что должен вернуть `listByStatus("")` — все заказы или только «ready»? — answer: **все заказы**. Задание:
  «Если переданный статус непустой — добавить в запрос условие». Подтверждено планом лабораторной
  `opencode-plans/pvz-lab5.md:40` («пустая строка — все, вкладка „Принято“ показывает ВСЕ заказы, так в C++
  `dialogs.cpp:202-220»`) и чек-листом `:122` («Принято» — 3 при одном issued и одном cancelled). Контроллер передаёт
  `""` для вкладки «Принято» сознательно.
- [x] Q2: Пинют ли существующие тесты поведение метода? — answer: **нет**. Ни один тест в `../../pvz/src/test` не вызывает
  `listByStatus`/`buildReport`/`GET /report`; вся сюита зелёная и с заглушкой. Писать тесты запрещено заданием.
- [x] Q3: Куда положить условный фильтр? — answer: ветвление в `OrderServiceImpl` (зеркалит структуру `listOrders`:
  `findAll()` vs `findBySearchTerm(...)`); в репозитории — один статический SQL-запрос без ветвлений.

## backlog

- [x] **Task1** — Реализовать `listByStatus`: `OrderRepository.findByStatus` + тело метода в `OrderServiceImpl`
    - **agent:** coder
    - **inputs:**
        - Спецификация: `../../tasks/lab-5/lab-5-impl-listByStatus.md`; семантика `""` = все заказы (см. open_questions Q1).
        - Файлы к изменению (только эти два + javadoc в интерфейсе):
            - `../../pvz/src/main/java/io/allitov/pvz/repository/OrderRepository.java` — добавить метод (после `findBySearchTerm`):
              ```java
              @SqlQuery(ORDER_COLUMNS + " from orders where status = :status order by created_at desc, id desc")
              List<Order> findByStatus(@Bind("status") String status);
              ```
              Конвенции подтверждены: `ORDER_COLUMNS` — существующая строковая константа (выбирает `id, code, phone,
              article, cell, status, createdAt`); сортировка `order by created_at desc, id desc` — та же, что в
              `findAll`/`findBySearchTerm`; `@Bind`/`@SqlQuery` уже импортированы.
            - `pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java:60-63` — заменить заглушку:
              ```java
              @Override
              public List<Order> listByStatus(String status) {
                  if (status == null || status.isBlank()) {
                      return orderRepository.findAll();
                  }
                  return orderRepository.findByStatus(status.trim());
              }
              ```
              Нормализация `null`/blank → все заказы повторяет паттерн `OrderFilter.of()` (`model/OrderFilter.java:23-25`).
              `@Transactional` НЕ нужен — метод read-only, как соседние `listOrders`/`countClosedOrders`.
            - `pvz/src/main/java/io/allitov/pvz/service/OrderService.java:32` — дописать javadoc к объявлению
              `listByStatus`: контракт «пустая/blank строка — все заказы; иначе фильтр по точному совпадению статуса
              ("ready"/"issued"/"cancelled"); сортировка по created_at, свежие сверху; данные не изменяются».
        - Запрещено: менять `src/test` (тесты писать не надо — прямое требование задания); трогать контроллер,
          `report.html`, заглушки `buildReport`/`issueOrder`/`cancelOrder` (другие задачи лабораторной).
        - Проверка: из каталога `../../pvz` выполнить `./gradlew test` (конвенция `.opencode/rules/build-commands.md:6`).
          ВАЖНО: `PvzApplicationTests`/`OrderServiceImplIntegrationTest` требуют Docker — без него они молча
          пропускаются (`disabledWithoutDocker = true`), это НЕ провал; отчитаться, что именно прогнано.
          ВАЖНО: не пытаться проверять через `GET /report` — страница уронится с 500 из-за не связанной с этой задачей
          заглушки `buildReport()` (шаблон `report.html:12` разыменовывает `${report.readyCount}` без guard).
        - Статусы в БД хранятся lowercase-строками (`varchar(20) default 'ready'`), значения только
          `ready`/`issued`/`cancelled`; контроллер передаёт литералы — приведение регистра не требуется.
    - **deps:** []
    - **acceptance:** (1) `./gradlew test` из `../../pvz` зелёный (unit + `@WebMvcTest` проходят; Testcontainers-классы
      проходят при Docker либо пропущены — в отчёте указано, что запускалось); (2) файлы `src/test` не изменялись и не
      создавались; (3) в `OrderRepository` есть `findByStatus`, SQL переиспользует `ORDER_COLUMNS` и заканчивается
      `where status = :status order by created_at desc, id desc`; (4) в `OrderServiceImpl` заглушка заменена: пустая/
      blank-строка → `findAll()`, иначе → `findByStatus`; (5) заглушки остальных методов и контроллер/шаблоны не тронуты.
    - **description:** Добавить в JDBI SQL-object `OrderRepository` метод `findByStatus(@Bind("status") String status)`
      с переиспользованием `ORDER_COLUMNS` и сортировкой `created_at desc, id desc`; заменить тело-заглушку
      `listByStatus` в `OrderServiceImpl` на ветвление «пустая/blank → `findAll()`, иначе → `findByStatus(status.trim())»`;
      дополнить javadoc на `OrderService.listByStatus`; прогнать существующую сюиту тестов, ничего в ней не меняя.
    - **touched:**
        - `../../pvz/src/main/java/io/allitov/pvz/repository/OrderRepository.java` — добавлен `findByStatus(@Bind("status"))`
          (после `findBySearchTerm`, ORDER_COLUMNS + `where status = :status order by created_at desc, id desc`)
        - `../../pvz/src/main/java/io/allitov/pvz/service/impl/OrderServiceImpl.java` — заглушка `listByStatus` заменена
          ветвлением (null/blank → `findAll()`, иначе → `findByStatus(status.trim())`), без `@Transactional`
        - `../../pvz/src/main/java/io/allitov/pvz/service/OrderService.java` — javadoc на `listByStatus` дополнен контрактом
