package ru.vlsu.security.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vlsu.security.TestDb;
import ru.vlsu.security.model.ObjectStatus;
import ru.vlsu.security.model.RealEstate;
import ru.vlsu.security.model.SecurityStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealEstateDaoTest {

    private RealEstateDao dao;

    @BeforeEach
    void setUp() {
        TestDb.reset();
        dao = new RealEstateDao(TestDb.dataSource());
    }

    @Test
    void findAll_onlyRealEstate() {
        List<RealEstate> all = dao.findAll();
        assertEquals(3, all.size());
        assertTrue(all.stream().anyMatch(r -> r.address().contains("Школьная")));
    }

    @Test
    void findAll_includesClientName() {
        RealEstate flat = dao.findAll().stream().filter(r -> r.id() == 1).findFirst().orElseThrow();
        assertEquals("Смирнова Ольга Петровна", flat.clientName());
        assertEquals(SecurityStatus.ARMED, flat.securityStatus());
    }

    @Test
    void insert_withoutAddress_fails() {
        DataAccessException e = assertThrows(DataAccessException.class, () -> dao.insert(
                new RealEstate(0, 1, null, "Дача", null, ObjectStatus.ACTIVE, SecurityStatus.DISARMED)));
        assertEquals("23514", e.sqlState());
    }

    @Test
    void insert_update_delete() {
        long id = dao.insert(new RealEstate(0, 1, null, "Дача", "СНТ Луч, уч. 5", ObjectStatus.ACTIVE,
                SecurityStatus.DISARMED));
        dao.update(new RealEstate(id, 1, null, "Дача", "СНТ Луч, уч. 6", ObjectStatus.SUSPENDED,
                SecurityStatus.DISARMED));
        RealEstate r = dao.findAll().stream().filter(x -> x.id() == id).findFirst().orElseThrow();
        assertEquals("СНТ Луч, уч. 6", r.address());
        assertEquals(ObjectStatus.SUSPENDED, r.objectStatus());
        dao.delete(id);
        assertEquals(3, dao.findAll().size());
    }
}
