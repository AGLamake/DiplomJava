package ru.vlsu.security;

import atlantafx.base.theme.PrimerLight;
import com.zaxxer.hikari.HikariDataSource;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import ru.vlsu.security.config.AppConfig;
import ru.vlsu.security.config.Database;
import ru.vlsu.security.model.User;
import ru.vlsu.security.ui.LoginController;
import ru.vlsu.security.ui.MainController;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;

public class App extends Application {

    public static final String TITLE = "Охранное агентство";

    private HikariDataSource dataSource;
    private AppContext ctx;
    private Stage stage;
    private Scene scene;
    private MainController mainController;

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
        ctx = AppContext.create(dataSource, AppConfig.load());

        this.stage = stage;
        stage.setTitle(TITLE);
        showLogin();
        stage.show();
    }

    private void showLogin() {
        FXMLLoader loader = loader("login.fxml");
        Parent root = load(loader);
        loader.<LoginController>getController().init(ctx, this::showMain);
        setRoot(root, 1280, 720);
    }

    private void showMain(User user) {
        FXMLLoader loader = loader("main.fxml");
        Parent root = load(loader);
        mainController = loader.getController();
        mainController.init(ctx, user, () -> {
            mainController = null;
            showLogin();
        });
        setRoot(root, 1280, 720);
    }

    private void setRoot(Parent root, double width, double height) {
        if (scene == null) {
            scene = new Scene(root, width, height);
            scene.getStylesheets().add(App.class.getResource("ui/app.css").toExternalForm());
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }
    }

    private static FXMLLoader loader(String fxml) {
        return new FXMLLoader(App.class.getResource("ui/" + fxml));
    }

    private static Parent load(FXMLLoader loader) {
        try {
            return loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void stop() {
        if (mainController != null) {
            mainController.dispose();
        }
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
