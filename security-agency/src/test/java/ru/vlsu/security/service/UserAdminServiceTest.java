package ru.vlsu.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vlsu.security.TestDb;
import ru.vlsu.security.dao.UserDao;
import ru.vlsu.security.model.User;
import ru.vlsu.security.security.Role;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAdminServiceTest {

    private UserDao users;
    private AuditService audit;
    private AuthService auth;
    private UserAdminService admin;
    private User administrator;
    private User operator;

    @BeforeEach
    void setUp() throws AuthException {
        TestDb.reset();
        users = new UserDao(TestDb.dataSource());
        audit = new AuditService(TestDb.dataSource());
        auth = new AuthService(users, audit);
        admin = new UserAdminService(users, audit);
        administrator = auth.register(new RegistrationForm("admin", "secret1".toCharArray(),
                "secret1".toCharArray(), "Админов А.А.")).user();
        operator = auth.register(new RegistrationForm("Operator", "123456".toCharArray(),
                "123456".toCharArray(), "Операторов О.О.")).user();
    }

    private static User with(User u, Role role, boolean active) {
        return new User(u.id(), u.login(), u.fullName(), role, active, u.phone(), u.email());
    }

    @Test
    void update_activatesOperator() throws AuthException {
        admin.update(administrator, with(operator, Role.OPERATOR, true));
        assertEquals("Operator", auth.login("Operator", "123456".toCharArray()).login());
    }

    @Test
    void update_ownRole_throws() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> admin.update(administrator, with(administrator, Role.OPERATOR, true)));
        assertEquals("Нельзя изменить собственную роль или заблокировать себя", e.getMessage());
    }

    @Test
    void update_ownActive_throws() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> admin.update(administrator, with(administrator, Role.ADMINISTRATOR, false)));
        assertEquals("Нельзя изменить собственную роль или заблокировать себя", e.getMessage());
    }

    @Test
    void update_ownNameAllowed() {
        admin.update(administrator, new User(administrator.id(), "admin", "Новое Имя", Role.ADMINISTRATOR, true,
                "+7", null));
        assertEquals("Новое Имя", users.findById(administrator.id()).orElseThrow().fullName());
    }

    @Test
    void resetPassword_short_throws() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> admin.resetPassword(administrator, operator.id(), "123".toCharArray()));
        assertEquals("Пароль должен содержать не менее 6 символов", e.getMessage());
    }

    @Test
    void resetPassword_thenLoginWithNew() throws AuthException {
        admin.update(administrator, with(operator, Role.OPERATOR, true));
        admin.resetPassword(administrator, operator.id(), "newpass".toCharArray());
        assertEquals("Operator", auth.login("Operator", "newpass".toCharArray()).login());
    }

    @Test
    void update_writesAudit() {
        admin.update(administrator, with(operator, Role.ENGINEER, true));
        assertTrue(audit.findRecent(10).stream().anyMatch(a -> a.action().equals("USER_UPDATE")
                && a.entityId() != null && a.entityId() == operator.id()));
    }
}
