package ru.vlsu.security.web;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import ru.vlsu.security.dao.DataAccessException;
import ru.vlsu.security.dao.DbErrors;
import ru.vlsu.security.model.User;
import ru.vlsu.security.service.AuthException;
import ru.vlsu.security.service.RegistrationForm;
import ru.vlsu.security.service.RegistrationResult;

import java.io.Serializable;

/** Авторизация и регистрация (бывший LoginController). */
@Named
@ViewScoped
public class LoginBean implements Serializable {

    @Inject
    AppResources app;
    @Inject
    UserSession session;
    @Inject
    FacesUi ui;

    private boolean registration;
    private String login;
    private String password;
    private String regLogin;
    private String regFullName;
    private String regPassword;
    private String regRepeat;

    /** Переход на первый доступный раздел или null — остаться на странице. */
    public String login() {
        try {
            User user = app.ctx().auth().login(login, chars(password));
            password = null;
            ui.renewSession();
            session.login(user);
            return Pages.home(user.role()) + "?faces-redirect=true";
        } catch (AuthException e) {
            ui.warn(e.getMessage());
        } catch (DataAccessException e) {
            ui.error(DbErrors.message(e));
        }
        return null;
    }

    public void register() {
        RegistrationForm form = new RegistrationForm(regLogin, chars(regPassword), chars(regRepeat), regFullName);
        try {
            RegistrationResult result = app.ctx().auth().register(form);
            ui.info(result.message());
            login = result.user().login();
            regLogin = null;
            regFullName = null;
            regPassword = null;
            regRepeat = null;
            registration = false;
        } catch (AuthException e) {
            ui.warn(e.getMessage());
        } catch (DataAccessException e) {
            ui.error(DbErrors.message(e));
        }
    }

    public void showRegistration() {
        registration = true;
    }

    public void showLogin() {
        registration = false;
    }

    private static char[] chars(String s) {
        return s == null ? new char[0] : s.toCharArray();
    }

    public boolean isRegistration() {
        return registration;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRegLogin() {
        return regLogin;
    }

    public void setRegLogin(String regLogin) {
        this.regLogin = regLogin;
    }

    public String getRegFullName() {
        return regFullName;
    }

    public void setRegFullName(String regFullName) {
        this.regFullName = regFullName;
    }

    public String getRegPassword() {
        return regPassword;
    }

    public void setRegPassword(String regPassword) {
        this.regPassword = regPassword;
    }

    public String getRegRepeat() {
        return regRepeat;
    }

    public void setRegRepeat(String regRepeat) {
        this.regRepeat = regRepeat;
    }
}
