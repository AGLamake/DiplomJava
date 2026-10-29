package ru.vlsu.security;

import ru.vlsu.security.config.Database;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Тестовая база security_test: пересоздаётся из sql/01_schema.sql и sql/02_seed.sql. */
public final class TestDb {

    private static DataSource dataSource;

    private TestDb() {
    }

    public static synchronized DataSource dataSource() {
        if (dataSource == null) {
            dataSource = Database.create(Database.loadProperties("db-test.properties"));
        }
        return dataSource;
    }

    public static void reset() {
        try (Connection c = dataSource().getConnection();
             Statement st = c.createStatement()) {
            st.execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public;");
            st.execute(read("sql/01_schema.sql"));
            st.execute(read("sql/02_seed.sql"));
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Не удалось пересоздать тестовую БД", e);
        }
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
