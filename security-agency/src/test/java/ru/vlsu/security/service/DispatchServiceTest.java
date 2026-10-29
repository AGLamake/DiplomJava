package ru.vlsu.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vlsu.security.TestDb;
import ru.vlsu.security.dao.ResponseTeamDao;
import ru.vlsu.security.dao.UserDao;
import ru.vlsu.security.model.DispatchResult;
import ru.vlsu.security.model.ResponseTeam;
import ru.vlsu.security.monitoring.SensorType;
import ru.vlsu.security.security.PasswordHasher;
import ru.vlsu.security.security.Role;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.vlsu.security.TestDb.scalar;

class DispatchServiceTest {

    private AlarmService alarms;
    private DispatchService dispatches;
    private long operator;
    private long member;

    @BeforeEach
    void setUp() {
        TestDb.reset();
        AuditService audit = new AuditService(TestDb.dataSource());
        alarms = new AlarmService(TestDb.dataSource(), audit);
        dispatches = new DispatchService(TestDb.dataSource(), audit);
        UserDao users = new UserDao(TestDb.dataSource());
        String hash = PasswordHasher.hash("123456".toCharArray());
        operator = users.insert("op", hash, "Операторов О.О.", Role.OPERATOR, true);
        member = users.insert("gbr", hash, "Бойцов Б.Б.", Role.RESPONSE_TEAM, true);
        ResponseTeamDao teams = new ResponseTeamDao(TestDb.dataSource());
        ResponseTeam t2 = teams.findAll().get(1);
        teams.update(new ResponseTeam(t2.id(), t2.name(), t2.phone(), t2.status(), List.of(member), null));
    }

    @Test
    void dispatch_takesFirstAvailableTeam() throws Exception {
        long event = alarms.recordAlarm(1, SensorType.PANIC, operator, "msg");
        long id = dispatches.dispatch(1, event, operator);
        assertEquals("1", scalar("SELECT team_id FROM dispatches WHERE dispatch_id = " + id));
        assertEquals("dispatched", scalar("SELECT status FROM response_teams WHERE team_id = 1"));
        assertEquals("dispatching", scalar("SELECT status FROM alarm_events WHERE event_id = " + event));
    }

    @Test
    void dispatch_withoutEvent_createsManualEvent() throws Exception {
        long id = dispatches.dispatch(4, null, operator);
        assertEquals("manual_dispatch", scalar("""
                SELECT e.event_type FROM dispatches d JOIN alarm_events e ON e.event_id = d.event_id
                WHERE d.dispatch_id = """ + id));
    }

    @Test
    void dispatch_eventAlreadyDispatched_createsNewEvent() throws Exception {
        long event = alarms.recordAlarm(1, SensorType.PANIC, operator, "msg");
        dispatches.dispatch(1, event, operator);
        long second = dispatches.dispatch(1, event, operator);
        assertEquals("2", scalar("SELECT team_id FROM dispatches WHERE dispatch_id = " + second));
    }

    @Test
    void dispatch_noAvailableTeam_throws() throws Exception {
        dispatches.dispatch(1, null, operator);
        dispatches.dispatch(2, null, operator);
        String before = scalar("SELECT count(*) FROM alarm_events");
        assertThrows(NoTeamAvailableException.class, () -> dispatches.dispatch(3, null, operator));
        assertEquals(before, scalar("SELECT count(*) FROM alarm_events"));
    }

    @Test
    void markArrived_idempotent() throws Exception {
        long id = dispatches.dispatch(1, null, operator);
        dispatches.markArrived(id);
        String first = scalar("SELECT arrived_at FROM dispatches WHERE dispatch_id = " + id);
        assertNotNull(first);
        Thread.sleep(20);
        dispatches.markArrived(id);
        assertEquals(first, scalar("SELECT arrived_at FROM dispatches WHERE dispatch_id = " + id));
    }

    @Test
    void complete_freesTeam_andRearmsObject() throws Exception {
        long event = alarms.recordAlarm(1, SensorType.MOTION, operator, "msg");
        long id = dispatches.dispatch(1, event, operator);
        dispatches.complete(id, DispatchResult.FALSE_ALARM, "Окно открыто ветром", operator);
        assertEquals("available", scalar("SELECT status FROM response_teams WHERE team_id = 1"));
        assertEquals("false_alarm", scalar("SELECT status FROM alarm_events WHERE event_id = " + event));
        assertEquals("armed", scalar("SELECT security_status FROM protected_objects WHERE object_id = 1"));
        assertNotNull(scalar("SELECT arrived_at FROM dispatches WHERE dispatch_id = " + id));
        assertTrue(dispatches.activeDispatchForObject(1).isEmpty());
    }

    @Test
    void complete_noAccess_closesEvent() throws Exception {
        long event = alarms.recordAlarm(1, SensorType.MOTION, operator, "msg");
        long id = dispatches.dispatch(1, event, operator);
        dispatches.complete(id, DispatchResult.NO_ACCESS, null, operator);
        assertEquals("closed", scalar("SELECT status FROM alarm_events WHERE event_id = " + event));
    }

    @Test
    void findActiveForMember_onlyOwnTeams() throws Exception {
        dispatches.dispatch(1, null, operator);
        assertTrue(dispatches.findActiveForMember(member).isEmpty());
        long second = dispatches.dispatch(2, null, operator);
        var mine = dispatches.findActiveForMember(member);
        assertEquals(1, mine.size());
        assertEquals(second, mine.get(0).dispatchId());
        assertEquals("ГБР-2", mine.get(0).teamName());
        assertEquals(second, dispatches.activeDispatchForObject(2).orElseThrow());
    }
}
