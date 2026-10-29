package ru.vlsu.security.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vlsu.security.TestDb;
import ru.vlsu.security.model.InstallationRequest;
import ru.vlsu.security.model.RequestStatus;
import ru.vlsu.security.security.PasswordHasher;
import ru.vlsu.security.security.Role;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class InstallationRequestDaoTest {

    private InstallationRequestDao dao;
    private long manager;
    private long engineer;

    @BeforeEach
    void setUp() {
        TestDb.reset();
        dao = new InstallationRequestDao(TestDb.dataSource());
        UserDao users = new UserDao(TestDb.dataSource());
        String hash = PasswordHasher.hash("123456".toCharArray());
        manager = users.insert("manager", hash, "Менеджеров М.М.", Role.MANAGER, true);
        engineer = users.insert("engineer", hash, "Инженеров И.И.", Role.ENGINEER, true);
    }

    private InstallationRequest request(long id, Long assignedTo, RequestStatus status) {
        return new InstallationRequest(id, 1, null, manager, null, assignedTo, null, null, null, null, status,
                "Установить ИК-датчик");
    }

    private InstallationRequest find(long id) {
        return dao.findAll().stream().filter(r -> r.id() == id).findFirst().orElseThrow();
    }

    @Test
    void insert_setsCreatedAt() {
        long id = dao.insert(request(0, null, RequestStatus.NEW));
        InstallationRequest r = find(id);
        assertNotNull(r.createdAt());
        assertEquals("Менеджеров М.М.", r.createdByName());
    }

    @Test
    void assignedTo_nullable() {
        long id = dao.insert(request(0, null, RequestStatus.NEW));
        assertNull(find(id).assignedTo());
        dao.update(request(id, engineer, RequestStatus.ASSIGNED));
        assertEquals("Инженеров И.И.", find(id).assignedToName());
    }

    @Test
    void complete_setsCompletedAt() {
        long id = dao.insert(request(0, engineer, RequestStatus.ASSIGNED));
        dao.update(request(id, engineer, RequestStatus.COMPLETED));
        assertNotNull(find(id).completedAt());
    }
}
