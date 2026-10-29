package ru.vlsu.security.model;

import java.time.LocalDate;

public record Equipment(long id, long objectId, String objectName, long typeId, String typeName, String serialNumber,
                        LocalDate installationDate, EquipmentStatus status) {
}
