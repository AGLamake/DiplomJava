package ru.vlsu.security.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vlsu.security.AppContext;
import ru.vlsu.security.model.User;
import ru.vlsu.security.monitoring.SensorType;
import ru.vlsu.security.security.Role;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.vlsu.security.TestDb.scalar;

class MonitorBeanTest {

    private AppContext ctx;
    private User operator;
    private WebTestSupport.RecordingUi ui;
    private long clock = 1_000_000;

    @BeforeEach
    void setUp() {
        ctx = WebTestSupport.freshContext(2);
        operator = WebTestSupport.user(ctx, "op", "Операторов О.О.", Role.OPERATOR);
        ui = new WebTestSupport.RecordingUi();
    }

    private <B extends MonitorBean> B wire(B bean) {
        bean.app = AppResources.forTests(ctx);
        bean.userSession = WebTestSupport.session(operator);
        bean.ui = ui;
        bean.reload();
        bean.setObjectId(bean.getObjects().get(0).objectId());
        bean.select();
        return bean;
    }

    private CarMonitorBean car() {
        return wire(new CarMonitorBean() {
            @Override
            long now() {
                return clock;
            }
        });
    }

    private NdvMonitorBean ndv() {
        return wire(new NdvMonitorBean() {
            @Override
            long now() {
                return clock;
            }
        });
    }

    private static String last(List<String> lines) {
        return lines.get(lines.size() - 1);
    }

    @Test
    void car_threeSensors_dispatchAndArrival() {
        CarMonitorBean m = car();
        m.trigger(SensorType.HOOD);
        assertEquals(true, ui.callbacks.get("fired"));
        m.trigger(SensorType.FRONT_DOOR);
        m.trigger(SensorType.REAR_DOOR);
        m.requestGbr();
        List<String> lines = m.getJournal();
        assertTrue(lines.get(0).endsWith(
                "Подкапотное пространство: Срабатывание датчика взлома №1. Автомобиль [[4] Ford Fusion (М117РН)]."));
        assertTrue(lines.stream().anyMatch(l -> l.contains("Множественное срабатывание датчиков: [3].")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("ГБР: Группа Быстрого Реагирования отправлена [Операторов О.О.]")));
        assertTrue(last(lines).contains("уже в пути"));
        assertEquals("1 мин", m.getGbrText());
        assertEquals("3", scalar("SELECT count(*) FROM alarm_events WHERE object_id = 4"));
        assertEquals("dispatched", scalar("SELECT status FROM response_teams WHERE team_id = 1"));

        clock += 3500; // опрос пропущен: таймер догоняет прошедшее время одним вызовом
        m.poll();
        assertTrue(last(m.getJournal()).contains("ГБР: Группа Быстрого Реагирования прибыла"));
        assertEquals("ГБР прибыла на место происшествия", m.getStatus());
        assertEquals("—", m.getGbrText());
        assertNotNull(scalar("SELECT arrived_at FROM dispatches"));
        assertEquals(List.of("monitorForm:info", "monitorForm:journal"), ui.updated);

        ui.updated.clear();
        m.poll();
        assertTrue(ui.updated.isEmpty(), "без изменений страница не перерисовывается");
    }

    @Test
    void ndv_disarm_blocksSensors_andWritesAudit() {
        NdvMonitorBean m = ndv();
        assertFalse(m.isSensorsDisabled());
        m.setArmed(false);
        m.armedChanged();
        assertTrue(m.isSensorsDisabled());
        m.trigger(SensorType.PANIC);
        assertFalse(ui.callbacks.containsKey("fired"));
        m.callMchs();
        assertEquals("1 мин", m.getMchsText());
        assertEquals("disarmed", scalar("SELECT security_status FROM protected_objects WHERE object_id = 1"));
        assertTrue(ctx.audit().findRecent(10).stream().anyMatch(a -> a.action().equals("DISARM")));
        assertTrue(ctx.audit().findRecent(10).stream().anyMatch(a -> a.action().equals("MCHS_CALL")));
    }

    @Test
    void ndv_panic_noTeams_reportsInJournal() throws Exception {
        ctx.dispatches().dispatch(2, null, operator.id());
        ctx.dispatches().dispatch(3, null, operator.id());
        NdvMonitorBean m = ndv();
        m.trigger(SensorType.PANIC);
        assertTrue(last(m.getJournal()).endsWith("Нет свободных групп быстрого реагирования"));
    }

    @Test
    void readOnlyUser_cannotTriggerOrDisarm() {
        operator = WebTestSupport.user(ctx, "eng", "Инженеров И.И.", Role.ENGINEER);
        NdvMonitorBean m = ndv();
        assertTrue(m.isSensorsDisabled());
        assertTrue(m.isArmedDisabled());
        m.setArmed(false);
        m.armedChanged();
        assertTrue(m.isArmed(), "переключатель возвращается в прежнее положение");
        assertEquals("armed", scalar("SELECT security_status FROM protected_objects WHERE object_id = 1"));
    }
}
