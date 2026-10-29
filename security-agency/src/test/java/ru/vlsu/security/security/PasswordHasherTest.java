package ru.vlsu.security.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void hash_thenVerify_true() {
        String stored = PasswordHasher.hash("123456".toCharArray());
        assertTrue(PasswordHasher.verify("123456".toCharArray(), stored));
    }

    @Test
    void verify_wrongPassword_false() {
        String stored = PasswordHasher.hash("123456".toCharArray());
        assertFalse(PasswordHasher.verify("1234567".toCharArray(), stored));
    }

    @Test
    void hash_sameInput_differentOutputs() {
        assertNotEquals(PasswordHasher.hash("123456".toCharArray()), PasswordHasher.hash("123456".toCharArray()));
    }

    @Test
    void hash_format() {
        assertTrue(PasswordHasher.hash("пароль".toCharArray())
                .matches("pbkdf2\\$210000\\$[A-Za-z0-9+/=]+\\$[A-Za-z0-9+/=]+"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "pbkdf2$x$y$z", "pbkdf2$1000$!!!$???"})
    void verify_malformedStored_false(String stored) {
        assertFalse(PasswordHasher.verify("123456".toCharArray(), stored));
    }
}
