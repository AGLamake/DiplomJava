package ru.vlsu.security.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vlsu.security.TestDb;
import ru.vlsu.security.model.Equipment;
import ru.vlsu.security.model.EquipmentStatus;
import ru.vlsu.security.model.EquipmentType;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EquipmentDaoTest {

    private EquipmentDao dao;
    private EquipmentType motion;

    @BeforeEach
    void setUp() {
        TestDb.reset();
        dao = new EquipmentDao(TestDb.dataSource());
        motion = new EquipmentTypeDao(TestDb.dataSource()).findAll().stream()
                .filter(t -> t.category().equals("motion_sensor")).findFirst().orElseThrow();
    }

    private static Equipment item(long typeId, String serial) {
        return new Equipment(0, 1, null, typeId, null, serial, LocalDate.of(2026, 6, 1), EquipmentStatus.INSTALLED);
    }

    @Test
    void types_includeGasSensor() {
        assertTrue(new EquipmentTypeDao(TestDb.dataSource()).findAll().stream()
                .anyMatch(t -> t.name().equals("Датчик утечки газа (ДУГ)")));
    }

    @Test
    void insert_thenFindAll_hasTypeAndObjectNames() {
        long id = dao.insert(item(motion.id(), "SN-001"));
        Equipment e = dao.findAll().stream().filter(x -> x.id() == id).findFirst().orElseThrow();
        assertEquals("Датчик движения (ИК)", e.typeName());
        assertTrue(e.objectName().contains("Квартира"));
    }

    @Test
    void update_changesStatus() {
        long id = dao.insert(item(motion.id(), "SN-001"));
        dao.update(new Equipment(id, 1, null, motion.id(), null, "SN-001", null, EquipmentStatus.FAULT));
        assertEquals(EquipmentStatus.FAULT,
                dao.findAll().stream().filter(x -> x.id() == id).findFirst().orElseThrow().status());
    }

    @Test
    void duplicateSerial_fails() {
        dao.insert(item(motion.id(), "SN-001"));
        DataAccessException e = assertThrows(DataAccessException.class, () -> dao.insert(item(motion.id(), "SN-001")));
        assertEquals("23505", e.sqlState());
    }
}
