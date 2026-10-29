package ru.vlsu.security.service;

import ru.vlsu.security.dao.DataAccessException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** Журнал действий пользователей (таблица audit_log). */
public class AuditService {

    public record AuditEntry(long id, String userLogin, String action, String entityType, Long entityId,
                             OffsetDateTime createdAt, String details) {
    }

    private final DataSource ds;

    public AuditService(DataSource ds) {
        this.ds = ds;
    }

    public void log(Long userId, String action, String entityType, Long entityId, String detailsJson) {
        String sql = "INSERT INTO audit_log (user_id, action, entity_type, entity_id, details) VALUES (?, ?, ?, ?, ?::jsonb)";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            setLong(ps, 1, userId);
            ps.setString(2, action);
            ps.setString(3, entityType);
            setLong(ps, 4, entityId);
            ps.setString(5, detailsJson == null ? "{}" : detailsJson);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось записать журнал действий", e);
        }
    }

    public List<AuditEntry> findRecent(int limit) {
        String sql = """
                SELECT a.audit_id, u.login, a.action, a.entity_type, a.entity_id, a.created_at, a.details::text
                FROM audit_log a LEFT JOIN users u ON u.user_id = a.user_id
                ORDER BY a.created_at DESC, a.audit_id DESC
                LIMIT ?
                """;
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            List<AuditEntry> result = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long entityId = rs.getLong(5);
                    result.add(new AuditEntry(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            rs.wasNull() ? null : entityId, rs.getObject(6, OffsetDateTime.class), rs.getString(7)));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось прочитать журнал действий", e);
        }
    }

    static void setLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }
}
