package ru.vlsu.security.web;

import org.junit.jupiter.api.Test;
import ru.vlsu.security.model.User;
import ru.vlsu.security.security.Role;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.vlsu.security.web.AuthFilter.Decision.ALLOW;
import static ru.vlsu.security.web.AuthFilter.Decision.FORBIDDEN;
import static ru.vlsu.security.web.AuthFilter.Decision.HOME;
import static ru.vlsu.security.web.AuthFilter.Decision.LOGIN;
import static ru.vlsu.security.web.AuthFilter.Decision.UNAVAILABLE;
import static ru.vlsu.security.web.AuthFilter.decide;

class AuthFilterTest {

    private static User user(Role role) {
        return new User(1, "u", "Пользователь", role, true, null, null);
    }

    @Test
    void anonymous_redirectedToLogin() {
        assertEquals(LOGIN, decide("/clients.xhtml", null, true));
        assertEquals(LOGIN, decide("/index.xhtml", null, true));
        assertEquals(ALLOW, decide("/login.xhtml", null, true));
    }

    @Test
    void resourcesAndErrorPage_alwaysAllowed() {
        assertEquals(ALLOW, decide("/jakarta.faces.resource/primefaces.js.xhtml", null, false));
        assertEquals(ALLOW, decide("/error.xhtml", null, false));
    }

    @Test
    void databaseDown_unavailable() {
        assertEquals(UNAVAILABLE, decide("/login.xhtml", null, false));
        assertEquals(UNAVAILABLE, decide("/clients.xhtml", user(Role.ADMINISTRATOR), false));
    }

    @Test
    void loggedIn_loginAndIndexGoHome() {
        assertEquals(HOME, decide("/login.xhtml", user(Role.OPERATOR), true));
        assertEquals(HOME, decide("/index.xhtml", user(Role.OPERATOR), true));
    }

    @Test
    void sectionsFollowPermissionMatrix() {
        assertEquals(ALLOW, decide("/clients.xhtml", user(Role.OPERATOR), true));
        assertEquals(FORBIDDEN, decide("/users.xhtml", user(Role.OPERATOR), true));
        assertEquals(FORBIDDEN, decide("/car.xhtml", user(Role.ENGINEER), true));
        assertEquals(ALLOW, decide("/my-dispatches.xhtml", user(Role.RESPONSE_TEAM), true));
        assertEquals(FORBIDDEN, decide("/my-dispatches.xhtml", user(Role.ADMINISTRATOR), true));
    }

    @Test
    void homeIsFirstVisibleSection() {
        assertEquals("/car.xhtml", Pages.home(Role.OPERATOR));
        assertEquals("/clients.xhtml", Pages.home(Role.ENGINEER));
        assertEquals("/alarms.xhtml", Pages.home(Role.RESPONSE_TEAM));
    }
}
