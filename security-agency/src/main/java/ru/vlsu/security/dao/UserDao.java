package ru.vlsu.security.dao;

import ru.vlsu.security.model.User;
import ru.vlsu.security.security.Role;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {

    private static final String SELECT = """
            SELECT u.user_id, u.login, u.full_name, r.name AS role, u.is_active, u.phone, u.email
            FROM users u JOIN roles r ON r.role_id = u.role_id
            """;

    private final DataSource ds;

    public UserDao(DataSource ds) {
        this.ds = ds;
    }

    public Optional<User> findByLogin(String login) {
        List<User> found = query(SELECT + " WHERE u.login = ?", login);
        return found.stream().findFirst();
    }

    public Optional<User> findById(long id) {
        return query(SELECT + " WHERE u.user_id = ?", id).stream().findFirst();
    }

    public Optional<String> findPasswordHash(String login) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT password_hash FROM users WHERE login = ?")) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось прочитать пароль пользователя", e);
        }
    }

    public long insert(String login, String passwordHash, String fullName, Role role, boolean active) {
        String sql = """
                INSERT INTO users (role_id, login, password_hash, full_name, is_active)
                VALUES ((SELECT role_id FROM roles WHERE name = ?), ?, ?, ?, ?)
                RETURNING user_id
                """;
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, role.code());
            ps.setString(2, login);
            ps.setString(3, passwordHash);
            ps.setString(4, fullName);
            ps.setBoolean(5, active);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось добавить пользователя", e);
        }
    }

    public long count() {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT count(*) FROM users");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось посчитать пользователей", e);
        }
    }

    public List<User> findAll() {
        return query(SELECT + " ORDER BY u.login");
    }

    public List<User> findByRole(Role role) {
        return query(SELECT + " WHERE r.name = ? AND u.is_active ORDER BY u.full_name", role.code());
    }

    public void update(User user) {
        String sql = """
                UPDATE users SET full_name = ?, phone = ?, email = ?, is_active = ?,
                       role_id = (SELECT role_id FROM roles WHERE name = ?)
                WHERE user_id = ?
                """;
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, user.fullName());
            ps.setString(2, user.phone());
            ps.setString(3, user.email());
            ps.setBoolean(4, user.active());
            ps.setString(5, user.role().code());
            ps.setLong(6, user.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось изменить пользователя", e);
        }
    }

    public void updatePassword(long id, String hash) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE users SET password_hash = ? WHERE user_id = ?")) {
            ps.setString(1, hash);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось изменить пароль", e);
        }
    }

    private List<User> query(String sql, Object... params) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            List<User> result = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new User(
                            rs.getLong("user_id"),
                            rs.getString("login"),
                            rs.getString("full_name"),
                            Role.fromCode(rs.getString("role")),
                            rs.getBoolean("is_active"),
                            rs.getString("phone"),
                            rs.getString("email")));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось прочитать пользователей", e);
        }
    }
}
