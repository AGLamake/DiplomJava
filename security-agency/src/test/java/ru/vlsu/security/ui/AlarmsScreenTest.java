package ru.vlsu.security.ui;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vlsu.security.AppContext;
import ru.vlsu.security.TestDb;
import ru.vlsu.security.config.AppConfig;
import ru.vlsu.security.model.ResponseTeam;
import ru.vlsu.security.model.User;
import ru.vlsu.security.monitoring.SensorType;
import ru.vlsu.security.security.PasswordHasher;
import ru.vlsu.security.security.Role;
import ru.vlsu.security.security.Section;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlarmsScreenTest {

    private AppContext ctx;
    private User operator;
    private User member;
    private final List<Parent> opened = new ArrayList<>();

    @BeforeEach
    void setUp() {
        TestDb.reset();
        ctx = AppContext.create(TestDb.dataSource(), new AppConfig(900, 900));
        String hash = PasswordHasher.hash("123456".toCharArray());
        operator = ctx.users().findById(ctx.users().insert("op", hash, "Операторов О.О.", Role.OPERATOR, true))
                .orElseThrow();
        member = ctx.users().findById(ctx.users().insert("gbr", hash, "Бойцов Б.Б.", Role.RESPONSE_TEAM, true))
                .orElseThrow();
        ResponseTeam t1 = ctx.teams().findAll().get(0);
        ctx.teams().update(new ResponseTeam(t1.id(), t1.name(), t1.phone(), t1.status(), List.of(member.id()), null));
    }

    @AfterEach
    void tearDown() throws Exception {
        FxTestSupport.onFx(() -> {
            opened.forEach(Screens::dispose);
            return null;
        });
    }

    private Parent open(Section s, User u) throws Exception {
        Parent p = FxTestSupport.onFx(() -> {
            Parent root = Screens.open(s, ctx, u);
            new Scene(root, 1280, 720);
            root.applyCss();
            root.layout();
            return root;
        });
        opened.add(p);
        return p;
    }

    private static Button button(Parent p, String text) {
        for (Node n : p.lookupAll(".button")) {
            if (n instanceof Button b && text.equals(b.getText())) {
                return b;
            }
        }
        throw new AssertionError("Нет кнопки " + text);
    }

    @SuppressWarnings("unchecked")
    private static TableView<Object> table(Parent p) {
        return (TableView<Object>) p.lookup(".table-view");
    }

    @Test
    void alarms_buttonsDependOnRow() throws Exception {
        long withDispatch = ctx.alarms().recordAlarm(1, SensorType.PANIC, operator.id(), "КТС");
        ctx.dispatches().dispatch(1, withDispatch, operator.id());
        ctx.alarms().recordAlarm(4, SensorType.HOOD, operator.id(), "Капот");
        Parent p = open(Section.ALARMS, operator);
        FxTestSupport.onFx(() -> {
            TableView<Object> t = table(p);
            assertEquals(2, t.getItems().size());
            t.getSelectionModel().select(0); // новое событие без выезда (самое свежее)
            assertFalse(button(p, "Ложная тревога").isDisabled());
            assertTrue(button(p, "Завершить выезд").isDisabled());
            t.getSelectionModel().select(1);
            assertTrue(button(p, "Ложная тревога").isDisabled());
            assertFalse(button(p, "Завершить выезд").isDisabled());
            FxTestSupport.snapshot(p, "section-alarms-data");
            return null;
        });
    }

    @Test
    void alarms_readOnlyForManager() throws Exception {
        User manager = new User(99, "m", "Менеджеров М.М.", Role.MANAGER, true, null, null);
        Parent p = open(Section.ALARMS, manager);
        List<String> buttons = FxTestSupport.onFx(() -> UiSmokeTest.buttonTexts(p));
        assertEquals(List.of("Обновить"), buttons);
    }

    @Test
    void myDispatches_showsOwnTeamOnly() throws Exception {
        ctx.dispatches().dispatch(1, null, operator.id()); // ГБР-1 — команда сотрудника
        ctx.dispatches().dispatch(2, null, operator.id()); // ГБР-2 — чужая
        Parent p = open(Section.MY_DISPATCHES, member);
        FxTestSupport.onFx(() -> {
            TableView<Object> t = table(p);
            assertEquals(1, t.getItems().size());
            t.getSelectionModel().select(0);
            assertFalse(button(p, "Прибыли на место").isDisabled());
            button(p, "Прибыли на место").fire();
            FxTestSupport.snapshot(p, "section-my-dispatches");
            return null;
        });
        assertTrue(TestDb.scalar("SELECT count(*) FROM dispatches WHERE arrived_at IS NOT NULL").equals("1"));
    }
}
