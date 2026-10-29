package ru.vlsu.security.service;

import ru.vlsu.security.dao.UserDao;
import ru.vlsu.security.model.User;
import ru.vlsu.security.security.PasswordHasher;

/** Администрирование учётных записей: активация, смена роли, сброс пароля. */
public class UserAdminService {

    private final UserDao users;
    private final AuditService audit;

    public UserAdminService(UserDao users, AuditService audit) {
        this.users = users;
        this.audit = audit;
    }

    public void update(User actor, User changed) {
        if (actor.id() == changed.id() && (changed.role() != actor.role() || !changed.active())) {
            throw new IllegalArgumentException("Нельзя изменить собственную роль или заблокировать себя");
        }
        users.update(changed);
        audit.log(actor.id(), "USER_UPDATE", "user", changed.id(),
                "{\"role\": \"" + changed.role().code() + "\", \"active\": " + changed.active() + "}");
    }

    public void resetPassword(User actor, long userId, char[] newPassword) {
        if (newPassword == null || newPassword.length < AuthService.MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(AuthService.SHORT_PASSWORD);
        }
        users.updatePassword(userId, PasswordHasher.hash(newPassword));
        audit.log(actor.id(), "PASSWORD_RESET", "user", userId, null);
    }
}
