package ru.vlsu.security.config;

import java.util.Properties;

/** Настройки приложения из app.properties. */
public record AppConfig(int dispatchEtaSeconds, int mchsEtaSeconds) {

    public static AppConfig load() {
        Properties p = Database.loadProperties("app.properties");
        return new AppConfig(
                Integer.parseInt(p.getProperty("dispatch.eta.seconds", "900").trim()),
                Integer.parseInt(p.getProperty("mchs.eta.seconds", "900").trim()));
    }
}
