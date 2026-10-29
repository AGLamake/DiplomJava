package ru.vlsu.security.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vlsu.security.AppContext;
import ru.vlsu.security.TestDb;
import ru.vlsu.security.config.AppConfig;
import ru.vlsu.security.model.User;
import ru.vlsu.security.security.Permissions;
import ru.vlsu.security.security.Role;
import ru.vlsu.security.security.Section;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Загружает FXML-экраны, проверяет меню по ролям и сохраняет снимки в target/ui-snapshots. */
class UiSmokeTest {

    private AppContext ctx;

    @BeforeEach
    void setUp() {
        TestDb.reset();
        ctx = AppContext.create(TestDb.dataSource(), new AppConfig(900, 900));
    }

    static FXMLLoader loader(String fxml) {
        return new FXMLLoader(UiSmokeTest.class.getResource("/ru/vlsu/security/ui/" + fxml));
    }

    @Test
    void loginScreen_loads() throws Exception {
        FxTestSupport.onFx(() -> {
            FXMLLoader l = loader("login.fxml");
            Parent root = l.load();
            LoginController c = l.getController();
            assertNotNull(c);
            c.init(ctx, u -> { });
            FxTestSupport.snapshot(root, "login");
            return null;
        });
    }

    @Test
    void everySection_opensForAdministrator() throws Exception {
        User admin = new User(1, "admin", "Админов А.А.", Role.ADMINISTRATOR, true, null, null);
        for (Section s : Permissions.visibleSections(Role.ADMINISTRATOR)) {
            FxTestSupport.onFx(() -> {
                Parent screen = Screens.open(s, ctx, admin);
                FxTestSupport.snapshot(screen, "section-" + s.name().toLowerCase());
                return null;
            });
        }
    }

    @Test
    void readOnlyAccess_hidesEditButtons() throws Exception {
        User operator = new User(1, "op", "Операторов О.О.", Role.OPERATOR, true, null, null);
        User manager = new User(2, "man", "Менеджеров М.М.", Role.MANAGER, true, null, null);
        List<String> operatorButtons = FxTestSupport.onFx(() -> buttonTexts(Screens.open(Section.CLIENTS, ctx, operator)));
        List<String> managerButtons = FxTestSupport.onFx(() -> buttonTexts(Screens.open(Section.CLIENTS, ctx, manager)));
        assertEquals(List.of("Обновить"), operatorButtons);
        assertEquals(List.of("Добавить", "Изменить", "Удалить", "Обновить"), managerButtons);
    }

    static List<String> buttonTexts(Parent root) {
        new javafx.scene.Scene(root);
        root.applyCss();
        return root.lookupAll(".button").stream()
                .filter(n -> n instanceof Button && !(n.getParent() instanceof javafx.scene.control.ComboBox))
                .map(n -> ((Button) n).getText()).toList();
    }

    @Test
    void mainMenu_showsOnlyPermittedSections() throws Exception {
        for (Role role : List.of(Role.ADMINISTRATOR, Role.OPERATOR, Role.ENGINEER, Role.RESPONSE_TEAM)) {
            User user = new User(1, "u", "Тестов Т.Т.", role, true, null, null);
            List<String> titles = FxTestSupport.onFx(() -> {
                FXMLLoader l = loader("main.fxml");
                Parent root = l.load();
                l.<MainController>getController().init(ctx, user, () -> { });
                FxTestSupport.snapshot(root, "main-" + role.code());
                VBox menu = (VBox) root.lookup(".sidebar");
                return menu.getChildren().stream().map(n -> ((Button) n).getText()).toList();
            });
            List<String> expected = Permissions.visibleSections(role).stream().map(Section::title).toList();
            assertEquals(expected, titles, role.name());
        }
    }
}
