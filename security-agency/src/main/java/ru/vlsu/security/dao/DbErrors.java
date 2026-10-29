package ru.vlsu.security.dao;

/** Переводит коды ошибок PostgreSQL в понятные пользователю сообщения. */
public final class DbErrors {

    private DbErrors() {
    }

    public static String message(DataAccessException e) {
        String state = e.sqlState() == null ? "" : e.sqlState();
        return switch (state) {
            case "23503" -> "Запись используется в других таблицах и не может быть удалена";
            case "23505" -> "Запись с такими данными уже существует";
            case "23514" -> "Данные не прошли проверку";
            case "23502" -> "Не заполнено обязательное поле";
            default -> "Ошибка базы данных: " + e.getMessage();
        };
    }
}
