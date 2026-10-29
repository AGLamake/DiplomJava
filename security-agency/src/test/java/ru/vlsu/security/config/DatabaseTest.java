package ru.vlsu.security.config;

import org.junit.jupiter.api.Test;
import ru.vlsu.security.TestDb;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseTest {

    @Test
    void ping_testDb_returnsTrue() {
        assertTrue(Database.ping(TestDb.dataSource()));
    }

    @Test
    void ping_wrongPort_returnsFalse() {
        Properties p = Database.loadProperties("db-test.properties");
        p.setProperty("db.url", "jdbc:postgresql://localhost:1/security_test");
        assertFalse(Database.ping(Database.create(p)));
    }

    @Test
    void reset_loadsSeed() throws Exception {
        TestDb.reset();
        assertEquals(6, count("roles"));
        assertEquals(9, count("equipment_types"));
        assertEquals(2, count("response_teams"));
    }

    @Test
    void loadSettings_externalFileWins(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("db.properties");
        java.nio.file.Files.writeString(file, "db.url=jdbc:postgresql://example:5432/x\n");
        assertEquals("jdbc:postgresql://example:5432/x",
                Database.loadSettings(file, "db-test.properties").getProperty("db.url"));
    }

    @Test
    void loadSettings_noExternalFile_usesClasspath(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) {
        assertEquals("jdbc:postgresql://localhost:5432/security_test",
                Database.loadSettings(dir.resolve("missing.properties"), "db-test.properties").getProperty("db.url"));
    }

    @Test
    void appConfig_defaults() {
        AppConfig config = AppConfig.load();
        assertEquals(900, config.dispatchEtaSeconds());
        assertEquals(900, config.mchsEtaSeconds());
    }

    private static long count(String table) throws Exception {
        DataSource ds = TestDb.dataSource();
        try (Connection c = ds.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM " + table)) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
