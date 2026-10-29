package ru.vlsu.security.model;

import java.time.OffsetDateTime;

public record InstallationRequest(long id, long objectId, String objectName, long createdBy, String createdByName,
                                  Long assignedTo, String assignedToName, OffsetDateTime createdAt,
                                  OffsetDateTime scheduledAt, OffsetDateTime completedAt, RequestStatus status,
                                  String description) {
}
