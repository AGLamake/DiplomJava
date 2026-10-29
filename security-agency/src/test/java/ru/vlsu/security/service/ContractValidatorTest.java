package ru.vlsu.security.service;

import org.junit.jupiter.api.Test;
import ru.vlsu.security.model.Contract;
import ru.vlsu.security.model.ContractStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContractValidatorTest {

    private static final Set<Long> OWN = Set.of(1L, 5L);

    private static Contract c(String number, LocalDate from, LocalDate to, List<Long> objects) {
        return new Contract(0, 2, null, number, from, from, to, ContractStatus.ACTIVE, null, objects);
    }

    private static String error(Contract c) {
        return assertThrows(IllegalArgumentException.class, () -> ContractValidator.validate(c, OWN)).getMessage();
    }

    @Test
    void validate_validToBeforeValidFrom() {
        assertEquals("Дата окончания не может быть раньше даты начала",
                error(c("Д-1", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 4, 30), List.of(1L))));
    }

    @Test
    void validate_noObjects() {
        assertEquals("Выберите хотя бы один объект", error(c("Д-1", LocalDate.of(2026, 5, 1), null, List.of())));
    }

    @Test
    void validate_foreignObject() {
        assertEquals("Объект не принадлежит выбранному клиенту",
                error(c("Д-1", LocalDate.of(2026, 5, 1), null, List.of(2L))));
    }

    @Test
    void validate_blankNumber() {
        assertEquals("Поле «Номер договора» обязательно для заполнения",
                error(c("  ", LocalDate.of(2026, 5, 1), null, List.of(1L))));
    }

    @Test
    void validate_missingDates() {
        assertEquals("Поле «Дата подписания» обязательно для заполнения",
                error(new Contract(0, 2, null, "Д-1", null, LocalDate.now(), null, ContractStatus.ACTIVE, null,
                        List.of(1L))));
    }

    @Test
    void validate_ok() {
        assertDoesNotThrow(() -> ContractValidator.validate(
                c("Д-1", LocalDate.of(2026, 5, 1), LocalDate.of(2027, 5, 1), List.of(1L, 5L)), OWN));
    }
}
