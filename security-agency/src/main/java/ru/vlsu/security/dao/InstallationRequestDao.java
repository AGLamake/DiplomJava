package ru.vlsu.security.dao;

import ru.vlsu.security.model.InstallationRequest;
import ru.vlsu.security.model.RequestStatus;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.util.List;

public class InstallationRequestDao implements CrudDao<InstallationRequest> {

    private final Jdbc jdbc;

    public InstallationRequestDao(DataSource ds) {
        this.jdbc = new Jdbc(ds);
    }

    @Override
    public List<InstallationRequest> findAll() {
        return jdbc.query("""
                SELECT r.request_id, r.object_id, '[' || o.object_id || '] ' || o.name, r.created_by, cu.full_name,
                       r.assigned_to, au.full_name, r.created_at, r.scheduled_at, r.completed_at, r.status,
                       r.description
                FROM installation_requests r
                JOIN protected_objects o ON o.object_id = r.object_id
                JOIN users cu ON cu.user_id = r.created_by
                LEFT JOIN users au ON au.user_id = r.assigned_to
                ORDER BY r.created_at DESC
                """, rs -> new InstallationRequest(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getLong(4),
                rs.getString(5), Jdbc.getLong(rs, "assigned_to"), rs.getString(7),
                rs.getObject(8, OffsetDateTime.class), rs.getObject(9, OffsetDateTime.class),
                rs.getObject(10, OffsetDateTime.class), RequestStatus.fromCode(rs.getString(11)), rs.getString(12)));
    }

    @Override
    public long insert(InstallationRequest r) {
        return jdbc.insert("""
                INSERT INTO installation_requests (object_id, created_by, assigned_to, scheduled_at, status, description,
                                                   completed_at)
                VALUES (?, ?, ?, ?, ?, ?, CASE WHEN ? = 'completed' THEN CURRENT_TIMESTAMP END)
                RETURNING request_id
                """, r.objectId(), r.createdBy(), r.assignedTo(), r.scheduledAt(), r.status(), r.description(),
                r.status());
    }

    /** При переводе в «Выполнена» completed_at заполняется автоматически. */
    @Override
    public void update(InstallationRequest r) {
        jdbc.update("""
                UPDATE installation_requests
                SET object_id = ?, assigned_to = ?, scheduled_at = ?, status = ?, description = ?,
                    completed_at = CASE WHEN ? = 'completed' THEN COALESCE(completed_at, CURRENT_TIMESTAMP) END
                WHERE request_id = ?
                """, r.objectId(), r.assignedTo(), r.scheduledAt(), r.status(), r.description(), r.status(), r.id());
    }

    @Override
    public void delete(long id) {
        jdbc.update("DELETE FROM installation_requests WHERE request_id = ?", id);
    }
}
