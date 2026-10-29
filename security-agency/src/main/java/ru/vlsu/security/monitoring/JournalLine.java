package ru.vlsu.security.monitoring;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Строка журнала событий: «dd.MM.yyyy HH:mm:ss : сообщение». */
public final class JournalLine {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private JournalLine() {
    }

    public static String format(LocalDateTime time, String message) {
        return time.format(FORMAT) + " : " + message;
    }
}
