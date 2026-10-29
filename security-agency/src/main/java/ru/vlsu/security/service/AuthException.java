package ru.vlsu.security.service;

/** Ошибка входа или регистрации; сообщение показывается пользователю как есть. */
public class AuthException extends Exception {

    public AuthException(String message) {
        super(message);
    }
}
