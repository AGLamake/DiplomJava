package ru.vlsu.security.ui;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vlsu.security.AppContext;
import ru.vlsu.security.TestDb;
import ru.vlsu.security.config.AppConfig;
import ru.vlsu.security.model.User;
import ru.vlsu.security.security.PasswordHasher;
import ru.vlsu.security.security.Role;
import ru.vlsu.security.security.Section;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.vlsu.security.TestDb.scalar;

/** Сценарий мониторинга через реальный экран: датчики → ГБР → прибытие, с проверкой БД. */
class MonitorScreenTest {

    private AppContext ctx;
    private User operator;
    private Parent screen;

    @BeforeEach
    void setUp() {
        TestDb.reset();
        ctx = AppContext.create(TestDb.dataSource(), new AppConfig(2, 2));
        long id = ctx.users().insert("op", PasswordHasher.hash("123456".toCharArray()), "Операторов О.О.",
                Role.OPERATOR, true);
        operator = ctx.users().findById(id).orElseThrow();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (screen != null) {
            FxTestSupport.onFx(() -> {
                Screens.dispose(screen);
                return null;
            });
        }
    }

    private Parent open(Section section) throws Exception {
        screen = FxTestSupport.onFx(() -> {
            Parent p = Screens.open(section, ctx, operator);
            new Scene(p, 1280, 720);
            p.applyCss();
            p.layout();
            return p;
        });
        return screen;
    }

    @SuppressWarnings("unchecked")
    private static ComboBox<Object> combo(Parent p) {
        return (ComboBox<Object>) p.lookup(".combo-box");
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
    private static List<String> journal(Parent p) {
        return List.copyOf(((ListView<String>) p.lookup(".journal")).getItems());
    }

    @Test
    void car_threeSensors_dispatchAndArrival() throws Exception {
        Parent p = open(Section.CAR_MONITOR);
        List<String> lines = FxTestSupport.onFx(() -> {
            assertTrue(button(p, "Капот").isDisabled(), "датчики недоступны до выбора объекта");
            combo(p).getSelectionModel().select(0);
            button(p, "Капот").fire();
            button(p, "Передняя дверь").fire();
            button(p, "Задняя дверь").fire();
            button(p, "Отправить группу быстрого реагирования").fire();
            FxTestSupport.snapshot(p, "monitor-car");
            return journal(p);
        });
        assertTrue(lines.get(0).endsWith(
                "Подкапотное пространство: Срабатывание датчика взлома №1. Автомобиль [[4] Ford Fusion (М117РН)]."));
        assertTrue(lines.stream().anyMatch(l -> l.contains("Множественное срабатывание датчиков: [3].")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("ГБР: Группа Быстрого Реагирования отправлена [Операторов О.О.]")));
        assertTrue(lines.get(lines.size() - 1).contains("уже в пути"));
        assertEquals("3", scalar("SELECT count(*) FROM alarm_events WHERE object_id = 4"));
        assertEquals("1", scalar("SELECT count(*) FROM dispatches"));
        assertEquals("dispatched", scalar("SELECT status FROM response_teams WHERE team_id = 1"));

        Thread.sleep(3500);
        List<String> after = FxTestSupport.onFx(() -> journal(p));
        assertTrue(after.get(after.size() - 1).contains("ГБР: Группа Быстрого Реагирования прибыла"));
        assertNotNull(scalar("SELECT arrived_at FROM dispatches"));
    }

    @Test
    void ndv_disarm_blocksSensors_andWritesAudit() throws Exception {
        Parent p = open(Section.NDV_MONITOR);
        FxTestSupport.onFx(() -> {
            combo(p).getSelectionModel().select(0);
            ((atlantafx.base.controls.ToggleSwitch) p.lookup(".toggle-switch")).setSelected(false);
            assertTrue(button(p, "КТС").isDisabled());
            button(p, "Вызвать МЧС").fire();
            FxTestSupport.snapshot(p, "monitor-ndv");
            return null;
        });
        assertEquals("disarmed", scalar("SELECT security_status FROM protected_objects WHERE object_id = 1"));
        assertTrue(ctx.audit().findRecent(10).stream().anyMatch(a -> a.action().equals("DISARM")));
        assertTrue(ctx.audit().findRecent(10).stream().anyMatch(a -> a.action().equals("MCHS_CALL")));
    }

    @Test
    void ndv_panic_noTeams_reportsInJournal() throws Exception {
        ctx.dispatches().dispatch(2, null, operator.id());
        ctx.dispatches().dispatch(3, null, operator.id());
        Parent p = open(Section.NDV_MONITOR);
        List<String> lines = FxTestSupport.onFx(() -> {
            combo(p).getSelectionModel().select(0);
            button(p, "КТС").fire();
            return journal(p);
        });
        assertTrue(lines.get(lines.size() - 1).endsWith("Нет свободных групп быстрого реагирования"));
    }
}
