package ru.vlsu.security.model;

public enum TeamStatus implements CodedEnum {
    AVAILABLE("available", "Свободна"),
    DISPATCHED("dispatched", "На выезде"),
    UNAVAILABLE("unavailable", "Недоступна");

    private final String code;
    private final String title;

    TeamStatus(String code, String title) {
        this.code = code;
        this.title = title;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String title() {
        return title;
    }

    public static TeamStatus fromCode(String code) {
        return CodedEnum.fromCode(TeamStatus.class, code);
    }

    @Override
    public String toString() {
        return title;
    }
}
