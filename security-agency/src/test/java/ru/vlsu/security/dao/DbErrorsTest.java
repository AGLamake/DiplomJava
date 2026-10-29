package ru.vlsu.security.dao;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DbErrorsTest {

    private static DataAccessException ex(String state) {
        return new DataAccessException("Сбой", new SQLException("detail text", state));
    }

    @Test
    void foreignKey() {
        assertEquals("Запись используется в других таблицах и не может быть удалена", DbErrors.message(ex("23503")));
    }

    @Test
    void unique() {
        assertEquals("Запись с такими данными уже существует", DbErrors.message(ex("23505")));
    }

    @Test
    void check() {
        assertEquals("Данные не прошли проверку", DbErrors.message(ex("23514")));
    }

    @Test
    void notNull() {
        assertEquals("Не заполнено обязательное поле", DbErrors.message(ex("23502")));
    }

    @Test
    void unknown() {
        assertEquals("Ошибка базы данных: Сбой", DbErrors.message(ex("08006")));
    }
}
