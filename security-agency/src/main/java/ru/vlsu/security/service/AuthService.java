package ru.vlsu.security.service;

import ru.vlsu.security.dao.UserDao;
import ru.vlsu.security.model.User;
import ru.vlsu.security.security.PasswordHasher;
import ru.vlsu.security.security.Role;

import java.util.Arrays;
import java.util.Optional;

/** Вход и регистрация пользователей. */
public class AuthService {

    static final int MIN_PASSWORD_LENGTH = 6;
    static final String SHORT_PASSWORD = "Пароль должен содержать не менее 6 символов";

    private static final String FILL_ALL = "Заполните все поля!";
    private static final String BAD_CREDENTIALS = "Неверные данные для входа!";

    private final UserDao users;
    private final AuditService audit;

    public AuthService(UserDao users, AuditService audit) {
        this.users = users;
        this.audit = audit;
    }

    public User login(String login, char[] password) throws AuthException {
        if (isBlank(login) || password == null || password.length == 0) {
            throw new AuthException(FILL_ALL);
        }
        String trimmed = login.trim();
        Optional<String> hash = users.findPasswordHash(trimmed);
        if (hash.isEmpty() || !PasswordHasher.verify(password, hash.get())) {
            throw new AuthException(BAD_CREDENTIALS);
        }
        User user = users.findByLogin(trimmed).orElseThrow(() -> new AuthException(BAD_CREDENTIALS));
        if (!user.active()) {
            throw new AuthException("Учётная запись ожидает активации администратором");
        }
        if (user.role() == Role.CLIENT) {
            throw new AuthException("Для роли «Клиент» вход в приложение не предусмотрен");
        }
        audit.log(user.id(), "LOGIN", "user", user.id(), null);
        return user;
    }

    public RegistrationResult register(RegistrationForm form) throws AuthException {
        if (isBlank(form.login()) || isBlank(form.fullName())
                || form.password() == null || form.password().length == 0
                || form.passwordRepeat() == null || form.passwordRepeat().length == 0) {
            throw new AuthException(FILL_ALL);
        }
        if (form.password().length < MIN_PASSWORD_LENGTH) {
            throw new AuthException(SHORT_PASSWORD);
        }
        if (!Arrays.equals(form.password(), form.passwordRepeat())) {
            throw new AuthException("Пароли отличаются!");
        }
        String login = form.login().trim();
        if (users.findByLogin(login).isPresent()) {
            throw new AuthException("Пользователь с таким логином уже существует");
        }

        boolean first = users.count() == 0;
        Role role = first ? Role.ADMINISTRATOR : Role.OPERATOR;
        long id = users.insert(login, PasswordHasher.hash(form.password()), form.fullName().trim(), role, first);
        User user = users.findById(id).orElseThrow();
        audit.log(id, "REGISTER", "user", id, null);
        String message = first
                ? "Учётная запись администратора создана. Выполните вход."
                : "Заявка на регистрацию отправлена администратору.";
        return new RegistrationResult(user, message);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
