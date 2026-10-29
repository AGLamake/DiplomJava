package ru.vlsu.security.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vlsu.security.TestDb;
import ru.vlsu.security.model.Client;
import ru.vlsu.security.model.ClientType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientDaoTest {

    private ClientDao dao;

    @BeforeEach
    void setUp() {
        TestDb.reset();
        dao = new ClientDao(TestDb.dataSource());
    }

    @Test
    void findAll_returnsSeed() {
        assertEquals(4, dao.findAll().size());
    }

    @Test
    void insert_thenFind() {
        long id = dao.insert(new Client(0, ClientType.INDIVIDUAL, "Новиков Н.Н.", null, "+79990001122", null));
        Client c = dao.findAll().stream().filter(x -> x.id() == id).findFirst().orElseThrow();
        assertEquals("Новиков Н.Н.", c.name());
        assertEquals(ClientType.INDIVIDUAL, c.type());
    }

    @Test
    void update_changesPhone() {
        Client c = dao.findAll().get(0);
        dao.update(new Client(c.id(), c.type(), c.name(), c.contactPerson(), "+70000000000", c.email()));
        assertEquals("+70000000000",
                dao.findAll().stream().filter(x -> x.id() == c.id()).findFirst().orElseThrow().phone());
    }

    @Test
    void delete_clientWithObjects_failsWithFkState() {
        DataAccessException e = assertThrows(DataAccessException.class, () -> dao.delete(1));
        assertEquals("23503", e.sqlState());
    }

    @Test
    void insert_blankName_failsCheck() {
        DataAccessException e = assertThrows(DataAccessException.class,
                () -> dao.insert(new Client(0, ClientType.INDIVIDUAL, "   ", null, "+7", null)));
        assertEquals("23514", e.sqlState());
    }
}
