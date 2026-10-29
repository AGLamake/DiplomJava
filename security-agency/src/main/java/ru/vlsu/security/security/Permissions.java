package ru.vlsu.security.security;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static ru.vlsu.security.security.Access.NONE;
import static ru.vlsu.security.security.Access.READ;
import static ru.vlsu.security.security.Access.WRITE;

/** Матрица прав: какая роль что может делать в каждом разделе. */
public final class Permissions {

    private static final Map<Role, EnumMap<Section, Access>> MATRIX = new EnumMap<>(Role.class);

    static {
        // Порядок: administrator, manager, engineer, operator, response_team, client
        row(Section.CAR_MONITOR,      WRITE, NONE,  NONE,  WRITE, NONE,  NONE);
        row(Section.NDV_MONITOR,      WRITE, NONE,  NONE,  WRITE, NONE,  NONE);
        row(Section.ALARMS,           WRITE, READ,  NONE,  WRITE, READ,  NONE);
        row(Section.CLIENTS,          WRITE, WRITE, READ,  READ,  NONE,  NONE);
        row(Section.REAL_ESTATE,      WRITE, WRITE, READ,  READ,  NONE,  NONE);
        row(Section.VEHICLES,         WRITE, WRITE, READ,  READ,  NONE,  NONE);
        row(Section.CONTRACTS,        WRITE, WRITE, NONE,  READ,  NONE,  NONE);
        row(Section.TEAMS,            WRITE, READ,  NONE,  READ,  READ,  NONE);
        row(Section.EQUIPMENT,        WRITE, READ,  WRITE, READ,  NONE,  NONE);
        row(Section.INSTALL_REQUESTS, WRITE, WRITE, WRITE, NONE,  NONE,  NONE);
        row(Section.MY_DISPATCHES,    NONE,  NONE,  NONE,  NONE,  WRITE, NONE);
        row(Section.USERS,            WRITE, NONE,  NONE,  NONE,  NONE,  NONE);
        row(Section.AUDIT,            READ,  NONE,  NONE,  NONE,  NONE,  NONE);
    }

    private Permissions() {
    }

    private static void row(Section section, Access... byRole) {
        Role[] roles = Role.values();
        for (int i = 0; i < roles.length; i++) {
            MATRIX.computeIfAbsent(roles[i], r -> new EnumMap<>(Section.class)).put(section, byRole[i]);
        }
    }

    public static Access access(Role role, Section section) {
        return MATRIX.get(role).getOrDefault(section, NONE);
    }

    public static List<Section> visibleSections(Role role) {
        List<Section> result = new ArrayList<>();
        for (Section s : Section.values()) {
            if (access(role, s).canRead()) {
                result.add(s);
            }
        }
        return result;
    }
}
