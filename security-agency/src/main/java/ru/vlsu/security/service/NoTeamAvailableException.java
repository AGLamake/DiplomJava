package ru.vlsu.security.service;

public class NoTeamAvailableException extends Exception {

    public NoTeamAvailableException() {
        super("Нет свободных групп быстрого реагирования");
    }
}
