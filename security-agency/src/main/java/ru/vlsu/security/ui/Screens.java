package ru.vlsu.security.ui;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import ru.vlsu.security.AppContext;
import ru.vlsu.security.model.User;
import ru.vlsu.security.security.Section;

/** Фабрика экранов разделов главного меню. */
public final class Screens {

    /** Обработчики жизненного цикла экрана; хранятся в properties узла. */
    public interface Lifecycle {
        default void onShown() {
        }

        default void dispose() {
        }
    }

    private static final String KEY = "screen.lifecycle";

    private Screens() {
    }

    public static Parent open(Section section, AppContext ctx, User user) {
        return switch (section) {
            default -> placeholder(section);
        };
    }

    public static <T extends Node> T withLifecycle(T node, Lifecycle lifecycle) {
        node.getProperties().put(KEY, lifecycle);
        return node;
    }

    static void onShown(Node node) {
        if (node.getProperties().get(KEY) instanceof Lifecycle l) {
            l.onShown();
        }
    }

    static void dispose(Node node) {
        if (node.getProperties().get(KEY) instanceof Lifecycle l) {
            l.dispose();
        }
    }

    private static Parent placeholder(Section section) {
        Label label = new Label(section.title());
        label.getStyleClass().add("screen-title");
        return new StackPane(label);
    }
}
