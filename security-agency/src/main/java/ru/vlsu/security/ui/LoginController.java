package ru.vlsu.security.ui;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import ru.vlsu.security.AppContext;
import ru.vlsu.security.dao.DataAccessException;
import ru.vlsu.security.dao.DbErrors;
import ru.vlsu.security.model.User;
import ru.vlsu.security.service.AuthException;
import ru.vlsu.security.service.RegistrationForm;
import ru.vlsu.security.service.RegistrationResult;

import java.util.function.Consumer;

public class LoginController {

    @FXML private VBox loginPane;
    @FXML private VBox registrationPane;
    @FXML private TextField loginField;
    @FXML private PasswordField passwordField;
    @FXML private TextField regLoginField;
    @FXML private TextField regFullNameField;
    @FXML private PasswordField regPasswordField;
    @FXML private PasswordField regRepeatField;

    private AppContext ctx;
    private Consumer<User> onLoggedIn;

    public void init(AppContext ctx, Consumer<User> onLoggedIn) {
        this.ctx = ctx;
        this.onLoggedIn = onLoggedIn;
    }

    @FXML
    private void onLogin() {
        try {
            User user = ctx.auth().login(loginField.getText(), passwordField.getText().toCharArray());
            passwordField.clear();
            onLoggedIn.accept(user);
        } catch (AuthException e) {
            Dialogs.warning(e.getMessage());
        } catch (DataAccessException e) {
            Dialogs.error(DbErrors.message(e));
        }
    }

    @FXML
    private void onRegister() {
        RegistrationForm form = new RegistrationForm(regLoginField.getText(),
                regPasswordField.getText().toCharArray(), regRepeatField.getText().toCharArray(),
                regFullNameField.getText());
        try {
            RegistrationResult result = ctx.auth().register(form);
            Dialogs.info(result.message());
            loginField.setText(result.user().login());
            regLoginField.clear();
            regFullNameField.clear();
            regPasswordField.clear();
            regRepeatField.clear();
            onShowLogin();
        } catch (AuthException e) {
            Dialogs.warning(e.getMessage());
        } catch (DataAccessException e) {
            Dialogs.error(DbErrors.message(e));
        }
    }

    @FXML
    private void onShowRegistration() {
        toggle(registrationPane, loginPane);
    }

    @FXML
    private void onShowLogin() {
        toggle(loginPane, registrationPane);
    }

    private static void toggle(VBox show, VBox hide) {
        hide.setVisible(false);
        hide.setManaged(false);
        show.setVisible(true);
        show.setManaged(true);
    }
}
