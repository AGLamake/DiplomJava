package ru.vlsu.security.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vlsu.security.AppContext;
import ru.vlsu.security.security.Role;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginBeanTest {

    private AppContext ctx;
    private WebTestSupport.RecordingUi ui;
    private UserSession session;
    private LoginBean bean;

    @BeforeEach
    void setUp() {
        ctx = WebTestSupport.freshContext(900);
        ui = new WebTestSupport.RecordingUi();
        session = new UserSession();
        session.ui = ui;
        bean = new LoginBean();
        bean.app = AppResources.forTests(ctx);
        bean.session = session;
        bean.ui = ui;
    }

    private void register(String login, String name) {
        bean.showRegistration();
        bean.setRegLogin(login);
        bean.setRegFullName(name);
        bean.setRegPassword("123456");
        bean.setRegRepeat("123456");
        bean.register();
    }

    @Test
    void firstUser_isAdministrator_andLogsIn() {
        register("admin", "Админов А.А.");
        assertEquals(List.of("Учётная запись администратора создана. Выполните вход."), ui.infos);
        assertFalse(bean.isRegistration(), "после регистрации показывается форма входа");
        assertEquals("admin", bean.getLogin());

        bean.setPassword("123456");
        assertEquals("/car.xhtml?faces-redirect=true", bean.login());
        assertEquals(Role.ADMINISTRATOR, session.getUser().role());
        assertNull(bean.getPassword(), "пароль не хранится после входа");
    }

    @Test
    void secondUser_waitsForActivation() {
        register("admin", "Админов А.А.");
        register("op", "Операторов О.О.");
        bean.setLogin("op");
        bean.setPassword("123456");
        assertNull(bean.login());
        assertEquals(List.of("Учётная запись ожидает активации администратором"), ui.warnings);
        assertFalse(session.isLoggedIn());
    }

    @Test
    void wrongPassword_warns() {
        register("admin", "Админов А.А.");
        bean.setPassword("bad");
        assertNull(bean.login());
        assertEquals(List.of("Неверные данные для входа!"), ui.warnings);
    }

    @Test
    void registration_passwordMismatch_keepsForm() {
        bean.showRegistration();
        bean.setRegLogin("x");
        bean.setRegFullName("X");
        bean.setRegPassword("123456");
        bean.setRegRepeat("654321");
        bean.register();
        assertEquals(List.of("Пароли отличаются!"), ui.warnings);
        assertTrue(bean.isRegistration());
    }
}
