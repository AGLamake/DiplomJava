package ru.vlsu.security.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import static ru.vlsu.security.App.TITLE;

/** Обёртки над Alert вместо MessageBox из WinForms-версии. */
public final class Dialogs {

    private Dialogs() {
    }

    public static void info(String message) {
        show(Alert.AlertType.INFORMATION, message);
    }

    public static void warning(String message) {
        show(Alert.AlertType.WARNING, message);
    }

    public static void error(String message) {
        show(Alert.AlertType.ERROR, message);
    }

    public static boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setTitle(TITLE);
        alert.setHeaderText(null);
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private static void show(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message);
        alert.setTitle(TITLE);
        alert.setHeaderText(null);
        try {
            alert.showAndWait();
        } catch (IllegalStateException e) {
            // Вызов из таймера (Timeline): модальное ожидание запрещено, показываем без ожидания.
            alert.show();
        }
    }
}
