package ru.vlsu.security.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vlsu.security.TestDb;
import ru.vlsu.security.model.ResponseTeam;
import ru.vlsu.security.model.TeamStatus;
import ru.vlsu.security.security.PasswordHasher;
import ru.vlsu.security.security.Role;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResponseTeamDaoTest {

    private ResponseTeamDao dao;
    private long ivanov;
    private long petrov;

    @BeforeEach
    void setUp() {
        TestDb.reset();
        dao = new ResponseTeamDao(TestDb.dataSource());
        UserDao users = new UserDao(TestDb.dataSource());
        String hash = PasswordHasher.hash("123456".toCharArray());
        ivanov = users.insert("ivanov", hash, "Иванов И.И.", Role.RESPONSE_TEAM, true);
        petrov = users.insert("petrov", hash, "Петров П.П.", Role.RESPONSE_TEAM, true);
    }

    private ResponseTeam find(long id) {
        return dao.findAll().stream().filter(t -> t.id() == id).findFirst().orElseThrow();
    }

    @Test
    void findAll_seed2() {
        assertEquals(2, dao.findAll().size());
        assertEquals(TeamStatus.AVAILABLE, find(1).status());
    }

    @Test
    void insert_withMembers_thenMembersText() {
        long id = dao.insert(new ResponseTeam(0, "ГБР-3", "+7", TeamStatus.AVAILABLE, List.of(ivanov, petrov), null));
        ResponseTeam t = find(id);
        assertEquals(List.of(ivanov, petrov), t.memberUserIds());
        assertEquals("Иванов И.И., Петров П.П.", t.membersText());
    }

    @Test
    void update_replacesMembers() {
        long id = dao.insert(new ResponseTeam(0, "ГБР-3", "+7", TeamStatus.AVAILABLE, List.of(ivanov, petrov), null));
        dao.update(new ResponseTeam(id, "ГБР-3", "+7", TeamStatus.UNAVAILABLE, List.of(petrov), null));
        ResponseTeam t = find(id);
        assertEquals(List.of(petrov), t.memberUserIds());
        assertEquals(TeamStatus.UNAVAILABLE, t.status());
    }
}
