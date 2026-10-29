# ИС охранного агентства на Java — план реализации

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Переписать настольное приложение «Информационная система охранного агентства» (WinForms/C#, `template.md`) на Java + JavaFX с СУБД PostgreSQL и разграничением прав по ролям.

**Architecture:** Слоистое приложение: `model` (record-классы) → `dao` (JDBC + `PreparedStatement`) → `service` (бизнес-правила, транзакции) → `ui` (FXML + контроллеры). Логика мониторинга (датчики, таймеры ГБР/МЧС) вынесена в чистый класс `MonitoringSession` без JavaFX и БД — он покрыт юнит-тестами. Доступ к разделам определяет одна таблица прав `Permissions`.

**Tech Stack:** Java 21, JavaFX 21.0.5 (FXML), AtlantaFX 2.0.1, PostgreSQL 16 + JDBC `org.postgresql:postgresql:42.7.4`, HikariCP 5.1.0, slf4j-simple 2.0.16, JUnit Jupiter 5.11.3, Maven 3.9 (`javafx-maven-plugin` 0.0.8, `maven-shade-plugin` 3.6.0).

**Spec:** раздел «Проектные решения» ниже + исходный код `D:\code\Diplom\template.md` (функциональность) + схема `D:\code\Diplom\2 этап курсовой.sql` (БД) + `Diplomnaya_rabota_MorozovI.docx` (тест-план, гл. 5; скриншоты форм рис. 16–17).

## Проектные решения

1. **БД — схема из `2 этап курсовой.sql`** (PostgreSQL), а не MS SQL-схема диплома. Соответствие старых таблиц новым:
   | Было (MS SQL) | Стало (PostgreSQL) |
   |---|---|
   | `Client` | `clients` (одно поле `name` вместо Ф/И/О; тип — физ./юр. лицо) |
   | `ObjectTable` | `protected_objects` с `object_type='real_estate'`, адрес одной строкой в `address` |
   | `Car` | `protected_objects` (`object_type='vehicle'`) + `vehicles` (1:1) |
   | `Contract` | `contracts` + `contract_objects` (M:N) |
   | `Triggering`, `ContractTriggering` | `alarm_events` (прямая ссылка на объект) |
   | `RRT`, `RrtStaff`, `Staff` | `response_teams`, `team_members` → `users` (сотрудники = пользователи) |
   | `GroupTriggering*` | `dispatches` (выезд ГБР по событию) |
   | `User` | `users` + `roles` |
2. **Изменение схемы:** в CHECK `equipment_types.category` добавляется `'gas_sensor'` и тип «Датчик утечки газа (ДУГ)» — он есть в старом приложении.
3. **Пароли:** PBKDF2WithHmacSHA256, 210 000 итераций, соль 16 байт, хэш 32 байта, формат хранения `pbkdf2$210000$<base64 salt>$<base64 hash>` (встроено в JDK, без зависимостей). Совместимость со старыми SHA-256-хэшами не нужна — БД новая.
4. **Регистрация:** первый зарегистрированный пользователь в пустой БД становится `administrator` и сразу активен. Все последующие — `operator` с `is_active = FALSE`; активирует и меняет роль администратор в разделе «Пользователи».
5. **Роли и доступ** (W — изменение, R — только просмотр, — — раздел скрыт):

   | Раздел (`Section`) | administrator | manager | engineer | operator | response_team | client |
   |---|---|---|---|---|---|---|
   | `CAR_MONITOR` Автомобиль (мониторинг) | W | — | — | W | — | — |
   | `NDV_MONITOR` Недвижимость (мониторинг) | W | — | — | W | — | — |
   | `ALARMS` Тревоги и выезды | W | R | — | W | R | — |
   | `CLIENTS` Клиенты | W | W | R | R | — | — |
   | `REAL_ESTATE` Объекты недвижимости | W | W | R | R | — | — |
   | `VEHICLES` Автомобили | W | W | R | R | — | — |
   | `CONTRACTS` Договоры | W | W | — | R | — | — |
   | `TEAMS` Группы быстрого реагирования | W | R | — | R | R | — |
   | `EQUIPMENT` Оборудование | W | R | W | R | — | — |
   | `INSTALL_REQUESTS` Заявки на монтаж | W | W | W | — | — | — |
   | `MY_DISPATCHES` Мои выезды | — | — | — | — | W | — |
   | `USERS` Пользователи | W | — | — | — | — | — |
   | `AUDIT` Журнал действий | R | — | — | — | — | — |

   Роль `client` в настольное приложение не входит (у неё нет ни одного раздела).
6. **Мониторинг** повторяет поведение C#-версии (тексты сообщений — ниже, в Task 10), с исправлениями: третье срабатывание или КТС вызывает ГБР автоматически; повторный вызов при ГБР в пути пишет «уже в пути»; при снятой охране датчики заблокированы; каждое срабатывание пишется в `alarm_events`, выезд — в `dispatches`, постановка/снятие охраны, глушение двигателя, вызов МЧС — в `audit_log`. Время прибытия ГБР и МЧС задаётся в `app.properties` (`dispatch.eta.seconds`, `mchs.eta.seconds`, по умолчанию 900).
7. **Картинки** машины и квартиры вырезаются из скриншотов диплома (исходников C#-проекта нет), подписи стираются, датчики накладываются кнопками JavaFX.
8. **Без** `module-info.java` (classpath-приложение), запуск через класс `Launcher` (не наследует `Application`) — так работает и fat-jar.

## Global Constraints

- Корень проекта: `D:\code\Diplom\security-agency\`; пакет `ru.vlsu.security`; groupId `ru.vlsu`, artifactId `security-agency`, версия `1.0.0`.
- Java 21 (`maven.compiler.release=21`), кодировка исходников UTF-8.
- Весь SQL — только через `PreparedStatement` с параметрами; конкатенация пользовательского ввода в SQL запрещена.
- DAO бросают `ru.vlsu.security.dao.DataAccessException extends RuntimeException` (поле `String sqlState`), оборачивая `SQLException`.
- Все тексты интерфейса и сообщений — на русском, точно как указано в задачах.
- Тесты, работающие с БД, используют отдельную базу `security_test` (`src/test/resources/db-test.properties`) и вызывают `TestDb.reset()` в `@BeforeEach`.
- Размер главного окна 1280×720, тема AtlantaFX `PrimerLight`, акцентный цвет `#37474F` (BlueGrey800), боковое меню `#263238` (BlueGrey900).
- Коммиты — после каждой задачи, сообщения на английском в стиле `feat: …` / `test: …`.

## Review Focus

1. **Нет свободной ГБР** при автоматическом вызове (3-е срабатывание/КТС) → в журнале «Нет свободных групп быстрого реагирования», сессия не «зависает» в состоянии «ГБР в пути», следующий вызов снова пробует. Тесты: Task 10 (`cancelGbr_allowsNewRequest`), Task 11 (`dispatch_noAvailableTeam_throws`).
2. **БД не запущена / неверный пароль в `db.properties`** при старте → окно с текстом «Не удалось подключиться к базе данных: …» и выход, а не стек-трейс. Тест: Task 2 (`ping_wrongPort_returnsFalse`).
3. **Удаление записи, на которую ссылаются** (клиент с договорами, объект с тревогами) → «Запись используется в других таблицах и не может быть удалена». Тесты: Task 5 (`DbErrorsTest`), Task 6 (`delete_clientWithObjects_failsWithFkState`).
4. **Поиск кириллицей в другом регистре** («м117рн» находит «М117РН», « иванов » с пробелами находит «Иванов И.И.»). Тест: Task 5 (`RowFilterTest`).
5. **Некорректный ввод в формах** (цена «abc», отрицательная цена, дата окончания договора раньше начала) → понятное сообщение, запись не сохраняется. Тесты: Task 5 (`FormParsersTest`), Task 8 (`validate_validToBeforeValidFrom`).

---

### Task 1: Окружение и каркас Maven-проекта

**Files:**
- Create: `security-agency/pom.xml`
- Create: `security-agency/src/main/java/ru/vlsu/security/Launcher.java`
- Create: `security-agency/src/main/java/ru/vlsu/security/App.java`
- Create: `D:/code/Diplom/.gitignore`
- Test: `security-agency/src/test/java/ru/vlsu/security/SmokeTest.java`

**Interfaces:**
- Produces: `Launcher.main(String[])` → `Application.launch(App.class, args)`; `App extends javafx.application.Application`.

- [ ] **Step 1: Установить инструменты.**
  - PostgreSQL 16 (установщик EDB с postgresql.org, порт 5432, пароль пользователя `postgres` запомнить; галочка pgAdmin).
  - Maven 3.9.9: скачать `apache-maven-3.9.9-bin.zip`, распаковать в `C:\tools\apache-maven-3.9.9`, добавить `C:\tools\apache-maven-3.9.9\bin` в PATH.
  - Проверка: `mvn -v` показывает `Apache Maven 3.9.9` и `Java version: 21`; `"C:\Program Files\PostgreSQL\16\bin\psql" --version` показывает `16.x`.
- [ ] **Step 2: `git init` в `D:\code\Diplom`**, `.gitignore`: `target/`, `*.iml`, `.idea/`, `security-agency/src/main/resources/db.properties`, `~$*.docx`.
- [ ] **Step 3: `pom.xml`** с зависимостями из Tech Stack (`javafx-controls`, `javafx-fxml` 21.0.5; `io.github.mkpaz:atlantafx-base:2.0.1`; `postgresql`; `HikariCP`; `slf4j-simple`; `junit-jupiter` scope test), `maven-surefire-plugin` 3.5.2, `javafx-maven-plugin` 0.0.8 с `<mainClass>ru.vlsu.security.App</mainClass>`, `maven-shade-plugin` 3.6.0 с `ManifestResourceTransformer` → `ru.vlsu.security.Launcher`.
- [ ] **Step 4: Написать `SmokeTest.javaRuntimeIs21`**: `assertTrue(Runtime.version().feature() >= 21)`.
- [ ] **Step 5: `App.start`** показывает пустое окно с заголовком «Охранное агентство», 1280×720, применяет `Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet())`.
- [ ] **Step 6: Проверить.** `cd security-agency && mvn -q test` → `BUILD SUCCESS`; `mvn javafx:run` → открывается пустое окно.
- [ ] **Step 7: Commit** `chore: maven skeleton with JavaFX`.

---

### Task 2: Схема БД, подключение, тестовая БД

**Files:**
- Create: `security-agency/sql/01_schema.sql` (копия `2 этап курсовой.sql` + изменение из решения 2)
- Create: `security-agency/sql/02_seed.sql`
- Create: `security-agency/src/main/resources/db.properties.example`, `app.properties`
- Create: `security-agency/src/main/java/ru/vlsu/security/config/Database.java`, `AppConfig.java`
- Create: `security-agency/src/main/java/ru/vlsu/security/dao/DataAccessException.java`
- Create: `security-agency/src/test/resources/db-test.properties`
- Create: `security-agency/src/test/java/ru/vlsu/security/TestDb.java`
- Test: `security-agency/src/test/java/ru/vlsu/security/config/DatabaseTest.java`

**Interfaces:**
- Produces:
  - `Database.loadProperties(String classpathResource): Properties` (ключи `db.url`, `db.user`, `db.password`)
  - `Database.create(Properties p): HikariDataSource` (maximumPoolSize 5)
  - `Database.ping(DataSource ds): boolean` — `SELECT 1`, `false` при любой `SQLException`, connectionTimeout 3 с
  - `AppConfig.load(): AppConfig` → `record AppConfig(int dispatchEtaSeconds, int mchsEtaSeconds)` из `app.properties`
  - `DataAccessException(String message, SQLException cause)`, `String sqlState()`
  - `TestDb.dataSource(): DataSource` (singleton), `TestDb.reset(): void` — выполняет `DROP SCHEMA public CASCADE; CREATE SCHEMA public;`, затем `sql/01_schema.sql`, затем `sql/02_seed.sql` (пути относительно рабочей директории Maven = `security-agency`)

- [ ] **Step 1: Создать базы:** `psql -U postgres -c "CREATE DATABASE security"` и `... "CREATE DATABASE security_test"`.
- [ ] **Step 2: `01_schema.sql`** — скопировать `2 этап курсовой.sql`; в CHECK `equipment_types.category` добавить `'gas_sensor'`; в INSERT типов добавить `('Датчик утечки газа (ДУГ)', 'gas_sensor', 'Фиксация утечки бытового газа')`.
- [ ] **Step 3: `02_seed.sql`** (без пользователей — их создаёт регистрация): 4 клиента (2 физ., 2 юр. лица); 3 объекта `real_estate` (напр. «Квартира», адрес «г. Владимир, ул. Школьная, д. 7, кв. 104»); 3 объекта `vehicle` + `vehicles` (Ford Fusion М117РН, Renault Logan Т843СТ, Lada Vesta А001АА); 3 договора `active` с `contract_objects`; 2 `response_teams` (`ГБР-1`, `ГБР-2`, статус `available`); по 2 точки `vehicle_locations` на каждый автомобиль (координаты Владимира ~56.13, 40.40). Все объекты `security_status='armed'`.
- [ ] **Step 4: Написать тесты** в `DatabaseTest`:
  - `ping_testDb_returnsTrue` — `assertTrue(Database.ping(TestDb.dataSource()))`
  - `ping_wrongPort_returnsFalse` — Properties с `db.url=jdbc:postgresql://localhost:1/security_test` → `assertFalse(Database.ping(Database.create(p)))`
  - `reset_loadsSeed` — после `TestDb.reset()`: `SELECT count(*) FROM roles` = 6, `FROM equipment_types` = 9, `FROM response_teams` = 2
  - `appConfig_defaults` — `AppConfig.load()` → `dispatchEtaSeconds()==900`, `mchsEtaSeconds()==900`
- [ ] **Step 5: Запустить** `mvn -q test -Dtest=DatabaseTest` → FAIL (классов нет).
- [ ] **Step 6: Реализовать** `Database`, `AppConfig`, `DataAccessException`, `TestDb` (скрипт целиком в один `Statement.execute` — pgjdbc выполняет несколько операторов без параметров).
- [ ] **Step 7: Запустить** `mvn -q test` → PASS.
- [ ] **Step 8: В `App.start`**: если `!Database.ping(ds)` → `Alert(ERROR, "Не удалось подключиться к базе данных: " + url)` и `Platform.exit()`. Проверить вручную, остановив службу PostgreSQL.
- [ ] **Step 9: Commit** `feat: postgres schema, seed and connection`.

---

### Task 3: Роли, права, хэширование паролей

**Files:**
- Create: `security-agency/src/main/java/ru/vlsu/security/security/Role.java`, `Section.java`, `Access.java`, `Permissions.java`, `PasswordHasher.java`
- Test: `security-agency/src/test/java/ru/vlsu/security/security/PermissionsTest.java`, `PasswordHasherTest.java`

**Interfaces:**
- Produces:
  - `enum Role { ADMINISTRATOR, MANAGER, ENGINEER, OPERATOR, RESPONSE_TEAM, CLIENT; String code(); String title(); static Role fromCode(String) }` — коды как в `roles.name`, названия: «Администратор», «Менеджер», «Инженер», «Оператор», «Сотрудник ГБР», «Клиент»
  - `enum Section` — 13 значений из таблицы решения 5, у каждого `String title()` (текст кнопки меню из таблицы)
  - `enum Access { NONE, READ, WRITE; boolean canRead(); boolean canWrite() }`
  - `Permissions.access(Role, Section): Access`; `Permissions.visibleSections(Role): List<Section>` (в порядке объявления `Section`, без `NONE`)
  - `PasswordHasher.hash(char[] password): String`; `PasswordHasher.verify(char[] password, String stored): boolean`

- [ ] **Step 1: Тесты `PermissionsTest`:**
  - `operator_canWriteMonitoring` — `access(OPERATOR, CAR_MONITOR) == WRITE`
  - `operator_readsClients` — `access(OPERATOR, CLIENTS) == READ`
  - `engineer_writesEquipment_noContracts` — `EQUIPMENT == WRITE`, `CONTRACTS == NONE`
  - `client_hasNoSections` — `visibleSections(CLIENT).isEmpty()`
  - `onlyAdminSeesUsers` — для каждой роли кроме ADMINISTRATOR `access(r, USERS) == NONE`
  - `matrixComplete` — параметризованный тест по всем 6×13 ячейкам таблицы решения 5 (таблица переносится в `@CsvSource` дословно)
- [ ] **Step 2: Тесты `PasswordHasherTest`:**
  - `hash_thenVerify_true` — `verify("123456".toCharArray(), hash("123456".toCharArray()))`
  - `verify_wrongPassword_false`
  - `hash_sameInput_differentOutputs` (соль)
  - `hash_format` — `matches("pbkdf2\\$210000\\$[A-Za-z0-9+/=]+\\$[A-Za-z0-9+/=]+")`
  - `verify_malformedStored_false` — для `""`, `"abc"`, `"pbkdf2$x$y$z"` возвращает `false`, не бросает
- [ ] **Step 3: Запустить** → FAIL.
- [ ] **Step 4: Реализовать.** Матрица в `Permissions` — `EnumMap<Role, EnumMap<Section, Access>>`, заполняется из таблицы решения 5. `verify` сравнивает через `MessageDigest.isEqual`.
- [ ] **Step 5: Запустить** → PASS.
- [ ] **Step 6: Commit** `feat: roles, permission matrix, password hashing`.

---

### Task 4: Пользователи, вход и регистрация (сервис + окна)

**Files:**
- Create: `.../model/User.java`, `.../dao/UserDao.java`, `.../service/AuthService.java`, `.../service/AuthException.java`, `.../service/RegistrationForm.java`, `.../service/AuditService.java`
- Create: `.../AppContext.java`
- Create: `.../ui/LoginController.java`, `src/main/resources/ru/vlsu/security/ui/login.fxml`, `.../ui/MainController.java`, `main.fxml`, `src/main/resources/ru/vlsu/security/ui/app.css`
- Test: `.../service/AuthServiceTest.java`, `.../service/AuditServiceTest.java`

**Interfaces:**
- Consumes: `PasswordHasher`, `Role`, `Permissions` (Task 3), `TestDb` (Task 2)
- Produces:
  - `record User(long id, String login, String fullName, Role role, boolean active, String phone, String email)`
  - `UserDao(DataSource)`: `Optional<User> findByLogin(String)`, `Optional<String> findPasswordHash(String login)`, `long insert(String login, String passwordHash, String fullName, Role role, boolean active)`, `long count()`, `List<User> findAll()`, `List<User> findByRole(Role)`, `void update(User)` (full_name, phone, email, role, is_active), `void updatePassword(long id, String hash)`
  - `record RegistrationForm(String login, char[] password, char[] passwordRepeat, String fullName)`
  - `AuthService(UserDao, AuditService)`: `User login(String login, char[] password) throws AuthException`; `RegistrationResult register(RegistrationForm) throws AuthException`; `record RegistrationResult(User user, String message)`
  - `AuditService(DataSource)`: `void log(Long userId, String action, String entityType, Long entityId, String detailsJson)`; `List<AuditEntry> findRecent(int limit)`; `record AuditEntry(long id, String userLogin, String action, String entityType, Long entityId, OffsetDateTime createdAt, String details)`
  - `record AppContext(DataSource ds, AppConfig config, AuthService auth, UserDao users, AuditService audit, …)` — поля добавляются следующими задачами
  - `MainController.init(AppContext ctx, User user)`

**Точные тексты `AuthException`:**
| Условие | Сообщение |
|---|---|
| пустой логин/пароль/ФИО (после `trim`) | `Заполните все поля!` |
| пароль короче 6 символов | `Пароль должен содержать не менее 6 символов` |
| пароли не совпадают | `Пароли отличаются!` |
| логин занят | `Пользователь с таким логином уже существует` |
| нет пользователя или неверный пароль | `Неверные данные для входа!` |
| `is_active = false` | `Учётная запись ожидает активации администратором` |
| роль `CLIENT` | `Для роли «Клиент» вход в приложение не предусмотрен` |

Сообщения `RegistrationResult.message`: первый пользователь — `Учётная запись администратора создана. Выполните вход.`; остальные — `Заявка на регистрацию отправлена администратору.`

- [ ] **Step 1: Тесты `AuthServiceTest`** (`TestDb.reset()` в `@BeforeEach`):
  - `firstRegistration_isActiveAdministrator` — `register(admin, "secret1"×2, "Иванов И.И.")` → `role()==ADMINISTRATOR`, `active()==true`
  - `secondRegistration_isInactiveOperator` → `OPERATOR`, `active()==false`, message = `Заявка на регистрацию отправлена администратору.`
  - `register_passwordMismatch_throws_andNothingInserted` — сообщение `Пароли отличаются!`, `users.count()==0`
  - `register_duplicateLogin_throws`
  - `register_shortPassword_throws`
  - `register_blankFields_throws` — логин `"   "` → `Заполните все поля!`
  - `login_ok_returnsUser`; `login_wrongPassword_throws`; `login_unknownUser_throws` (то же сообщение, что для неверного пароля)
  - `login_inactive_throws`; `login_clientRole_throws` (роль меняется через `users.update`)
  - `login_sqlInjectionAttempt_fails` — логин `' OR '1'='1` → `Неверные данные для входа!`
  - `register_writesAudit` — после регистрации в `audit.findRecent(10)` есть запись с `action="REGISTER"`, `entityType="user"`
- [ ] **Step 2: Тест `AuditServiceTest.log_thenFindRecent_newestFirst`.**
- [ ] **Step 3: Запустить** → FAIL.
- [ ] **Step 4: Реализовать** `UserDao` (role через `JOIN roles`), `AuditService` (`details` пишется как `?::jsonb`), `AuthService`. Успешный вход пишет аудит `LOGIN`.
- [ ] **Step 5: Запустить** → PASS.
- [ ] **Step 6: UI.** `login.fxml` — `StackPane` с двумя `VBox`: «Авторизация» (поля «Логин», «Пароль», кнопки «Войти», «Регистрация») и «Регистрация» (поля «Логин», «ФИО», «Пароль», «Повторите пароль», кнопки «Создать аккаунт», «Назад»). Ошибки показываются `Alert(WARNING)` с текстом исключения; после успешной регистрации — `Alert(INFORMATION)` с `message` и возврат на панель входа. `Enter` в поле пароля = «Войти».
- [ ] **Step 7: `main.fxml`** — `BorderPane`: слева `VBox` меню (фон `#263238`, кнопки на всю ширину 160 px), сверху строка «Пользователь: ФИО (Роль)» и кнопки «О программе», «Выход»; центр — `StackPane contentPane`. `MainController.init` создаёт кнопки только для `Permissions.visibleSections(user.role())`; пока раздел не реализован — в центре `Label` с его названием. «О программе» — текст из C#-версии (`AboutProgrammBTN_Click`). «Выход» → возврат на окно входа.
- [ ] **Step 8: Проверить вручную:** регистрация первого пользователя → администратор → вход → меню показывает все разделы администратора; второй пользователь не может войти до активации (активировать пока SQL-запросом).
- [ ] **Step 9: Commit** `feat: authentication, registration and role-based main menu`.

---

### Task 5: Общая инфраструктура таблиц (CRUD-экран)

**Files:**
- Create: `.../dao/CrudDao.java`, `.../dao/DbErrors.java`
- Create: `.../ui/table/ColumnSpec.java`, `TableSpec.java`, `EntityForm.java`, `RowFilter.java`, `FormParsers.java`, `TableViewController.java`, `src/main/resources/ru/vlsu/security/ui/table.fxml`
- Test: `.../dao/DbErrorsTest.java`, `.../ui/table/RowFilterTest.java`, `.../ui/table/FormParsersTest.java`

**Interfaces:**
- Produces:
  - `interface CrudDao<T> { List<T> findAll(); long insert(T item); void update(T item); void delete(long id); }`
  - `DbErrors.message(DataAccessException e): String`
  - `record ColumnSpec<T>(String title, Function<T, Object> getter, double width)`
  - `interface EntityForm<T> { Optional<T> show(Window owner, T initialOrNull); }` — `null` = создание
  - `record TableSpec<T>(String title, Section section, List<ColumnSpec<T>> columns, CrudDao<T> dao, EntityForm<T> form, ToLongFunction<T> idOf)`
  - `RowFilter.matches(T row, List<ColumnSpec<T>> columns, String query): boolean`
  - `FormParsers.price(String): BigDecimal` (null для пустой строки), `FormParsers.requireText(String value, String fieldTitle): String`; ошибки — `IllegalArgumentException` с текстом для пользователя
  - `TableViewController.create(TableSpec<T>, Access access): Parent` (статическая фабрика, загружает `table.fxml`)

**Тексты `DbErrors.message`** по `sqlState`: `23503` → `Запись используется в других таблицах и не может быть удалена`; `23505` → `Запись с такими данными уже существует`; `23514` → `Данные не прошли проверку`; `23502` → `Не заполнено обязательное поле`; иное → `Ошибка базы данных: ` + `getMessage()`.

- [ ] **Step 1: Тесты `DbErrorsTest`** — по одному на каждый `sqlState` из таблицы + неизвестный.
- [ ] **Step 2: Тесты `RowFilterTest`** (row — `record R(String a, Integer b)`):
  - `emptyQuery_matchesAll` (`""`, `"   "`, `null`)
  - `cyrillicCaseInsensitive` — строка «М117РН», запрос «м117рн» → true
  - `trimsQuery` — « Иванов » находит «Иванов И.И.»
  - `matchesAnyColumn` — запрос «42» находит строку с `b=42`
  - `nullCell_ignored` — `a=null` не бросает NPE
- [ ] **Step 3: Тесты `FormParsersTest`:** `price("1 500,50")` = `1500.50`; `price("1500.5")` = `1500.5`; `price("")` = `null`; `price("abc")` → `Цена должна быть числом`; `price("-5")` → `Цена не может быть отрицательной`; `requireText("  ", "Имя")` → `Поле «Имя» обязательно для заполнения`.
- [ ] **Step 4: Запустить** → FAIL.
- [ ] **Step 5: Реализовать.** `RowFilter` сравнивает `toLowerCase(Locale.ROOT)` подстрокой. `TableViewController`: сверху заголовок и `TextField` «Поиск…» (фильтр на лету через `FilteredList` + `RowFilter`), `TableView` с колонками из спецификации, снизу кнопки «Добавить», «Изменить», «Удалить», «Обновить». При `Access.READ` первые три скрыты (`setVisible(false); setManaged(false)`), двойной клик ничего не делает. При `WRITE` двойной клик = «Изменить». «Удалить» — подтверждение `Удалить выбранную запись?`. Любая `DataAccessException` → `Alert(ERROR, DbErrors.message(e))`; `IllegalArgumentException` из формы → `Alert(WARNING, e.getMessage())`, форма остаётся открытой.
- [ ] **Step 6: Запустить** → PASS.
- [ ] **Step 7: Commit** `feat: generic CRUD table screen`.

---

### Task 6: Клиенты, недвижимость, автомобили

**Files:**
- Create: `.../model/Client.java`, `ClientType.java`, `RealEstate.java`, `VehicleObject.java`, `ObjectStatus.java`, `SecurityStatus.java`
- Create: `.../dao/ClientDao.java`, `RealEstateDao.java`, `VehicleDao.java`
- Create: `.../ui/forms/ClientForm.java`, `RealEstateForm.java`, `VehicleForm.java`, `.../ui/Screens.java`
- Modify: `.../ui/MainController.java` (открытие разделов через `Screens`), `.../AppContext.java`
- Test: `.../dao/ClientDaoTest.java`, `RealEstateDaoTest.java`, `VehicleDaoTest.java`

**Interfaces:**
- Consumes: `CrudDao`, `TableSpec`, `EntityForm`, `TableViewController`, `FormParsers` (Task 5)
- Produces:
  - `enum ClientType { INDIVIDUAL("individual","Физ. лицо"), LEGAL_ENTITY("legal_entity","Юр. лицо") }`; аналогично `ObjectStatus` (`active` «Активен», `suspended` «Приостановлен», `closed` «Закрыт»), `SecurityStatus` (`armed` «Под охраной», `disarmed` «Снят с охраны», `alarm` «Тревога», `unknown` «Неизвестно»). У всех `code()`, `title()`, `fromCode(String)`, `toString()` = `title()`
  - `record Client(long id, ClientType type, String name, String contactPerson, String phone, String email)`
  - `record RealEstate(long id, long clientId, String clientName, String name, String address, ObjectStatus objectStatus, SecurityStatus securityStatus)`
  - `record VehicleObject(long objectId, long clientId, String clientName, String brand, String model, String regNumber, String vin, ObjectStatus objectStatus, SecurityStatus securityStatus)`
  - `ClientDao`, `RealEstateDao`, `VehicleDao` implements `CrudDao<…>`; `VehicleDao.insert` в одной транзакции создаёт `protected_objects` (`name = brand + " " + model`) и `vehicles`, `update` обновляет обе, `delete(objectId)` удаляет объект (vehicles — каскадом)
  - `Screens.open(Section, AppContext, User): Parent` — `switch` по разделам; реализованные в этой задаче: `CLIENTS`, `REAL_ESTATE`, `VEHICLES`

- [ ] **Step 1: Тесты DAO:**
  - `ClientDaoTest`: `findAll_returnsSeed` (4), `insert_thenFind`, `update_changesPhone`, `delete_clientWithObjects_failsWithFkState` (`DataAccessException.sqlState()=="23503"`), `insert_blankName_failsCheck` (`23514`)
  - `RealEstateDaoTest`: `findAll_onlyRealEstate` (3, все `object_type='real_estate'`), `insert_withoutAddress_fails` (`23514`), `findAll_includesClientName`
  - `VehicleDaoTest`: `findAll_returnsSeedVehicles` (3), `insert_createsObjectAndVehicle` (оба ряда есть, `name` = «Brand Model»), `insert_duplicateRegNumber_fails_andNoOrphanObject` (`23505`; число `protected_objects` не изменилось), `delete_removesBoth`
- [ ] **Step 2: Запустить** → FAIL. **Step 3: Реализовать DAO.** **Step 4: Запустить** → PASS.
- [ ] **Step 5: Формы** (`Dialog<T>` с `GridPane`): клиент — тип (`ComboBox<ClientType>`), «Наименование / ФИО», «Контактное лицо», «Телефон», «E-mail»; недвижимость — «Клиент» (`ComboBox<Client>`), «Название», «Адрес», «Статус объекта»; автомобиль — «Клиент», «Марка», «Модель», «Гос. номер» (приводится к верхнему регистру), «VIN», «Статус объекта». Статус охраны в формах не редактируется — только на экранах мониторинга. Колонки таблиц — все поля record, кроме id-ссылок (вместо `clientId` — `clientName`).
- [ ] **Step 6: Проверить вручную** тестовые примеры 3 и 4 диплома (добавить Renault Logan Т843СТ; удалить объект недвижимости без договора) под ролями администратор (W) и оператор (R — кнопок нет).
- [ ] **Step 7: Commit** `feat: clients, real estate and vehicles screens`.

---

### Task 7: Пользователи (администрирование) и журнал действий

**Files:**
- Create: `.../ui/forms/UserForm.java`, `.../ui/UsersScreen.java`, `.../ui/AuditScreen.java`
- Modify: `.../ui/Screens.java`
- Test: `.../service/UserAdminServiceTest.java`; Create: `.../service/UserAdminService.java`

**Interfaces:**
- Consumes: `UserDao`, `AuditService`, `PasswordHasher`
- Produces: `UserAdminService(UserDao, AuditService)`: `void update(User actor, User changed)`, `void resetPassword(User actor, long userId, char[] newPassword)`; ошибки — `IllegalArgumentException`

- [ ] **Step 1: Тесты:**
  - `update_activatesOperator` — после `update` пользователь может войти через `AuthService.login`
  - `update_ownRoleOrActive_throws` — администратор меняет свою роль или снимает свой `active` → `Нельзя изменить собственную роль или заблокировать себя`
  - (правило «нельзя менять себя» гарантирует, что в системе всегда остаётся хотя бы один активный администратор — отдельная проверка не нужна)
  - `resetPassword_short_throws` (`Пароль должен содержать не менее 6 символов`), `resetPassword_thenLoginWithNew`
  - `update_writesAudit` (`action="USER_UPDATE"`)
- [ ] **Step 2: Запустить** → FAIL. **Step 3: Реализовать.** **Step 4: Запустить** → PASS.
- [ ] **Step 5: UI.** «Пользователи» — `TableViewController` с `CrudDao<User>`-адаптером (`insert` и `delete` бросают `UnsupportedOperationException`, кнопки «Добавить» и «Удалить» скрыты) + дополнительная кнопка «Сбросить пароль» (диалог с двумя полями). Колонки: Логин, ФИО, Роль, Активен (Да/Нет), Телефон, E-mail. Форма: ФИО, телефон, e-mail, роль (`ComboBox<Role>`), «Активен» (`CheckBox`). «Журнал действий» — только чтение, `findRecent(1000)`, колонки Время (`dd.MM.yyyy HH:mm:ss`), Пользователь, Действие, Сущность, ID, Подробности.
- [ ] **Step 6: Commit** `feat: user administration and audit log`.

---

### Task 8: Договоры

**Files:**
- Create: `.../model/Contract.java`, `ContractStatus.java`, `.../dao/ContractDao.java`, `.../ui/forms/ContractForm.java`, `.../service/ContractValidator.java`
- Modify: `.../ui/Screens.java`
- Test: `.../dao/ContractDaoTest.java`, `.../service/ContractValidatorTest.java`

**Interfaces:**
- Produces:
  - `enum ContractStatus` (`draft` «Черновик», `active` «Действует», `expired` «Истёк», `terminated` «Расторгнут»)
  - `record Contract(long id, long clientId, String clientName, String number, LocalDate signedOn, LocalDate validFrom, LocalDate validTo, ContractStatus status, BigDecimal price, List<Long> objectIds)`
  - `ContractDao implements CrudDao<Contract>`; `insert`/`update` в транзакции переписывают `contract_objects`
  - `ContractValidator.validate(Contract, Set<Long> clientObjectIds): void` — `IllegalArgumentException`

- [ ] **Step 1: Тесты:**
  - `ContractDaoTest`: `insert_withTwoObjects_thenFindAll_hasObjectIds`, `update_replacesObjects`, `delete_cascadesContractObjects`, `duplicateNumber_fails` (`23505`)
  - `ContractValidatorTest`: `validate_validToBeforeValidFrom` → `Дата окончания не может быть раньше даты начала`; `validate_noObjects` → `Выберите хотя бы один объект`; `validate_foreignObject` → `Объект не принадлежит выбранному клиенту`; `validate_blankNumber` → `Поле «Номер договора» обязательно для заполнения`; `validate_ok`
- [ ] **Step 2: Запустить** → FAIL. **Step 3: Реализовать.** **Step 4: Запустить** → PASS.
- [ ] **Step 5: Форма:** клиент (`ComboBox<Client>`), номер, три `DatePicker`, статус, цена (`FormParsers.price`), объекты — `ListView<CheckBox>` из объектов выбранного клиента (недвижимость + автомобили, подпись «[id] название»), перезаполняется при смене клиента. Перед закрытием вызывает `ContractValidator`. Колонки: Номер, Клиент, Подписан, С, По, Статус, Цена, Объектов (кол-во).
- [ ] **Step 6: Commit** `feat: contracts screen`.

---

### Task 9: ГБР, оборудование, заявки на монтаж

**Files:**
- Create: `.../model/ResponseTeam.java`, `TeamStatus.java`, `Equipment.java`, `EquipmentType.java`, `EquipmentStatus.java`, `InstallationRequest.java`, `RequestStatus.java`
- Create: `.../dao/ResponseTeamDao.java`, `EquipmentDao.java`, `EquipmentTypeDao.java`, `InstallationRequestDao.java`
- Create: `.../ui/forms/ResponseTeamForm.java`, `EquipmentForm.java`, `InstallationRequestForm.java`
- Modify: `.../ui/Screens.java`
- Test: `.../dao/ResponseTeamDaoTest.java`, `EquipmentDaoTest.java`, `InstallationRequestDaoTest.java`

**Interfaces:**
- Produces:
  - `enum TeamStatus` (`available` «Свободна», `dispatched` «На выезде», `unavailable` «Недоступна»)
  - `record ResponseTeam(long id, String name, String phone, TeamStatus status, List<Long> memberUserIds, String membersText)` — `membersText` = ФИО через запятую
  - `record EquipmentType(long id, String name, String category)`
  - `enum EquipmentStatus` (`planned` «Запланировано», `installed` «Установлено», `fault` «Неисправно», `removed` «Демонтировано»)
  - `record Equipment(long id, long objectId, String objectName, long typeId, String typeName, String serialNumber, LocalDate installationDate, EquipmentStatus status)`
  - `enum RequestStatus` (`new` «Новая», `assigned` «Назначена», `in_progress` «В работе», `completed` «Выполнена», `cancelled` «Отменена»)
  - `record InstallationRequest(long id, long objectId, String objectName, long createdBy, String createdByName, Long assignedTo, String assignedToName, OffsetDateTime createdAt, OffsetDateTime scheduledAt, OffsetDateTime completedAt, RequestStatus status, String description)`
  - DAO implements `CrudDao<…>`; `EquipmentTypeDao.findAll(): List<EquipmentType>`; `InstallationRequestDao.insert` берёт `createdBy` из record (форма подставляет текущего пользователя); при переводе в `COMPLETED` DAO ставит `completed_at = now()`, если пусто

- [ ] **Step 1: Тесты:**
  - `ResponseTeamDaoTest`: `findAll_seed2`, `insert_withMembers_thenMembersText`, `update_replacesMembers`
  - `EquipmentDaoTest`: `insert_thenFindAll_hasTypeAndObjectNames`, `duplicateSerial_fails` (`23505`)
  - `InstallationRequestDaoTest`: `insert_setsCreatedAt`, `complete_setsCompletedAt`, `assignedTo_nullable`
- [ ] **Step 2: Запустить** → FAIL. **Step 3: Реализовать.** **Step 4: Запустить** → PASS.
- [ ] **Step 5: Формы.** Члены ГБР выбираются из `UserDao.findByRole(RESPONSE_TEAM)`; исполнитель заявки — из `findByRole(ENGINEER)`; объект — общий список объектов (недвижимость + автомобили).
- [ ] **Step 6: Проверить вручную** тестовый пример 5 диплома в новом виде: поиск сотрудника ГБР по ФИО/телефону в разделе «ГБР».
- [ ] **Step 7: Commit** `feat: response teams, equipment and installation requests`.

---

### Task 10: Ядро мониторинга `MonitoringSession`

**Files:**
- Create: `.../monitoring/ObjectKind.java`, `SensorType.java`, `MonitoringListener.java`, `MonitoringSession.java`, `JournalLine.java`
- Test: `.../monitoring/MonitoringSessionTest.java`, `SensorTypeTest.java`

**Interfaces:**
- Produces:
  - `enum ObjectKind { CAR, NDV }`
  - `enum SensorType` — поля `ObjectKind kind`, `String code` (пишется в `alarm_events.event_type`), `String label` (подпись на схеме), `String template` (с `%d` = номер срабатывания), `String priority` (`medium|high|critical`), `boolean immediateDispatch`:

    | Константа | kind | code | label | template | priority | immediate |
    |---|---|---|---|---|---|---|
    | `HOOD` | CAR | `car_hood` | Капот | `Подкапотное пространство: Срабатывание датчика взлома №%d.` | high | – |
    | `FRONT_DOOR` | CAR | `car_front_door` | Передняя дверь | `Передние двери: Срабатывание датчика удара №%d.` | high | – |
    | `REAR_DOOR` | CAR | `car_rear_door` | Задняя дверь | `Задние двери: Срабатывание датчика удара №%d.` | high | – |
    | `TRUNK` | CAR | `car_trunk` | Багажник | `Багажник: Срабатывание датчика удара №%d.` | high | – |
    | `BATTERY` | CAR | `car_battery` | АКБ | `АКБ: Аккумулятор разряжен/отключен.` | medium | – |
    | `IGNITION` | CAR | `car_ignition` | Зажигание | `Зажигание: Двигатель заведен на охране №%d.` | critical | – |
    | `MOTION` | NDV | `motion` | ИК | `ИК: Срабатывание датчика движения №%d.` | high | – |
    | `DOOR_OPEN` | NDV | `door_open` | СМК | `СМК: Срабатывание датчика открытия двери №%d.` | high | – |
    | `GLASS_BREAK` | NDV | `glass_break` | ДРС | `ДРС: Срабатывание датчика разбития стекла №%d.` | high | – |
    | `FIRE` | NDV | `fire` | ДИП | `ДИП: Срабатывание комбинированного датчика пожарной сигнализации №%d.` | critical | – |
    | `PANIC` | NDV | `panic_button` | КТС | `КТС: Срабатывание тревожной кнопки №%d.` | critical | ✔ |
    | `WATER_LEAK` | NDV | `water_leak` | ДУВ | `ДУВ: Срабатывание датчика протечки воды №%d.` | medium | – |
    | `GAS_LEAK` | NDV | `gas_leak` | ДУГ | `ДУГ: Срабатывание датчика утечки газа №%d.` | critical | – |

    `static List<SensorType> forKind(ObjectKind)`.
  - `interface MonitoringListener { void onJournal(String message); void onStatus(String status); void onAlarm(SensorType sensor, String message); boolean onDispatchRequested(); void onGbrTimer(int secondsLeft); }` — `onDispatchRequested` возвращает `false`, если выезд создать не удалось (нет свободной группы)
  - `MonitoringSession(ObjectKind kind, String objectLabel, String operatorName, String ownerName, boolean armed, int gbrEtaSeconds, int mchsEtaSeconds, MonitoringListener listener)`
  - методы: `boolean trigger(SensorType)`, `boolean requestGbr()`, `boolean callMchs()`, `void setArmed(boolean)`, `void setIgnitionBlocked(boolean)`, `void tick()`; геттеры `int sensorCount()`, `boolean armed()`, `boolean ignitionBlocked()`, `int gbrSecondsLeft()` (−1 — не вызвана), `int mchsSecondsLeft()` (−1)
  - `JournalLine.format(LocalDateTime time, String message): String` → `dd.MM.yyyy HH:mm:ss : message`

**Правила (суффикс** `S` = ` Автомобиль [label].` для CAR, ` Объект [label].` для NDV):
- `trigger`: если `!armed` или `sensor.kind != kind` → `false`, ничего не происходит. Иначе `sensorCount++`; сообщение = `format(template, sensorCount) + S`; для `IGNITION` при `ignitionBlocked` — `Зажигание: Попытка включения зажигания №%d.` и статус не меняется, иначе статус `Машина заведена`. `onJournal(msg)`, `onAlarm(sensor, msg)`. Затем если `immediateDispatch` → `requestGbr()`; иначе если `sensorCount == 3` → `onJournal("Множественное срабатывание датчиков: [3]." + S)` и `requestGbr()`. Возврат `true`.
- `requestGbr`: если `gbrSecondsLeft > 0` → `onJournal("Группа быстрого реагирования уже в пути. Время до прибытия [" + ceil(sec/60) + " минут]")`, `false`. Иначе если `!listener.onDispatchRequested()` → `onJournal("Нет свободных групп быстрого реагирования")`, `false`. Иначе `gbrSecondsLeft = gbrEta`, `onJournal("ГБР: Группа Быстрого Реагирования отправлена [" + operatorName + "]." + S)`, `onGbrTimer(gbrEta)`, `true`.
- `callMchs` (только NDV, иначе `false`): если в пути → `МЧС уже в пути. Время до прибытия [N минут]`; иначе `mchsSecondsLeft = mchsEta`, `МЧС: МЧС вызвана на охраняемый объект [operator].` + S.
- `setArmed(true)`: статус `Машина поставлена на охрану` / `Объект поставлен на охрану`; журнал `Охрана: Машина поставлена на охрану сотрудником X по обращению владельца договора [owner].` + S (для NDV — `Объект поставлен на охрану`). `setArmed(false)`: `Машина снята с охраны` / `Объект снят с охраны`, журнал `Охрана: Охрана снята сотрудником X по обращению владельца договора [owner].` + S; `sensorCount = 0`. Повторная установка того же значения — без действий.
- `setIgnitionBlocked(true)` (только CAR): статус `Двигатель заглушен`, журнал `Глушение связи: Двигатель заглушен сотрудником X по обращению владельца договора [owner].` + S; `false`: статус `Машина на стоянке`, журнал `Глушение связи: Доступ к запуску двигателя разрешен сотрудником X по обращению владельца договора [owner].` + S.
- `tick`: если `gbrSecondsLeft > 0` → уменьшить, `onGbrTimer(новое значение)` (при прибытии вызывается `onGbrTimer(0)` — это сигнал контроллеру); при достижении 0 → `ГБР: Группа Быстрого Реагирования прибыла [operator].` + S, статус `ГБР прибыла на место происшествия`, `gbrSecondsLeft = -1`, `sensorCount = 0`. Аналогично МЧС: `МЧС: МЧС прибыла на охраняемый объект [operator].` + S, статус `МЧС прибыл на охраняемый объект`.

- [ ] **Step 1: Тесты** (`RecordingListener` в тесте пишет все вызовы в списки; `onDispatchRequested` возвращает настраиваемое значение):
  - `SensorTypeTest.forKind_counts` — CAR 6, NDV 7; `codes_unique`
  - `trigger_disarmed_ignored`; `trigger_wrongKind_ignored`
  - `trigger_firstSensor_journalAndAlarm` — CAR, label `[4] Ford Fusion (М117РН)`, `HOOD` → журнал ровно `Подкапотное пространство: Срабатывание датчика взлома №1. Автомобиль [[4] Ford Fusion (М117РН)].`
  - `thirdSensor_autoDispatch` — 3 срабатывания → в журнале строка `Множественное срабатывание датчиков: [3]. …` и `ГБР: … отправлена …`, `gbrSecondsLeft()==900`
  - `fourthSensor_whileEnRoute_noSecondDispatch` — `onDispatchRequested` вызван 1 раз
  - `panic_dispatchesImmediately` — NDV, один `PANIC` → `gbrSecondsLeft()==900`
  - `requestGbr_twice_alreadyEnRoute` — вторая строка `Группа быстрого реагирования уже в пути. Время до прибытия [15 минут]`
  - `cancelGbr_allowsNewRequest` — `onDispatchRequested` возвращает `false` → журнал `Нет свободных групп быстрого реагирования`, `gbrSecondsLeft()==-1`; затем возвращает `true` → вызов проходит
  - `tick_900_gbrArrives` — после 900 `tick()` статус `ГБР прибыла на место происшествия`, `gbrSecondsLeft()==-1`, `sensorCount()==0`
  - `ignition_blocked_attemptMessage` — `Зажигание: Попытка включения зажигания №1. …`
  - `mchs_car_returnsFalse`; `mchs_ndv_arrivesAfterEta` (eta 5, 5 тиков)
  - `disarm_resetsCounter_andBlocksSensors`
  - `setArmed_sameValue_noJournal`
  - `JournalLine.format(2023-04-11T13:58:02, "x")` = `11.04.2023 13:58:02 : x`
- [ ] **Step 2: Запустить** → FAIL. **Step 3: Реализовать.** **Step 4: Запустить** → PASS.
- [ ] **Step 5: Commit** `feat: monitoring session core`.

---

### Task 11: Сервисы тревог и выездов (БД)

**Files:**
- Create: `.../model/MonitoredObject.java`, `DispatchResult.java`, `DispatchView.java`, `AlarmEventView.java`
- Create: `.../service/AlarmService.java`, `DispatchService.java`, `NoTeamAvailableException.java`
- Modify: `.../AppContext.java`
- Test: `.../service/AlarmServiceTest.java`, `DispatchServiceTest.java`

**Interfaces:**
- Consumes: `SensorType`, `ObjectKind` (Task 10), `AuditService` (Task 4), `SecurityStatus` (Task 6)
- Produces:
  - `record MonitoredObject(long objectId, ObjectKind kind, String label, String ownerName, SecurityStatus securityStatus)`; `label` для CAR — `[objectId] Brand Model (REG)`, для NDV — `[objectId] address`; `toString()` = `label`
  - `AlarmService(DataSource, AuditService)`:
    - `List<MonitoredObject> listActive(ObjectKind)` — только `object_status='active'`, по `object_id`
    - `long recordAlarm(long objectId, SensorType sensor, long operatorId, String message)` — `alarm_events` (`event_type=sensor.code`, `priority=sensor.priority`, `status='new'`, `description=message`; для CAR `latitude/longitude` — из последней `vehicle_locations`), объект → `security_status='alarm'`; возвращает `event_id`
    - `void setSecurityStatus(long objectId, SecurityStatus status, long userId)` + аудит `ARM`/`DISARM`
    - `void logIgnition(long objectId, boolean blocked, long userId)` — аудит `IGNITION_BLOCK`/`IGNITION_UNBLOCK`
    - `void logMchs(long objectId, long userId)` — аудит `MCHS_CALL`
    - `void markFalseAlarm(long eventId, long userId)` — событие `false_alarm`, объект `armed`, аудит
    - `List<AlarmEventView> findRecent(int limit)` — `record AlarmEventView(long eventId, String objectLabel, String eventType, String priority, String status, OffsetDateTime receivedAt, String operatorName, Long dispatchId, String teamName, OffsetDateTime arrivedAt, OffsetDateTime completedAt, String result)`
  - `enum DispatchResult` (`confirmed` «Проникновение подтверждено», `false_alarm` «Ложная тревога», `no_access` «Нет доступа», `other` «Иное»)
  - `DispatchService(DataSource, AuditService)`:
    - `long dispatch(long objectId, Long eventId, long operatorId) throws NoTeamAvailableException` — одна транзакция: если `eventId == null` → создать событие `manual_dispatch`/`high`; взять первую `response_teams` со статусом `available` (`ORDER BY team_id LIMIT 1 FOR UPDATE SKIP LOCKED`); `dispatches` insert; команда → `dispatched`; событие → `dispatching`
    - `void markArrived(long dispatchId)` — `arrived_at = now()` только если пусто
    - `void complete(long dispatchId, DispatchResult result, String notes, long userId)` — `completed_at`, `arrived_at` (если пусто), `result`; команда → `available`; событие → `confirmed` / `false_alarm` / `closed` (для `no_access`, `other`); объект → `armed`
    - `List<DispatchView> findActiveForMember(long userId)` — незавершённые выезды команд, где пользователь в `team_members`; `record DispatchView(long dispatchId, long eventId, String objectLabel, String eventType, String teamName, OffsetDateTime dispatchedAt, OffsetDateTime arrivedAt)`
    - `Optional<Long> activeDispatchForObject(long objectId)`

- [ ] **Step 1: Тесты `AlarmServiceTest`:** `listActive_car_labelsFormat` (метка содержит `Ford Fusion (М117РН)`), `recordAlarm_insertsEvent_andSetsAlarmStatus`, `recordAlarm_vehicle_hasCoordinates`, `recordAlarm_realEstate_noCoordinates`, `setSecurityStatus_writesAudit`, `markFalseAlarm_rearmsObject`.
- [ ] **Step 2: Тесты `DispatchServiceTest`:** `dispatch_takesFirstAvailableTeam` (команда стала `dispatched`), `dispatch_withoutEvent_createsManualEvent`, `dispatch_noAvailableTeam_throws` (обе команды заняты), `markArrived_idempotent` (второй вызов не меняет время), `complete_freesTeam_andRearmsObject`, `findActiveForMember_onlyOwnTeams`.
- [ ] **Step 3: Запустить** → FAIL. **Step 4: Реализовать.** **Step 5: Запустить** → PASS.
- [ ] **Step 6: Commit** `feat: alarm and dispatch services`.

---

### Task 12: Экраны мониторинга «Автомобиль» и «Недвижимость»

**Files:**
- Create: `security-agency/tools/make_scheme_images.py`
- Create: `src/main/resources/ru/vlsu/security/ui/images/car.png`, `flat.png` (результат скрипта)
- Create: `.../ui/monitor/SensorPoint.java`, `MonitorController.java`, `src/main/resources/ru/vlsu/security/ui/monitor.fxml`
- Modify: `.../ui/Screens.java`, `.../ui/MainController.java`

**Interfaces:**
- Consumes: `MonitoringSession`, `MonitoringListener`, `SensorType` (Task 10); `AlarmService`, `DispatchService`, `MonitoredObject` (Task 11); `AppConfig`
- Produces: `MonitorController.create(ObjectKind, AppContext, User, Access): Parent`; `MonitorController.stop()` (останавливает `Timeline`; `MainController` вызывает для всех экранов мониторинга при выходе)
  - `record SensorPoint(SensorType sensor, double x, double y)`; `SensorPoint.forKind(ObjectKind): List<SensorPoint>`

**Картинки и координаты датчиков.** Источник — `Diplomnaya_rabota_MorozovI.docx` → `word/media/image22.png` (авто, 1266×665) и `image23.png` (квартира, 1266×660). Обе обрезаются прямоугольником `(125, 80, 717, 435)` → 592×355. Координаты (относительно обрезки) — центр бывшей радиокнопки:

| CAR | x | y | NDV | x | y |
|---|---|---|---|---|---|
| HOOD | 58 | 85 | FIRE (ДИП) | 98 | 46 |
| FRONT_DOOR | 332 | 99 | GAS_LEAK (ДУГ) | 406 | 61 |
| REAR_DOOR | 438 | 123 | MOTION (ИК) | 18 | 144 |
| TRUNK | 58 | 259 | DOOR_OPEN (СМК) | 303 | 178 |
| IGNITION | 318 | 273 | GLASS_BREAK (ДРС) | 506 | 160 |
| BATTERY | 254 | 302 | WATER_LEAK (ДУВ) | 506 | 207 |
| | | | PANIC (КТС) | 177 | 259 |

Скрипт закрашивает белым прямоугольник `(x-12, y-12, x+20+9*len(label), y+13)` вокруг каждой точки (стирает старый кружок и подпись).

- [ ] **Step 1: Скрипт** `tools/make_scheme_images.py <docx> <out_dir>`: распаковывает два PNG из docx (`zipfile`), обрезает, стирает подписи, сохраняет `car.png`/`flat.png`. Запуск: `python -I tools/make_scheme_images.py "../Diplomnaya_rabota_MorozovI.docx" src/main/resources/ru/vlsu/security/ui/images`. Проверка — открыть PNG и убедиться, что подписей и кружков нет.
- [ ] **Step 2: `monitor.fxml`** по скриншотам диплома (рис. 16–17): слева `Pane` 592×355 с `ImageView` и `ToggleButton` датчиков (стиль: маленькая кнопка с текстом `label`, при срабатывании краснеет на 2 с), под ним `ToggleSwitch` «Глушение двигателя» (только CAR) и «Охрана включена», кнопки «Отправить группу быстрого реагирования», «Вызвать МЧС» (только NDV); справа сверху — `ComboBox<MonitoredObject>` «Объект» и подписи «Владелец», «Статус: …», «Время до ГБР: N мин», «Время до МЧС: N мин» (только NDV); справа — `ListView<String>` журнала с автопрокруткой к последней строке.
- [ ] **Step 3: `MonitorController`.** При выборе объекта создаётся новая `MonitoringSession` (`armed = status == ARMED`), журнал очищается. Реализация `MonitoringListener`: `onJournal` → `JournalLine.format(now, msg)` в список; `onAlarm` → `alarmService.recordAlarm(...)`, запоминает `lastEventId`; `onDispatchRequested` → `dispatchService.dispatch(objectId, lastEventId, user.id())`, `false` при `NoTeamAvailableException`; `onStatus`/`onGbrTimer` → подписи (минуты = `ceil(sec/60)`). Переключатель охраны → `session.setArmed` + `alarmService.setSecurityStatus`; глушение → `session.setIgnitionBlocked` + `logIgnition`; МЧС → `session.callMchs()` + `logMchs` при `true`. При `onGbrTimer(0)` вызывается `dispatchService.markArrived(id)` для `dispatchService.activeDispatchForObject(objectId)`. Датчики недоступны, если объект не выбран или охрана снята. Все действия с БД оборачиваются в обработчик `DataAccessException` → `Alert(ERROR, DbErrors.message(e))`. `Timeline` раз в секунду вызывает `session.tick()`.
- [ ] **Step 4: Проверить вручную** (в `app.properties` временно `dispatch.eta.seconds=20`, `mchs.eta.seconds=20`), под оператором:
  1. CAR: выбрать Ford Fusion → 3 датчика → в журнале «Множественное срабатывание…», «ГБР … отправлена», таймер идёт, через 20 с — «прибыла»; в «Тревоги и выезды» есть 3 события и выезд.
  2. Повторно нажать «Отправить ГБР» во время отсчёта → «уже в пути».
  3. Глушение → «Зажигание» → «Попытка включения зажигания».
  4. Снять охрану → датчики недоступны; в «Журнале действий» — `DISARM`.
  5. NDV: КТС → ГБР сразу; «Вызвать МЧС» → отсчёт МЧС.
  6. Занять обе ГБР (две тревоги на разных объектах) → третья: «Нет свободных групп быстрого реагирования».
  Вернуть 900 в `app.properties`.
- [ ] **Step 5: Commit** `feat: car and real estate monitoring screens`.

---

### Task 13: «Тревоги и выезды» и «Мои выезды»

**Files:**
- Create: `.../ui/AlarmsScreen.java`, `.../ui/MyDispatchesScreen.java`, `.../ui/forms/CompleteDispatchDialog.java`
- Modify: `.../ui/Screens.java`

**Interfaces:**
- Consumes: `AlarmService.findRecent`, `markFalseAlarm`; `DispatchService.complete`, `markArrived`, `findActiveForMember` (Task 11); `RowFilter` (Task 5)
- Produces: `CompleteDispatchDialog.show(Window): Optional<CompleteDispatchDialog.Result>`; `record Result(DispatchResult result, String notes)`

- [ ] **Step 1: «Тревоги и выезды»** — таблица `AlarmEventView` (`findRecent(500)`), колонки: Время, Объект, Событие (`SensorType.label` по `code`, для `manual_dispatch` — «Ручной вызов»), Приоритет, Статус, Оператор, ГБР, Прибытие, Завершение, Результат; поиск через `RowFilter`; автообновление каждые 5 с. При `WRITE`: кнопки «Завершить выезд» (только для строки с незавершённым выездом → `CompleteDispatchDialog` → `complete`) и «Ложная тревога» (только для события без выезда в статусе `new` → `markFalseAlarm`).
- [ ] **Step 2: «Мои выезды»** (роль `response_team`) — таблица `DispatchView` из `findActiveForMember(user.id())`, кнопки «Прибыли на место» (`markArrived`) и «Завершить выезд» (`CompleteDispatchDialog`).
- [ ] **Step 3: Проверить вручную:** создать пользователя-сотрудника ГБР, активировать с ролью «Сотрудник ГБР», включить в `ГБР-1`; оператор вызывает ГБР; сотрудник видит выезд, отмечает прибытие и завершает с результатом «Ложная тревога» → у оператора событие `false_alarm`, объект снова «Под охраной», команда «Свободна».
- [ ] **Step 4: Commit** `feat: alarms overview and response team dispatches`.

---

### Task 14: Сборка, запуск и приёмочная проверка

**Files:**
- Modify: `security-agency/pom.xml` (shade уже есть — проверить `Launcher`)
- Create: `security-agency/README.md`, `security-agency/build-app.ps1`

- [ ] **Step 1: `README.md`** — требования (JDK 21, PostgreSQL 16), создание БД и запуск `psql -U postgres -d security -f sql/01_schema.sql` и `02_seed.sql`, копирование `db.properties.example` → `db.properties`, команды `mvn test`, `mvn javafx:run`, `.\build-app.ps1`; первый зарегистрированный пользователь — администратор; таблица ролей из решения 5.
- [ ] **Step 2: `build-app.ps1`** — `mvn -q -DskipTests package`, затем `jpackage --type app-image --name SecurityAgency --input target --main-jar security-agency-1.0.0.jar --main-class ru.vlsu.security.Launcher --dest target/dist`. Проверка: `target\dist\SecurityAgency\SecurityAgency.exe` запускается на машине без установленной JDK (или с переименованной `JAVA_HOME`).
- [ ] **Step 3: Полный прогон** `mvn test` → `BUILD SUCCESS`, 0 failures.
- [ ] **Step 4: Приёмка по тест-плану диплома (гл. 5)** на чистой БД: пример 1 (вход), 2 (регистрация → активация администратором → вход), 3 (добавление автомобиля Renault Logan Т843СТ), 4 (удаление объекта недвижимости), 5 (поиск сотрудника ГБР), 6 (поиск клиента по телефону), плюс сценарии из Task 12 Step 4 и Task 13 Step 3. Для каждого — снимок экрана в `docs/screenshots/` (пригодятся для обновления текста диплома).
- [ ] **Step 5: Commit** `docs: readme, packaging script, acceptance screenshots`.

---

## После плана (не код)

Обновить текст диплома: §2.2 (требования: Windows 10+, PostgreSQL), §3.1–3.2 (новая ER-диаграмма и словарь данных по `01_schema.sql`, таблица ролей), §4.1 (IntelliJ IDEA, JDK 21, JavaFX, PostgreSQL/pgAdmin вместо Visual Studio/SSMS), §4.2 (новые скриншоты), §4.3 (классы: `PasswordHasher` с PBKDF2, `MonitoringSession`, `Permissions`), гл. 5 (новые тест-примеры + автотесты JUnit), приложение А (новый код).
