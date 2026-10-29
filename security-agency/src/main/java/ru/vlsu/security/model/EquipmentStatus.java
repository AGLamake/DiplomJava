package ru.vlsu.security.model;

public enum EquipmentStatus implements CodedEnum {
    PLANNED("planned", "Запланировано"),
    INSTALLED("installed", "Установлено"),
    FAULT("fault", "Неисправно"),
    REMOVED("removed", "Демонтировано");

    private final String code;
    private final String title;

    EquipmentStatus(String code, String title) {
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

    public static EquipmentStatus fromCode(String code) {
        return CodedEnum.fromCode(EquipmentStatus.class, code);
    }

    @Override
    public String toString() {
        return title;
    }
}
