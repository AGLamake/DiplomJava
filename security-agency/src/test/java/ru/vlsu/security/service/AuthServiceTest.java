package ru.vlsu.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vlsu.security.TestDb;
import ru.vlsu.security.dao.UserDao;
import ru.vlsu.security.model.User;
import ru.vlsu.security.security.Role;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    private UserDao users;
    private AuditService audit;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        TestDb.reset();
        users = new UserDao(TestDb.dataSource());
        audit = new AuditService(TestDb.dataSource());
        auth = new AuthService(users, audit);
    }

    private static RegistrationForm form(String login, String password, String repeat, String fullName) {
        return new RegistrationForm(login, password.toCharArray(), repeat.toCharArray(), fullName);
    }

    private User registerAdmin() throws AuthException {
        return auth.register(form("admin", "secret1", "secret1", "Иванов И.И.")).user();
    }

    @Test
    void firstRegistration_isActiveAdministrator() throws AuthException {
        RegistrationResult result = auth.register(form("admin", "secret1", "secret1", "Иванов И.И."));
        assertEquals(Role.ADMINISTRATOR, result.user().role());
        assertTrue(result.user().active());
        assertEquals("Учётная запись администратора создана. Выполните вход.", result.message());
    }

    @Test
    void secondRegistration_isInactiveOperator() throws AuthException {
        registerAdmin();
        RegistrationResult result = auth.register(form("Operator", "123456", "123456", "Петров П.П."));
        assertEquals(Role.OPERATOR, result.user().role());
        assertFalse(result.user().active());
        assertEquals("Заявка на регистрацию отправлена администратору.", result.message());
    }

    @Test
    void register_passwordMismatch_throws_andNothingInserted() {
        AuthException e = assertThrows(AuthException.class,
                () -> auth.register(form("admin", "secret1", "secret2", "Иванов И.И.")));
        assertEquals("Пароли отличаются!", e.getMessage());
        assertEquals(0, users.count());
    }

    @Test
    void register_duplicateLogin_throws() throws AuthException {
        registerAdmin();
        AuthException e = assertThrows(AuthException.class,
                () -> auth.register(form("admin", "secret1", "secret1", "Другой")));
        assertEquals("Пользователь с таким логином уже существует", e.getMessage());
    }

    @Test
    void register_shortPassword_throws() {
        AuthException e = assertThrows(AuthException.class,
                () -> auth.register(form("admin", "12345", "12345", "Иванов И.И.")));
        assertEquals("Пароль должен содержать не менее 6 символов", e.getMessage());
    }

    @Test
    void register_blankFields_throws() {
        AuthException e = assertThrows(AuthException.class,
                () -> auth.register(form("   ", "secret1", "secret1", "Иванов И.И.")));
        assertEquals("Заполните все поля!", e.getMessage());
        AuthException e2 = assertThrows(AuthException.class,
                () -> auth.register(form("admin", "secret1", "secret1", " ")));
        assertEquals("Заполните все поля!", e2.getMessage());
    }

    @Test
    void login_ok_returnsUser() throws AuthException {
        registerAdmin();
        User u = auth.login("admin", "secret1".toCharArray());
        assertEquals("admin", u.login());
        assertEquals("Иванов И.И.", u.fullName());
    }

    @Test
    void login_wrongPassword_throws() throws AuthException {
        registerAdmin();
        AuthException e = assertThrows(AuthException.class, () -> auth.login("admin", "wrong12".toCharArray()));
        assertEquals("Неверные данные для входа!", e.getMessage());
    }

    @Test
    void login_unknownUser_throws() {
        AuthException e = assertThrows(AuthException.class, () -> auth.login("nobody", "secret1".toCharArray()));
        assertEquals("Неверные данные для входа!", e.getMessage());
    }

    @Test
    void login_blank_throws() {
        AuthException e = assertThrows(AuthException.class, () -> auth.login("", "".toCharArray()));
        assertEquals("Заполните все поля!", e.getMessage());
    }

    @Test
    void login_inactive_throws() throws AuthException {
        registerAdmin();
        auth.register(form("Operator", "123456", "123456", "Петров П.П."));
        AuthException e = assertThrows(AuthException.class, () -> auth.login("Operator", "123456".toCharArray()));
        assertEquals("Учётная запись ожидает активации администратором", e.getMessage());
    }

    @Test
    void login_clientRole_throws() throws AuthException {
        User admin = registerAdmin();
        User op = auth.register(form("client1", "123456", "123456", "Клиент К.К.")).user();
        users.update(new User(op.id(), op.login(), op.fullName(), Role.CLIENT, true, null, null));
        AuthException e = assertThrows(AuthException.class, () -> auth.login("client1", "123456".toCharArray()));
        assertEquals("Для роли «Клиент» вход в приложение не предусмотрен", e.getMessage());
        assertEquals(Role.ADMINISTRATOR, admin.role());
    }

    @Test
    void login_sqlInjectionAttempt_fails() throws AuthException {
        registerAdmin();
        AuthException e = assertThrows(AuthException.class,
                () -> auth.login("' OR '1'='1", "' OR '1'='1".toCharArray()));
        assertEquals("Неверные данные для входа!", e.getMessage());
    }

    @Test
    void register_writesAudit() throws AuthException {
        User admin = registerAdmin();
        assertTrue(audit.findRecent(10).stream().anyMatch(a ->
                a.action().equals("REGISTER") && a.entityType().equals("user")
                        && a.entityId() != null && a.entityId() == admin.id()));
    }
}
