package ru.vlsu.security.service;

import ru.vlsu.security.dao.Jdbc;
import ru.vlsu.security.model.AlarmEventView;
import ru.vlsu.security.model.MonitoredObject;
import ru.vlsu.security.model.SecurityStatus;
import ru.vlsu.security.monitoring.ObjectKind;
import ru.vlsu.security.monitoring.SensorType;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.util.List;

/** Тревожные события и режим охраны объектов. */
public class AlarmService {

    /** SQL-выражение подписи объекта: «[id] Марка Модель (номер)» или «[id] адрес». */
    static final String LABEL = """
            '[' || o.object_id || '] ' || CASE WHEN v.vehicle_id IS NOT NULL
                THEN v.brand || ' ' || v.model || ' (' || v.reg_number || ')' ELSE o.address END""";

    private final Jdbc jdbc;
    private final AuditService audit;

    public AlarmService(DataSource ds, AuditService audit) {
        this.jdbc = new Jdbc(ds);
        this.audit = audit;
    }

    public List<MonitoredObject> listActive(ObjectKind kind) {
        String type = kind == ObjectKind.CAR ? "vehicle" : "real_estate";
        return jdbc.query("SELECT o.object_id, " + LABEL + ", c.name, o.security_status "
                        + """
                        FROM protected_objects o
                        JOIN clients c ON c.client_id = o.client_id
                        LEFT JOIN vehicles v ON v.object_id = o.object_id
                        WHERE o.object_type = ? AND o.object_status = 'active'
                        ORDER BY o.object_id
                        """,
                rs -> new MonitoredObject(rs.getLong(1), kind, rs.getString(2), rs.getString(3),
                        SecurityStatus.fromCode(rs.getString(4))), type);
    }

    public long recordAlarm(long objectId, SensorType sensor, long operatorId, String message) {
        return jdbc.inTransaction(c -> {
            long id = Jdbc.insert(c, """
                    INSERT INTO alarm_events (object_id, operator_id, event_type, priority, status, description,
                                              latitude, longitude)
                    SELECT ?, ?, ?, ?, 'new', ?, l.latitude, l.longitude
                    FROM (SELECT 1) AS one
                    LEFT JOIN LATERAL (
                        SELECT vl.latitude, vl.longitude FROM vehicle_locations vl
                        JOIN vehicles v ON v.vehicle_id = vl.vehicle_id
                        WHERE v.object_id = ? ORDER BY vl.recorded_at DESC LIMIT 1) l ON TRUE
                    RETURNING event_id
                    """, objectId, operatorId, sensor.code(), sensor.priority(), message, objectId);
            Jdbc.update(c, "UPDATE protected_objects SET security_status = 'alarm' WHERE object_id = ?", objectId);
            return id;
        });
    }

    public void setSecurityStatus(long objectId, SecurityStatus status, long userId) {
        jdbc.update("UPDATE protected_objects SET security_status = ? WHERE object_id = ?", status, objectId);
        audit.log(userId, status == SecurityStatus.ARMED ? "ARM" : "DISARM", "object", objectId, null);
    }

    public void logIgnition(long objectId, boolean blocked, long userId) {
        audit.log(userId, blocked ? "IGNITION_BLOCK" : "IGNITION_UNBLOCK", "object", objectId, null);
    }

    public void logMchs(long objectId, long userId) {
        audit.log(userId, "MCHS_CALL", "object", objectId, null);
    }

    public void markFalseAlarm(long eventId, long userId) {
        jdbc.inTransaction(c -> {
            Jdbc.update(c, "UPDATE alarm_events SET status = 'false_alarm' WHERE event_id = ?", eventId);
            Jdbc.update(c, """
                    UPDATE protected_objects SET security_status = 'armed'
                    WHERE object_id = (SELECT object_id FROM alarm_events WHERE event_id = ?)
                    """, eventId);
            return null;
        });
        audit.log(userId, "FALSE_ALARM", "alarm_event", eventId, null);
    }

    public List<AlarmEventView> findRecent(int limit) {
        return jdbc.query("SELECT e.event_id, e.object_id, " + LABEL + """
                        , e.event_type, e.priority, e.status, e.received_at, u.full_name,
                               d.dispatch_id, t.name, d.arrived_at, d.completed_at, d.result
                        FROM alarm_events e
                        JOIN protected_objects o ON o.object_id = e.object_id
                        LEFT JOIN vehicles v ON v.object_id = o.object_id
                        LEFT JOIN users u ON u.user_id = e.operator_id
                        LEFT JOIN dispatches d ON d.event_id = e.event_id
                        LEFT JOIN response_teams t ON t.team_id = d.team_id
                        ORDER BY e.received_at DESC, e.event_id DESC
                        LIMIT ?
                        """,
                rs -> new AlarmEventView(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getObject(7, OffsetDateTime.class), rs.getString(8),
                        Jdbc.getLong(rs, "dispatch_id"), rs.getString(10), rs.getObject(11, OffsetDateTime.class),
                        rs.getObject(12, OffsetDateTime.class), rs.getString(13)), limit);
    }
}
