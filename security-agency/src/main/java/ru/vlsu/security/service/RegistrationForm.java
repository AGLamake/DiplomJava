package ru.vlsu.security.service;

public record RegistrationForm(String login, char[] password, char[] passwordRepeat, String fullName) {
}
