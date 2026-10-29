package ru.vlsu.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vlsu.security.TestDb;
import ru.vlsu.security.dao.UserDao;
import ru.vlsu.security.model.MonitoredObject;
import ru.vlsu.security.model.SecurityStatus;
import ru.vlsu.security.monitoring.ObjectKind;
import ru.vlsu.security.monitoring.SensorType;
import ru.vlsu.security.security.PasswordHasher;
import ru.vlsu.security.security.Role;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlarmServiceTest {

    private AlarmService alarms;
    private AuditService audit;
    private long operator;

    @BeforeEach
    void setUp() {
        TestDb.reset();
        audit = new AuditService(TestDb.dataSource());
        alarms = new AlarmService(TestDb.dataSource(), audit);
        operator = new UserDao(TestDb.dataSource()).insert("op", PasswordHasher.hash("123456".toCharArray()),
                "Операторов О.О.", Role.OPERATOR, true);
    }

    static String scalar(String sql) throws Exception {
        try (var c = TestDb.dataSource().getConnection(); var st = c.createStatement(); var rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    @Test
    void listActive_car_labelsFormat() {
        List<MonitoredObject> cars = alarms.listActive(ObjectKind.CAR);
        assertEquals(3, cars.size());
        MonitoredObject ford = cars.get(0);
        assertEquals("[4] Ford Fusion (М117РН)", ford.label());
        assertEquals("Каприянов Андрей Фёдорович", ford.ownerName());
        assertEquals(SecurityStatus.ARMED, ford.securityStatus());
    }

    @Test
    void listActive_ndv_labelsFormat() {
        List<MonitoredObject> flats = alarms.listActive(ObjectKind.NDV);
        assertEquals(3, flats.size());
        assertEquals("[1] г. Владимир, ул. Школьная, д. 7, кв. 104", flats.get(0).label());
    }

    @Test
    void recordAlarm_insertsEvent_andSetsAlarmStatus() throws Exception {
        long id = alarms.recordAlarm(1, SensorType.PANIC, operator, "КТС: ...");
        assertEquals("panic_button", scalar("SELECT event_type FROM alarm_events WHERE event_id = " + id));
        assertEquals("critical", scalar("SELECT priority FROM alarm_events WHERE event_id = " + id));
        assertEquals("new", scalar("SELECT status FROM alarm_events WHERE event_id = " + id));
        assertEquals("alarm", scalar("SELECT security_status FROM protected_objects WHERE object_id = 1"));
    }

    @Test
    void recordAlarm_vehicle_hasCoordinates() throws Exception {
        long id = alarms.recordAlarm(4, SensorType.HOOD, operator, "msg");
        assertEquals("56.129100", scalar("SELECT latitude FROM alarm_events WHERE event_id = " + id));
        assertEquals("40.406700", scalar("SELECT longitude FROM alarm_events WHERE event_id = " + id));
    }

    @Test
    void recordAlarm_realEstate_noCoordinates() throws Exception {
        long id = alarms.recordAlarm(1, SensorType.MOTION, operator, "msg");
        assertNull(scalar("SELECT latitude FROM alarm_events WHERE event_id = " + id));
    }

    @Test
    void setSecurityStatus_writesAudit() throws Exception {
        alarms.setSecurityStatus(1, SecurityStatus.DISARMED, operator);
        assertEquals("disarmed", scalar("SELECT security_status FROM protected_objects WHERE object_id = 1"));
        assertTrue(audit.findRecent(5).stream().anyMatch(a -> a.action().equals("DISARM") && a.entityId() == 1L));
    }

    @Test
    void logIgnitionAndMchs_writeAudit() {
        alarms.logIgnition(4, true, operator);
        alarms.logMchs(1, operator);
        List<String> actions = audit.findRecent(5).stream().map(AuditService.AuditEntry::action).toList();
        assertTrue(actions.contains("IGNITION_BLOCK"));
        assertTrue(actions.contains("MCHS_CALL"));
    }

    @Test
    void markFalseAlarm_rearmsObject() throws Exception {
        long id = alarms.recordAlarm(1, SensorType.MOTION, operator, "msg");
        alarms.markFalseAlarm(id, operator);
        assertEquals("false_alarm", scalar("SELECT status FROM alarm_events WHERE event_id = " + id));
        assertEquals("armed", scalar("SELECT security_status FROM protected_objects WHERE object_id = 1"));
    }

    @Test
    void findRecent_includesDispatchColumns() {
        alarms.recordAlarm(1, SensorType.MOTION, operator, "msg");
        var rows = alarms.findRecent(10);
        assertEquals(1, rows.size());
        assertEquals("Операторов О.О.", rows.get(0).operatorName());
        assertNull(rows.get(0).dispatchId());
    }
}
