package ru.vlsu.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SmokeTest {

    @Test
    void javaRuntimeIs21() {
        assertTrue(Runtime.version().feature() >= 21);
    }

    @Test
    void appClassExists() throws ClassNotFoundException {
        Class.forName("ru.vlsu.security.Launcher");
    }
}
