package ru.vlsu.security.model;

public enum ClientType implements CodedEnum {
    INDIVIDUAL("individual", "Физ. лицо"),
    LEGAL_ENTITY("legal_entity", "Юр. лицо");

    private final String code;
    private final String title;

    ClientType(String code, String title) {
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

    public static ClientType fromCode(String code) {
        return CodedEnum.fromCode(ClientType.class, code);
    }

    @Override
    public String toString() {
        return title;
    }
}
