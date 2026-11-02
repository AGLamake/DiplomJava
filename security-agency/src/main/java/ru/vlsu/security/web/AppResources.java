package ru.vlsu.security.web;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import ru.vlsu.security.AppContext;
import ru.vlsu.security.config.AppConfig;
import ru.vlsu.security.config.Database;

import java.nio.file.Path;
import java.util.Properties;

/**
 * Пул соединений и сервисы приложения (бывший App.start()).
 * Путь к внешнему db.properties задаётся системным свойством security.db;
 * если файла нет, берётся db.properties из WEB-INF/classes.
 */
@ApplicationScoped
public class AppResources {

    private HikariDataSource dataSource;
    private AppContext ctx;
    private String dbUrl;
    private String settingsError;
    private volatile boolean ready;

    @PostConstruct
    void init() {
        Properties dbProps;
        try {
            dbProps = Database.loadSettings(Path.of(System.getProperty("security.db", "db.properties")),
                    "db.properties");
        } catch (IllegalStateException e) {
            settingsError = e.getMessage();
            return;
        }
        dbUrl = dbProps.getProperty("db.url");
        dataSource = Database.create(dbProps);
        ctx = AppContext.create(dataSource, AppConfig.load());
    }

    /** Для тестов: готовый контекст без пула и файла настроек. */
    static AppResources forTests(AppContext ctx) {
        AppResources r = new AppResources();
        r.ctx = ctx;
        r.ready = true;
        return r;
    }

    public AppContext ctx() {
        return ctx;
    }

    /** База доступна; пока нет — проверяется заново при каждом обращении. */
    public boolean isReady() {
        if (!ready && ctx != null) {
            ready = Database.ping(ctx.ds());
        }
        return ready;
    }

    public String unavailableMessage() {
        return settingsError != null ? settingsError : "Не удалось подключиться к базе данных: " + dbUrl;
    }

    @PreDestroy
    void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
