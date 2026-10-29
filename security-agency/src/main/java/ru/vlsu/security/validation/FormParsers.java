package ru.vlsu.security.validation;

import java.math.BigDecimal;

/** Разбор и проверка ввода в формах; сообщения исключений показываются пользователю. */
public final class FormParsers {

    private FormParsers() {
    }

    public static BigDecimal price(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = text.replaceAll("\\s", "").replace(',', '.');
        BigDecimal value;
        try {
            value = new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Цена должна быть числом");
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException("Цена не может быть отрицательной");
        }
        return value;
    }

    public static String requireText(String value, String fieldTitle) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Поле «" + fieldTitle + "» обязательно для заполнения");
        }
        return value.trim();
    }

    public static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static <T> T requireChoice(T value, String fieldTitle) {
        if (value == null) {
            throw new IllegalArgumentException("Поле «" + fieldTitle + "» обязательно для заполнения");
        }
        return value;
    }
}
