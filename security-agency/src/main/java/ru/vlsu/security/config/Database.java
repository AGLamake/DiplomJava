package ru.vlsu.security.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/** Подключение к PostgreSQL через пул HikariCP. */
public final class Database {

    private Database() {
    }

    public static Properties loadProperties(String classpathResource) {
        try (InputStream in = Database.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IllegalStateException("Не найден файл настроек " + classpathResource);
            }
            Properties p = new Properties();
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return p;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static HikariDataSource create(Properties p) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(p.getProperty("db.url"));
        config.setUsername(p.getProperty("db.user"));
        config.setPassword(p.getProperty("db.password"));
        config.setMaximumPoolSize(5);
        config.setConnectionTimeout(3000);
        // Пул не должен падать при создании, если БД недоступна — это проверяет ping().
        config.setInitializationFailTimeout(-1);
        return new HikariDataSource(config);
    }

    public static boolean ping(DataSource ds) {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("SELECT 1");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
