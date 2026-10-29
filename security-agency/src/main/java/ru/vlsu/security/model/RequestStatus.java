package ru.vlsu.security.model;

public enum RequestStatus implements CodedEnum {
    NEW("new", "Новая"),
    ASSIGNED("assigned", "Назначена"),
    IN_PROGRESS("in_progress", "В работе"),
    COMPLETED("completed", "Выполнена"),
    CANCELLED("cancelled", "Отменена");

    private final String code;
    private final String title;

    RequestStatus(String code, String title) {
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

    public static RequestStatus fromCode(String code) {
        return CodedEnum.fromCode(RequestStatus.class, code);
    }

    @Override
    public String toString() {
        return title;
    }
}
