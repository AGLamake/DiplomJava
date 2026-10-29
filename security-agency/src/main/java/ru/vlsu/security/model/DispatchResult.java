package ru.vlsu.security.model;

public enum DispatchResult implements CodedEnum {
    CONFIRMED("confirmed", "Проникновение подтверждено"),
    FALSE_ALARM("false_alarm", "Ложная тревога"),
    NO_ACCESS("no_access", "Нет доступа"),
    OTHER("other", "Иное");

    private final String code;
    private final String title;

    DispatchResult(String code, String title) {
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

    public static DispatchResult fromCode(String code) {
        return CodedEnum.fromCode(DispatchResult.class, code);
    }

    @Override
    public String toString() {
        return title;
    }
}
