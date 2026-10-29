package ru.vlsu.security.model;

public enum SecurityStatus implements CodedEnum {
    ARMED("armed", "Под охраной"),
    DISARMED("disarmed", "Снят с охраны"),
    ALARM("alarm", "Тревога"),
    UNKNOWN("unknown", "Неизвестно");

    private final String code;
    private final String title;

    SecurityStatus(String code, String title) {
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

    public static SecurityStatus fromCode(String code) {
        return CodedEnum.fromCode(SecurityStatus.class, code);
    }

    @Override
    public String toString() {
        return title;
    }
}
