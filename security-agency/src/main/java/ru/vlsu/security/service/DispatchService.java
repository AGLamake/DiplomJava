package ru.vlsu.security.service;

import ru.vlsu.security.dao.Jdbc;
import ru.vlsu.security.model.DispatchResult;
import ru.vlsu.security.model.DispatchView;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** Выезды групп быстрого реагирования. */
public class DispatchService {

    private final Jdbc jdbc;
    private final AuditService audit;

    public DispatchService(DataSource ds, AuditService audit) {
        this.jdbc = new Jdbc(ds);
        this.audit = audit;
    }

    /**
     * Отправляет первую свободную группу. Если события нет (ручной вызов) или по нему уже был выезд,
     * создаётся событие «manual_dispatch».
     */
    public long dispatch(long objectId, Long eventId, long operatorId) throws NoTeamAvailableException {
        Long dispatchId = jdbc.inTransaction(c -> {
            List<Long> team = Jdbc.query(c, """
                    SELECT team_id FROM response_teams WHERE status = 'available'
                    ORDER BY team_id LIMIT 1 FOR UPDATE SKIP LOCKED
                    """, rs -> rs.getLong(1));
            if (team.isEmpty()) {
                return null;
            }
            Long event = eventId;
            if (event != null && !Jdbc.query(c, "SELECT 1 FROM dispatches WHERE event_id = ?", rs -> 1, event).isEmpty()) {
                event = null;
            }
            if (event == null) {
                event = Jdbc.insert(c, """
                        INSERT INTO alarm_events (object_id, operator_id, event_type, priority, status, description)
                        VALUES (?, ?, 'manual_dispatch', 'high', 'new', 'Ручной вызов ГБР оператором')
                        RETURNING event_id
                        """, objectId, operatorId);
            }
            long id = Jdbc.insert(c, """
                    INSERT INTO dispatches (event_id, team_id, operator_id) VALUES (?, ?, ?) RETURNING dispatch_id
                    """, event, team.get(0), operatorId);
            Jdbc.update(c, "UPDATE response_teams SET status = 'dispatched' WHERE team_id = ?", team.get(0));
            Jdbc.update(c, "UPDATE alarm_events SET status = 'dispatching' WHERE event_id = ?", event);
            return id;
        });
        if (dispatchId == null) {
            throw new NoTeamAvailableException();
        }
        audit.log(operatorId, "DISPATCH", "dispatch", dispatchId, null);
        return dispatchId;
    }

    public void markArrived(long dispatchId) {
        jdbc.update("UPDATE dispatches SET arrived_at = CURRENT_TIMESTAMP WHERE dispatch_id = ? AND arrived_at IS NULL",
                dispatchId);
    }

    public void complete(long dispatchId, DispatchResult result, String notes, long userId) {
        String eventStatus = switch (result) {
            case CONFIRMED -> "confirmed";
            case FALSE_ALARM -> "false_alarm";
            case NO_ACCESS, OTHER -> "closed";
        };
        jdbc.inTransaction(c -> {
            Jdbc.update(c, """
                    UPDATE dispatches SET arrived_at = COALESCE(arrived_at, CURRENT_TIMESTAMP),
                           completed_at = CURRENT_TIMESTAMP, result = ?, notes = ?
                    WHERE dispatch_id = ?
                    """, result, notes, dispatchId);
            Jdbc.update(c, """
                    UPDATE response_teams SET status = 'available'
                    WHERE team_id = (SELECT team_id FROM dispatches WHERE dispatch_id = ?)
                    """, dispatchId);
            Jdbc.update(c, """
                    UPDATE alarm_events SET status = ?
                    WHERE event_id = (SELECT event_id FROM dispatches WHERE dispatch_id = ?)
                    """, eventStatus, dispatchId);
            Jdbc.update(c, """
                    UPDATE protected_objects SET security_status = 'armed'
                    WHERE object_id = (SELECT e.object_id FROM dispatches d JOIN alarm_events e ON e.event_id = d.event_id
                                       WHERE d.dispatch_id = ?)
                    """, dispatchId);
            return null;
        });
        audit.log(userId, "DISPATCH_COMPLETE", "dispatch", dispatchId, "{\"result\": \"" + result.code() + "\"}");
    }

    public List<DispatchView> findActiveForMember(long userId) {
        return jdbc.query("SELECT d.dispatch_id, d.event_id, " + AlarmService.LABEL + """
                        , e.event_type, t.name, d.dispatched_at, d.arrived_at
                        FROM dispatches d
                        JOIN alarm_events e ON e.event_id = d.event_id
                        JOIN protected_objects o ON o.object_id = e.object_id
                        LEFT JOIN vehicles v ON v.object_id = o.object_id
                        JOIN response_teams t ON t.team_id = d.team_id
                        JOIN team_members m ON m.team_id = d.team_id AND m.user_id = ?
                        WHERE d.completed_at IS NULL
                        ORDER BY d.dispatched_at
                        """,
                rs -> new DispatchView(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getObject(6, OffsetDateTime.class), rs.getObject(7, OffsetDateTime.class)),
                userId);
    }

    public Optional<Long> activeDispatchForObject(long objectId) {
        return jdbc.query("""
                SELECT d.dispatch_id FROM dispatches d JOIN alarm_events e ON e.event_id = d.event_id
                WHERE e.object_id = ? AND d.completed_at IS NULL
                ORDER BY d.dispatched_at DESC LIMIT 1
                """, rs -> rs.getLong(1), objectId).stream().findFirst();
    }
}
