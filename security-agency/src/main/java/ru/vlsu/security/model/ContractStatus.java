package ru.vlsu.security.model;

public enum ContractStatus implements CodedEnum {
    DRAFT("draft", "Черновик"),
    ACTIVE("active", "Действует"),
    EXPIRED("expired", "Истёк"),
    TERMINATED("terminated", "Расторгнут");

    private final String code;
    private final String title;

    ContractStatus(String code, String title) {
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

    public static ContractStatus fromCode(String code) {
        return CodedEnum.fromCode(ContractStatus.class, code);
    }

    @Override
    public String toString() {
        return title;
    }
}
