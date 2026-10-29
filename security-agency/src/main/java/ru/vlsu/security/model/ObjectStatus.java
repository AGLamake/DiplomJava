package ru.vlsu.security.model;

public enum ObjectStatus implements CodedEnum {
    ACTIVE("active", "Активен"),
    SUSPENDED("suspended", "Приостановлен"),
    CLOSED("closed", "Закрыт");

    private final String code;
    private final String title;

    ObjectStatus(String code, String title) {
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

    public static ObjectStatus fromCode(String code) {
        return CodedEnum.fromCode(ObjectStatus.class, code);
    }

    @Override
    public String toString() {
        return title;
    }
}
