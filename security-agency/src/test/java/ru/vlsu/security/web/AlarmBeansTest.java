package ru.vlsu.security.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vlsu.security.AppContext;
import ru.vlsu.security.TestDb;
import ru.vlsu.security.model.DispatchResult;
import ru.vlsu.security.model.ResponseTeam;
import ru.vlsu.security.model.User;
import ru.vlsu.security.monitoring.SensorType;
import ru.vlsu.security.security.Role;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlarmBeansTest {

    private AppContext ctx;
    private User operator;
    private User member;
    private WebTestSupport.RecordingUi ui;

    @BeforeEach
    void setUp() {
        ctx = WebTestSupport.freshContext(900);
        operator = WebTestSupport.user(ctx, "op", "Операторов О.О.", Role.OPERATOR);
        member = WebTestSupport.user(ctx, "gbr", "Бойцов Б.Б.", Role.RESPONSE_TEAM);
        ResponseTeam t1 = ctx.teams().findAll().get(0);
        ctx.teams().update(new ResponseTeam(t1.id(), t1.name(), t1.phone(), t1.status(), List.of(member.id()), null));
        ui = new WebTestSupport.RecordingUi();
    }

    @Test
    void alarms_actionsDependOnRow() throws Exception {
        long withDispatch = ctx.alarms().recordAlarm(1, SensorType.PANIC, operator.id(), "КТС");
        ctx.dispatches().dispatch(1, withDispatch, operator.id());
        long fresh = ctx.alarms().recordAlarm(4, SensorType.HOOD, operator.id(), "Капот");
        AlarmsBean bean = WebTestSupport.wire(new AlarmsBean(), ctx, operator, ui);
        assertEquals(2, bean.getRows().size());

        bean.setSelected(WebTestSupport.rowWhere(bean, a -> a.eventId() == fresh));
        assertTrue(bean.isCanMarkFalseAlarm());
        assertFalse(bean.isCanComplete());

        bean.setSelected(WebTestSupport.rowWhere(bean, a -> a.eventId() == withDispatch));
        assertFalse(bean.isCanMarkFalseAlarm());
        assertTrue(bean.isCanComplete());

        bean.startComplete();
        bean.complete();
        assertEquals(List.of("Поле «Результат» обязательно для заполнения"), ui.warnings);
        bean.getCompletion().setResult(DispatchResult.FALSE_ALARM);
        bean.complete();
        assertEquals(true, ui.callbacks.get("saved"));
        assertFalse(bean.isCanComplete(), "завершённый выезд нельзя завершить повторно");
    }

    @Test
    void alarms_readOnlyForManager() {
        User manager = WebTestSupport.user(ctx, "m", "Менеджеров М.М.", Role.MANAGER);
        long fresh = ctx.alarms().recordAlarm(4, SensorType.HOOD, operator.id(), "Капот");
        AlarmsBean bean = WebTestSupport.wire(new AlarmsBean(), ctx, manager, ui);
        assertFalse(bean.isCanWrite());
        bean.setSelected(WebTestSupport.rowWhere(bean, a -> a.eventId() == fresh));
        bean.markFalseAlarm();
        assertEquals("new", TestDb.scalar("SELECT status FROM alarm_events WHERE event_id = " + fresh));
    }

    @Test
    void myDispatches_ownTeamOnly_markArrived() throws Exception {
        ctx.dispatches().dispatch(1, null, operator.id()); // ГБР-1 — команда сотрудника
        ctx.dispatches().dispatch(2, null, operator.id()); // ГБР-2 — чужая
        MyDispatchesBean bean = WebTestSupport.wire(new MyDispatchesBean(), ctx, member, ui);
        assertEquals(1, bean.getRows().size());
        bean.setSelected(bean.getRows().get(0));
        assertTrue(bean.isCanMarkArrived());
        bean.markArrived();
        assertEquals("1", TestDb.scalar("SELECT count(*) FROM dispatches WHERE arrived_at IS NOT NULL"));
        assertFalse(bean.isCanMarkArrived());
    }
}
