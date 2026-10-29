package ru.vlsu.security;

import ru.vlsu.security.config.AppConfig;
import ru.vlsu.security.dao.ClientDao;
import ru.vlsu.security.dao.RealEstateDao;
import ru.vlsu.security.dao.UserDao;
import ru.vlsu.security.dao.VehicleDao;
import ru.vlsu.security.service.AuditService;
import ru.vlsu.security.service.AuthService;

import javax.sql.DataSource;

/** Все сервисы приложения; создаётся один раз при запуске. */
public record AppContext(DataSource ds, AppConfig config, UserDao users, AuditService audit, AuthService auth,
                         ClientDao clients, RealEstateDao realEstate, VehicleDao vehicles) {

    public static AppContext create(DataSource ds, AppConfig config) {
        UserDao users = new UserDao(ds);
        AuditService audit = new AuditService(ds);
        return new AppContext(ds, config, users, audit, new AuthService(users, audit),
                new ClientDao(ds), new RealEstateDao(ds), new VehicleDao(ds));
    }
}
