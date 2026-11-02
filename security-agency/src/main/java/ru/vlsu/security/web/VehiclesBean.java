package ru.vlsu.security.web;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import ru.vlsu.security.dao.CrudDao;
import ru.vlsu.security.model.Client;
import ru.vlsu.security.model.ObjectStatus;
import ru.vlsu.security.model.SecurityStatus;
import ru.vlsu.security.model.VehicleObject;
import ru.vlsu.security.security.Section;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

import static ru.vlsu.security.validation.FormParsers.optionalText;
import static ru.vlsu.security.validation.FormParsers.requireChoice;
import static ru.vlsu.security.validation.FormParsers.requireText;

@Named
@ViewScoped
public class VehiclesBean extends CrudBean<VehicleObject, VehiclesBean.Form> {

    private List<Client> clients = List.of();

    @Override
    public String getTitle() {
        return "Автомобили";
    }

    @Override
    protected Section section() {
        return Section.VEHICLES;
    }

    @Override
    protected List<ColumnSpec<VehicleObject>> columns() {
        return List.of(
                new ColumnSpec<>("ID", VehicleObject::objectId, 60),
                new ColumnSpec<>("Клиент", VehicleObject::clientName, 220),
                new ColumnSpec<>("Марка", VehicleObject::brand, 120),
                new ColumnSpec<>("Модель", VehicleObject::model, 120),
                new ColumnSpec<>("Гос. номер", VehicleObject::regNumber, 110),
                new ColumnSpec<>("VIN", VehicleObject::vin, 180),
                new ColumnSpec<>("Статус", VehicleObject::objectStatus, 110),
                new ColumnSpec<>("Охрана", VehicleObject::securityStatus, 110));
    }

    @Override
    protected List<VehicleObject> load() {
        return ctx().vehicles().findAll();
    }

    @Override
    protected long key(VehicleObject row) {
        return row.objectId();
    }

    @Override
    protected CrudDao<VehicleObject> dao() {
        return ctx().vehicles();
    }

    @Override
    protected String formTitle(boolean creating) {
        return creating ? "Новый автомобиль" : "Автомобиль";
    }

    @Override
    protected Form newForm(VehicleObject v) {
        clients = ctx().clients().findAll();
        Form f = new Form();
        if (v != null) {
            f.id = v.objectId();
            f.security = v.securityStatus();
            f.clientId = v.clientId();
            f.brand = v.brand();
            f.model = v.model();
            f.regNumber = v.regNumber();
            f.vin = v.vin();
            f.status = v.objectStatus();
        }
        return f;
    }

    @Override
    protected VehicleObject build(Form f) {
        Client c = requireChoice(Lookups.byId(clients, f.clientId, Client::id), "Клиент");
        String vin = optionalText(f.vin);
        return new VehicleObject(f.id, c.id(), c.name(), requireText(f.brand, "Марка"),
                requireText(f.model, "Модель"), requireText(f.regNumber, "Гос. номер").toUpperCase(Locale.ROOT),
                vin == null ? null : vin.toUpperCase(Locale.ROOT), requireChoice(f.status, "Статус объекта"),
                f.security);
    }

    public List<Client> getClients() {
        return clients;
    }

    public ObjectStatus[] getStatuses() {
        return ObjectStatus.values();
    }

    public static class Form implements Serializable {
        private long id;
        private Long clientId;
        private String brand;
        private String model;
        private String regNumber;
        private String vin;
        private ObjectStatus status = ObjectStatus.ACTIVE;
        private SecurityStatus security = SecurityStatus.DISARMED;

        public Long getClientId() {
            return clientId;
        }

        public void setClientId(Long clientId) {
            this.clientId = clientId;
        }

        public String getBrand() {
            return brand;
        }

        public void setBrand(String brand) {
            this.brand = brand;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getRegNumber() {
            return regNumber;
        }

        public void setRegNumber(String regNumber) {
            this.regNumber = regNumber;
        }

        public String getVin() {
            return vin;
        }

        public void setVin(String vin) {
            this.vin = vin;
        }

        public ObjectStatus getStatus() {
            return status;
        }

        public void setStatus(ObjectStatus status) {
            this.status = status;
        }
    }
}
