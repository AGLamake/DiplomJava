package ru.vlsu.security.web;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import ru.vlsu.security.dao.CrudDao;
import ru.vlsu.security.model.Client;
import ru.vlsu.security.model.ObjectStatus;
import ru.vlsu.security.model.RealEstate;
import ru.vlsu.security.model.SecurityStatus;
import ru.vlsu.security.security.Section;

import java.io.Serializable;
import java.util.List;

import static ru.vlsu.security.validation.FormParsers.requireChoice;
import static ru.vlsu.security.validation.FormParsers.requireText;

@Named
@ViewScoped
public class RealEstateBean extends CrudBean<RealEstate, RealEstateBean.Form> {

    private List<Client> clients = List.of();

    @Override
    public String getTitle() {
        return "Объекты недвижимости";
    }

    @Override
    protected Section section() {
        return Section.REAL_ESTATE;
    }

    @Override
    protected List<ColumnSpec<RealEstate>> columns() {
        return List.of(
                new ColumnSpec<>("ID", RealEstate::id, 60),
                new ColumnSpec<>("Клиент", RealEstate::clientName, 220),
                new ColumnSpec<>("Название", RealEstate::name, 150),
                new ColumnSpec<>("Адрес", RealEstate::address, 340),
                new ColumnSpec<>("Статус", RealEstate::objectStatus, 120),
                new ColumnSpec<>("Охрана", RealEstate::securityStatus, 120));
    }

    @Override
    protected List<RealEstate> load() {
        return ctx().realEstate().findAll();
    }

    @Override
    protected long key(RealEstate row) {
        return row.id();
    }

    @Override
    protected CrudDao<RealEstate> dao() {
        return ctx().realEstate();
    }

    @Override
    protected String formTitle(boolean creating) {
        return creating ? "Новый объект" : "Объект недвижимости";
    }

    @Override
    protected Form newForm(RealEstate r) {
        clients = ctx().clients().findAll();
        Form f = new Form();
        if (r != null) {
            f.id = r.id();
            f.clientId = r.clientId();
            f.name = r.name();
            f.address = r.address();
            f.status = r.objectStatus();
            f.security = r.securityStatus();
        }
        return f;
    }

    @Override
    protected RealEstate build(Form f) {
        Client c = requireChoice(Lookups.byId(clients, f.clientId, Client::id), "Клиент");
        return new RealEstate(f.id, c.id(), c.name(), requireText(f.name, "Название"),
                requireText(f.address, "Адрес"), requireChoice(f.status, "Статус объекта"), f.security);
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
        private String name;
        private String address;
        private ObjectStatus status = ObjectStatus.ACTIVE;
        private SecurityStatus security = SecurityStatus.DISARMED;

        public Long getClientId() {
            return clientId;
        }

        public void setClientId(Long clientId) {
            this.clientId = clientId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public ObjectStatus getStatus() {
            return status;
        }

        public void setStatus(ObjectStatus status) {
            this.status = status;
        }
    }
}
