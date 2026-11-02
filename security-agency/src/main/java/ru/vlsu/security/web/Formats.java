package ru.vlsu.security.web;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Форматы дат в таблицах. */
final class Formats {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private Formats() {
    }

    static String date(LocalDate d) {
        return d == null ? null : d.format(DATE);
    }

    static String dateTime(OffsetDateTime t) {
        return t == null ? null : t.atZoneSameInstant(ZoneId.systemDefault()).format(DATE_TIME);
    }
}
