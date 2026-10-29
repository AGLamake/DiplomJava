package ru.vlsu.security.dao;

import ru.vlsu.security.model.ResponseTeam;
import ru.vlsu.security.model.TeamStatus;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/** Группы быстрого реагирования и их состав (team_members). */
public class ResponseTeamDao implements CrudDao<ResponseTeam> {

    private final Jdbc jdbc;

    public ResponseTeamDao(DataSource ds) {
        this.jdbc = new Jdbc(ds);
    }

    @Override
    public List<ResponseTeam> findAll() {
        return jdbc.query("""
                SELECT t.team_id, t.name, t.phone, t.status,
                       ARRAY(SELECT m.user_id FROM team_members m JOIN users u ON u.user_id = m.user_id
                             WHERE m.team_id = t.team_id ORDER BY u.full_name),
                       (SELECT string_agg(u.full_name, ', ' ORDER BY u.full_name)
                        FROM team_members m JOIN users u ON u.user_id = m.user_id WHERE m.team_id = t.team_id)
                FROM response_teams t ORDER BY t.team_id
                """, rs -> new ResponseTeam(rs.getLong(1), rs.getString(2), rs.getString(3),
                TeamStatus.fromCode(rs.getString(4)), Arrays.stream((Long[]) rs.getArray(5).getArray()).toList(),
                rs.getString(6)));
    }

    @Override
    public long insert(ResponseTeam t) {
        return jdbc.inTransaction(c -> {
            long id = Jdbc.insert(c, "INSERT INTO response_teams (name, phone, status) VALUES (?, ?, ?) RETURNING team_id",
                    t.name(), t.phone(), t.status());
            writeMembers(c, id, t.memberUserIds());
            return id;
        });
    }

    @Override
    public void update(ResponseTeam t) {
        jdbc.inTransaction(c -> {
            Jdbc.update(c, "UPDATE response_teams SET name = ?, phone = ?, status = ? WHERE team_id = ?",
                    t.name(), t.phone(), t.status(), t.id());
            Jdbc.update(c, "DELETE FROM team_members WHERE team_id = ?", t.id());
            writeMembers(c, t.id(), t.memberUserIds());
            return null;
        });
    }

    @Override
    public void delete(long id) {
        jdbc.update("DELETE FROM response_teams WHERE team_id = ?", id);
    }

    private static void writeMembers(Connection c, long teamId, List<Long> userIds) throws SQLException {
        for (Long userId : userIds) {
            Jdbc.update(c, "INSERT INTO team_members (team_id, user_id) VALUES (?, ?)", teamId, userId);
        }
    }
}
