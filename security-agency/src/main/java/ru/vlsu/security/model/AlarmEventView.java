package ru.vlsu.security.model;

import java.time.OffsetDateTime;

/** Строка списка «Тревоги и выезды»: событие и (если есть) выезд ГБР по нему. */
public record AlarmEventView(long eventId, long objectId, String objectLabel, String eventType, String priority,
                             String status, OffsetDateTime receivedAt, String operatorName, Long dispatchId,
                             String teamName, OffsetDateTime arrivedAt, OffsetDateTime completedAt, String result) {
}
