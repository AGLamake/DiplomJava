package ru.vlsu.security.model;

import ru.vlsu.security.monitoring.ObjectKind;

/** Объект в списке экрана мониторинга. */
public record MonitoredObject(long objectId, ObjectKind kind, String label, String ownerName,
                              SecurityStatus securityStatus) {

    @Override
    public String toString() {
        return label;
    }
}
