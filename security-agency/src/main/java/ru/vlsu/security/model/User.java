package ru.vlsu.security.model;

import ru.vlsu.security.security.Role;

public record User(long id, String login, String fullName, Role role, boolean active, String phone, String email) {

    @Override
    public String toString() {
        return fullName + " (" + login + ")";
    }
}
