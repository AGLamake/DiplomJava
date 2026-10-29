package ru.vlsu.security;

import atlantafx.base.theme.PrimerLight;
import com.zaxxer.hikari.HikariDataSource;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ru.vlsu.security.config.Database;

import java.util.Properties;

public class App extends Application {

    public static final String TITLE = "Охранное агентство";

    private HikariDataSource dataSource;

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        Properties dbProps;
        try {
            dbProps = Database.loadProperties("db.properties");
        } catch (IllegalStateException e) {
            fatal(e.getMessage());
            return;
        }
        dataSource = Database.create(dbProps);
        if (!Database.ping(dataSource)) {
            fatal("Не удалось подключиться к базе данных: " + dbProps.getProperty("db.url"));
            return;
        }

        stage.setTitle(TITLE);
        stage.setScene(new Scene(new StackPane(), 1280, 720));
        stage.show();
    }

    @Override
    public void stop() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private static void fatal(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle(TITLE);
        alert.setHeaderText("Ошибка запуска");
        alert.showAndWait();
        Platform.exit();
    }
}
