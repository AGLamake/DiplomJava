package ru.vlsu.security.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionsTest {

    @Test
    void operator_canWriteMonitoring() {
        assertEquals(Access.WRITE, Permissions.access(Role.OPERATOR, Section.CAR_MONITOR));
    }

    @Test
    void operator_readsClients() {
        assertEquals(Access.READ, Permissions.access(Role.OPERATOR, Section.CLIENTS));
    }

    @Test
    void engineer_writesEquipment_noContracts() {
        assertEquals(Access.WRITE, Permissions.access(Role.ENGINEER, Section.EQUIPMENT));
        assertEquals(Access.NONE, Permissions.access(Role.ENGINEER, Section.CONTRACTS));
    }

    @Test
    void client_hasNoSections() {
        assertTrue(Permissions.visibleSections(Role.CLIENT).isEmpty());
    }

    @Test
    void onlyAdminSeesUsers() {
        for (Role r : Role.values()) {
            if (r != Role.ADMINISTRATOR) {
                assertEquals(Access.NONE, Permissions.access(r, Section.USERS), r.name());
            }
        }
    }

    @Test
    void roleCodes_matchRolesTable() {
        assertEquals(Role.RESPONSE_TEAM, Role.fromCode("response_team"));
        assertEquals("administrator", Role.ADMINISTRATOR.code());
        assertEquals("Сотрудник ГБР", Role.RESPONSE_TEAM.title());
    }

    // Порядок колонок: administrator, manager, engineer, operator, response_team, client
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "CAR_MONITOR,      W, -, -, W, -, -",
            "NDV_MONITOR,      W, -, -, W, -, -",
            "ALARMS,           W, R, -, W, R, -",
            "CLIENTS,          W, W, R, R, -, -",
            "REAL_ESTATE,      W, W, R, R, -, -",
            "VEHICLES,         W, W, R, R, -, -",
            "CONTRACTS,        W, W, -, R, -, -",
            "TEAMS,            W, R, -, R, R, -",
            "EQUIPMENT,        W, R, W, R, -, -",
            "INSTALL_REQUESTS, W, W, W, -, -, -",
            "MY_DISPATCHES,    -, -, -, -, W, -",
            "USERS,            W, -, -, -, -, -",
            "AUDIT,            R, -, -, -, -, -"
    })
    void matrixComplete(String section, String admin, String manager, String engineer,
                        String operator, String team, String client) {
        Section s = Section.valueOf(section);
        assertEquals(parse(admin), Permissions.access(Role.ADMINISTRATOR, s));
        assertEquals(parse(manager), Permissions.access(Role.MANAGER, s));
        assertEquals(parse(engineer), Permissions.access(Role.ENGINEER, s));
        assertEquals(parse(operator), Permissions.access(Role.OPERATOR, s));
        assertEquals(parse(team), Permissions.access(Role.RESPONSE_TEAM, s));
        assertEquals(parse(client), Permissions.access(Role.CLIENT, s));
    }

    private static Access parse(String v) {
        return switch (v) {
            case "W" -> Access.WRITE;
            case "R" -> Access.READ;
            default -> Access.NONE;
        };
    }
}
