package ru.vlsu.security.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vlsu.security.TestDb;
import ru.vlsu.security.model.ObjectStatus;
import ru.vlsu.security.model.SecurityStatus;
import ru.vlsu.security.model.VehicleObject;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VehicleDaoTest {

    private VehicleDao dao;

    @BeforeEach
    void setUp() {
        TestDb.reset();
        dao = new VehicleDao(TestDb.dataSource());
    }

    private static VehicleObject logan(String reg) {
        return new VehicleObject(0, 2, null, "Renault", "Logan", reg, null, ObjectStatus.ACTIVE,
                SecurityStatus.DISARMED);
    }

    @Test
    void findAll_returnsSeedVehicles() {
        assertEquals(3, dao.findAll().size());
    }

    @Test
    void insert_createsObjectAndVehicle() throws Exception {
        long objectId = dao.insert(logan("К555КК"));
        VehicleObject v = dao.findAll().stream().filter(x -> x.objectId() == objectId).findFirst().orElseThrow();
        assertEquals("К555КК", v.regNumber());
        assertEquals("Renault Logan", scalar("SELECT name FROM protected_objects WHERE object_id = " + objectId));
    }

    @Test
    void insert_duplicateRegNumber_fails_andNoOrphanObject() throws Exception {
        String before = scalar("SELECT count(*) FROM protected_objects");
        DataAccessException e = assertThrows(DataAccessException.class, () -> dao.insert(logan("Т843СТ")));
        assertEquals("23505", e.sqlState());
        assertEquals(before, scalar("SELECT count(*) FROM protected_objects"));
    }

    @Test
    void update_changesBothTables() throws Exception {
        long objectId = dao.insert(logan("К555КК"));
        dao.update(new VehicleObject(objectId, 2, null, "Renault", "Sandero", "К556КК", "VIN1",
                ObjectStatus.ACTIVE, SecurityStatus.DISARMED));
        VehicleObject v = dao.findAll().stream().filter(x -> x.objectId() == objectId).findFirst().orElseThrow();
        assertEquals("К556КК", v.regNumber());
        assertEquals("Renault Sandero", scalar("SELECT name FROM protected_objects WHERE object_id = " + objectId));
    }

    @Test
    void delete_removesBoth() throws Exception {
        long objectId = dao.insert(logan("К555КК"));
        dao.delete(objectId);
        assertEquals("0", scalar("SELECT count(*) FROM vehicles WHERE object_id = " + objectId));
        assertEquals("0", scalar("SELECT count(*) FROM protected_objects WHERE object_id = " + objectId));
    }

    private static String scalar(String sql) throws Exception {
        try (Connection c = TestDb.dataSource().getConnection();
             Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }
}
