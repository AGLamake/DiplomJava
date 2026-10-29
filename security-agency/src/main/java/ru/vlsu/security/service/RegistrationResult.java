package ru.vlsu.security.service;

import ru.vlsu.security.model.User;

public record RegistrationResult(User user, String message) {
}
