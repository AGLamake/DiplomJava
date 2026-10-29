package ru.vlsu.security;

import javafx.application.Application;

/**
 * Точка входа для fat-jar: класс не наследует Application,
 * поэтому JavaFX можно запускать с classpath без module-path.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
