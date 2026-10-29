package ru.vlsu.security.model;

import java.time.OffsetDateTime;

/** Незавершённый выезд для экрана «Мои выезды». */
public record DispatchView(long dispatchId, long eventId, String objectLabel, String eventType, String teamName,
                           OffsetDateTime dispatchedAt, OffsetDateTime arrivedAt) {
}
