package ru.vlsu.security;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class App extends Application {

    public static final String TITLE = "Охранное агентство";

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        stage.setTitle(TITLE);
        stage.setScene(new Scene(new StackPane(), 1280, 720));
        stage.show();
    }
}
