package ru.vlsu.security.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vlsu.security.TestDb;
import ru.vlsu.security.model.Contract;
import ru.vlsu.security.model.ContractStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContractDaoTest {

    private ContractDao dao;

    @BeforeEach
    void setUp() {
        TestDb.reset();
        dao = new ContractDao(TestDb.dataSource());
    }

    private static Contract contract(long id, String number, List<Long> objects) {
        return new Contract(id, 2, null, number, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 1),
                LocalDate.of(2027, 4, 30), ContractStatus.DRAFT, new BigDecimal("1000.00"), objects);
    }

    private Contract find(long id) {
        return dao.findAll().stream().filter(c -> c.id() == id).findFirst().orElseThrow();
    }

    @Test
    void findAll_seedHasObjects() {
        Contract c2 = find(2);
        assertEquals(List.of(1L, 5L), c2.objectIds());
        assertEquals("Смирнова Ольга Петровна", c2.clientName());
    }

    @Test
    void insert_withTwoObjects_thenFindAll_hasObjectIds() {
        long id = dao.insert(contract(0, "Д-TEST/1", List.of(1L, 5L)));
        assertEquals(List.of(1L, 5L), find(id).objectIds());
        assertEquals(ContractStatus.DRAFT, find(id).status());
    }

    @Test
    void update_replacesObjects() {
        long id = dao.insert(contract(0, "Д-TEST/1", List.of(1L, 5L)));
        dao.update(contract(id, "Д-TEST/1", List.of(5L)));
        assertEquals(List.of(5L), find(id).objectIds());
    }

    @Test
    void delete_cascadesContractObjects() {
        long id = dao.insert(contract(0, "Д-TEST/1", List.of(1L)));
        dao.delete(id);
        assertEquals(3, dao.findAll().size());
    }

    @Test
    void duplicateNumber_fails() {
        DataAccessException e = assertThrows(DataAccessException.class,
                () -> dao.insert(contract(0, "Д-2026/001", List.of(1L))));
        assertEquals("23505", e.sqlState());
    }
}
