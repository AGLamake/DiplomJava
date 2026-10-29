package ru.vlsu.security.model;

import java.util.List;

public record ResponseTeam(long id, String name, String phone, TeamStatus status, List<Long> memberUserIds,
                           String membersText) {
}
