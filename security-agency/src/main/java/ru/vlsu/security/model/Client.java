package ru.vlsu.security.model;

public record Client(long id, ClientType type, String name, String contactPerson, String phone, String email) {

    @Override
    public String toString() {
        return name;
    }
}
