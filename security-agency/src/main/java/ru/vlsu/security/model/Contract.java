package ru.vlsu.security.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record Contract(long id, long clientId, String clientName, String number, LocalDate signedOn,
                       LocalDate validFrom, LocalDate validTo, ContractStatus status, BigDecimal price,
                       List<Long> objectIds) {
}
