package ru.vlsu.security.model;

public record EquipmentType(long id, String name, String category) {

    @Override
    public String toString() {
        return name;
    }
}
