package ru.vlsu.security.service;

import ru.vlsu.security.model.Contract;

import java.util.Set;

import static ru.vlsu.security.validation.FormParsers.requireChoice;
import static ru.vlsu.security.validation.FormParsers.requireText;

/** Проверка договора перед сохранением. */
public final class ContractValidator {

    private ContractValidator() {
    }

    public static void validate(Contract c, Set<Long> clientObjectIds) {
        requireText(c.number(), "Номер договора");
        requireChoice(c.signedOn(), "Дата подписания");
        requireChoice(c.validFrom(), "Действует с");
        requireChoice(c.status(), "Статус");
        if (c.validTo() != null && c.validTo().isBefore(c.validFrom())) {
            throw new IllegalArgumentException("Дата окончания не может быть раньше даты начала");
        }
        if (c.objectIds() == null || c.objectIds().isEmpty()) {
            throw new IllegalArgumentException("Выберите хотя бы один объект");
        }
        if (!clientObjectIds.containsAll(c.objectIds())) {
            throw new IllegalArgumentException("Объект не принадлежит выбранному клиенту");
        }
    }
}
