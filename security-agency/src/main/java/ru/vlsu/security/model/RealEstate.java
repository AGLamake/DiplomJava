package ru.vlsu.security.model;

public record RealEstate(long id, long clientId, String clientName, String name, String address,
                         ObjectStatus objectStatus, SecurityStatus securityStatus) {
}
