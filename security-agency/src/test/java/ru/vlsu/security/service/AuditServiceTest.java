package ru.vlsu.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vlsu.security.TestDb;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuditServiceTest {

    private AuditService audit;

    @BeforeEach
    void setUp() {
        TestDb.reset();
        audit = new AuditService(TestDb.dataSource());
    }

    @Test
    void log_thenFindRecent_newestFirst() {
        audit.log(null, "FIRST", "object", 1L, "{}");
        audit.log(null, "SECOND", "object", 2L, "{\"a\": 1}");
        List<AuditService.AuditEntry> entries = audit.findRecent(10);
        assertEquals(2, entries.size());
        assertEquals("SECOND", entries.get(0).action());
        assertEquals("FIRST", entries.get(1).action());
        assertNull(entries.get(0).userLogin());
        assertEquals("{\"a\": 1}", entries.get(0).details());
    }
}
