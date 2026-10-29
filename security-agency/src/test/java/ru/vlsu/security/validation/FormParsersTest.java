package ru.vlsu.security.validation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormParsersTest {

    @Test
    void price_withSpacesAndComma() {
        assertEquals(new BigDecimal("1500.50"), FormParsers.price("1 500,50"));
    }

    @Test
    void price_withDot() {
        assertEquals(new BigDecimal("1500.5"), FormParsers.price("1500.5"));
    }

    @Test
    void price_empty_isNull() {
        assertNull(FormParsers.price(""));
        assertNull(FormParsers.price(null));
    }

    @Test
    void price_notNumber() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> FormParsers.price("abc"));
        assertEquals("Цена должна быть числом", e.getMessage());
    }

    @Test
    void price_negative() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> FormParsers.price("-5"));
        assertEquals("Цена не может быть отрицательной", e.getMessage());
    }

    @Test
    void requireText_blank() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> FormParsers.requireText("  ", "Имя"));
        assertEquals("Поле «Имя» обязательно для заполнения", e.getMessage());
    }

    @Test
    void requireText_trims() {
        assertEquals("Иван", FormParsers.requireText("  Иван ", "Имя"));
    }

    @Test
    void optionalText_blankToNull() {
        assertNull(FormParsers.optionalText("   "));
        assertEquals("a", FormParsers.optionalText(" a "));
    }
}
