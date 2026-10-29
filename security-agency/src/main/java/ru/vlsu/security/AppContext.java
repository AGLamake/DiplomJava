package ru.vlsu.security;

import ru.vlsu.security.config.AppConfig;
import ru.vlsu.security.dao.ClientDao;
import ru.vlsu.security.dao.ContractDao;
import ru.vlsu.security.dao.EquipmentDao;
import ru.vlsu.security.dao.EquipmentTypeDao;
import ru.vlsu.security.dao.InstallationRequestDao;
import ru.vlsu.security.dao.ObjectRefDao;
import ru.vlsu.security.dao.RealEstateDao;
import ru.vlsu.security.dao.ResponseTeamDao;
import ru.vlsu.security.dao.UserDao;
import ru.vlsu.security.dao.VehicleDao;
import ru.vlsu.security.service.AlarmService;
import ru.vlsu.security.service.AuditService;
import ru.vlsu.security.service.AuthService;
import ru.vlsu.security.service.DispatchService;

import javax.sql.DataSource;

/** Все сервисы приложения; создаётся один раз при запуске. */
public record AppContext(DataSource ds, AppConfig config, UserDao users, AuditService audit, AuthService auth,
                         ClientDao clients, RealEstateDao realEstate, VehicleDao vehicles,
                         ObjectRefDao objectRefs, ContractDao contracts, ResponseTeamDao teams,
                         EquipmentDao equipment, EquipmentTypeDao equipmentTypes,
                         InstallationRequestDao installRequests, AlarmService alarms,
                         DispatchService dispatches) {

    public static AppContext create(DataSource ds, AppConfig config) {
        UserDao users = new UserDao(ds);
        AuditService audit = new AuditService(ds);
        return new AppContext(ds, config, users, audit, new AuthService(users, audit),
                new ClientDao(ds), new RealEstateDao(ds), new VehicleDao(ds), new ObjectRefDao(ds), new ContractDao(ds),
                new ResponseTeamDao(ds), new EquipmentDao(ds), new EquipmentTypeDao(ds), new InstallationRequestDao(ds),
                new AlarmService(ds, audit), new DispatchService(ds, audit));
    }
}
