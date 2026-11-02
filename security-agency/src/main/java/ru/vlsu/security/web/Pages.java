package ru.vlsu.security.web;

import ru.vlsu.security.security.Permissions;
import ru.vlsu.security.security.Role;
import ru.vlsu.security.security.Section;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Страницы разделов главного меню. */
public final class Pages {

    public static final String LOGIN = "/login.xhtml";

    private static final Map<Section, String> PATHS = new EnumMap<>(Section.class);

    static {
        PATHS.put(Section.CAR_MONITOR, "/car.xhtml");
        PATHS.put(Section.NDV_MONITOR, "/ndv.xhtml");
        PATHS.put(Section.ALARMS, "/alarms.xhtml");
        PATHS.put(Section.CLIENTS, "/clients.xhtml");
        PATHS.put(Section.REAL_ESTATE, "/real-estate.xhtml");
        PATHS.put(Section.VEHICLES, "/vehicles.xhtml");
        PATHS.put(Section.CONTRACTS, "/contracts.xhtml");
        PATHS.put(Section.TEAMS, "/teams.xhtml");
        PATHS.put(Section.EQUIPMENT, "/equipment.xhtml");
        PATHS.put(Section.INSTALL_REQUESTS, "/install-requests.xhtml");
        PATHS.put(Section.MY_DISPATCHES, "/my-dispatches.xhtml");
        PATHS.put(Section.USERS, "/users.xhtml");
        PATHS.put(Section.AUDIT, "/audit.xhtml");
    }

    private Pages() {
    }

    public static String path(Section section) {
        return PATHS.get(section);
    }

    public static Optional<Section> section(String path) {
        return PATHS.entrySet().stream().filter(e -> e.getValue().equals(path)).map(Map.Entry::getKey).findFirst();
    }

    /** Первый доступный роли раздел — с него начинается работа после входа. */
    public static String home(Role role) {
        List<Section> sections = Permissions.visibleSections(role);
        return sections.isEmpty() ? LOGIN : path(sections.get(0));
    }
}
