package ru.vlsu.security.ui;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ru.vlsu.security.AppContext;
import ru.vlsu.security.dao.DataAccessException;
import ru.vlsu.security.dao.DbErrors;
import ru.vlsu.security.model.User;
import ru.vlsu.security.security.Permissions;
import ru.vlsu.security.security.Section;

import java.util.EnumMap;
import java.util.Map;

public class MainController {

    @FXML private Label userLabel;
    @FXML private VBox menu;
    @FXML private StackPane contentPane;

    private AppContext ctx;
    private User user;
    private Runnable onLogout;
    private final Map<Section, Node> screens = new EnumMap<>(Section.class);
    private final Map<Section, Button> buttons = new EnumMap<>(Section.class);

    public void init(AppContext ctx, User user, Runnable onLogout) {
        this.ctx = ctx;
        this.user = user;
        this.onLogout = onLogout;
        userLabel.setText("Пользователь: " + user.fullName() + " (" + user.role().title() + ")");
        for (Section section : Permissions.visibleSections(user.role())) {
            Button b = new Button(section.title());
            b.setMaxWidth(Double.MAX_VALUE);
            b.setOnAction(e -> open(section));
            buttons.put(section, b);
            menu.getChildren().add(b);
        }
        if (!buttons.isEmpty()) {
            open(buttons.keySet().iterator().next());
        }
    }

    private void open(Section section) {
        Node screen = screens.get(section);
        if (screen == null) {
            try {
                screen = Screens.open(section, ctx, user);
            } catch (DataAccessException e) {
                Dialogs.error(DbErrors.message(e));
                return;
            }
            screens.put(section, screen);
            contentPane.getChildren().add(screen);
        }
        for (Node n : contentPane.getChildren()) {
            n.setVisible(n == screen);
        }
        buttons.forEach((s, b) -> b.getStyleClass().remove("selected"));
        buttons.get(section).getStyleClass().add("selected");
        Screens.onShown(screen);
    }

    /** Останавливает таймеры экранов мониторинга. */
    public void dispose() {
        screens.values().forEach(Screens::dispose);
    }

    @FXML
    private void onAbout() {
        Dialogs.info("Данная программа создана в рамках дипломной работы:\n"
                + "Информационная система охранного агентства\n"
                + "Создатель:\nСтудент группы ПКуспк-320\nМорозов Иван Валерьевич");
    }

    @FXML
    private void onLogout() {
        dispose();
        onLogout.run();
    }
}
