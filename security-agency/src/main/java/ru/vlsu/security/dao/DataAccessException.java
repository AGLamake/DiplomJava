package ru.vlsu.security.dao;

import java.sql.SQLException;

/** Ошибка доступа к БД; sqlState позволяет показать пользователю понятное сообщение. */
public class DataAccessException extends RuntimeException {

    private final String sqlState;

    public DataAccessException(String message, SQLException cause) {
        super(message, cause);
        this.sqlState = cause == null ? null : cause.getSQLState();
    }

    public String sqlState() {
        return sqlState;
    }
}
