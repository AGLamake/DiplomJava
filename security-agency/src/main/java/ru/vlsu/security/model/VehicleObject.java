package ru.vlsu.security.model;

/** Автомобиль: строка protected_objects (object_type = 'vehicle') + vehicles. */
public record VehicleObject(long objectId, long clientId, String clientName, String brand, String model,
                            String regNumber, String vin, ObjectStatus objectStatus,
                            SecurityStatus securityStatus) {
}
