package ru.vlsu.security.web;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import ru.vlsu.security.dao.CrudDao;
import ru.vlsu.security.model.Client;
import ru.vlsu.security.model.Contract;
import ru.vlsu.security.model.ContractStatus;
import ru.vlsu.security.model.ObjectRef;
import ru.vlsu.security.security.Section;
import ru.vlsu.security.service.ContractValidator;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ru.vlsu.security.validation.FormParsers.price;
import static ru.vlsu.security.validation.FormParsers.requireChoice;

@Named
@ViewScoped
public class ContractsBean extends CrudBean<Contract, ContractsBean.Form> {

    private List<Client> clients = List.of();
    private List<ObjectRef> objects = List.of();

    @Override
    public String getTitle() {
        return "Договоры";
    }

    @Override
    protected Section section() {
        return Section.CONTRACTS;
    }

    @Override
    protected List<ColumnSpec<Contract>> columns() {
        return List.of(
                new ColumnSpec<>("Номер", Contract::number, 120),
                new ColumnSpec<>("Клиент", Contract::clientName, 240),
                new ColumnSpec<>("Подписан", c -> Formats.date(c.signedOn()), 100),
                new ColumnSpec<>("С", c -> Formats.date(c.validFrom()), 100),
                new ColumnSpec<>("По", c -> Formats.date(c.validTo()), 100),
                new ColumnSpec<>("Статус", Contract::status, 110),
                new ColumnSpec<>("Цена", Contract::price, 100),
                new ColumnSpec<>("Объектов", c -> c.objectIds().size(), 90));
    }

    @Override
    protected List<Contract> load() {
        return ctx().contracts().findAll();
    }

    @Override
    protected long key(Contract row) {
        return row.id();
    }

    @Override
    protected CrudDao<Contract> dao() {
        return ctx().contracts();
    }

    @Override
    protected String formTitle(boolean creating) {
        return creating ? "Новый договор" : "Договор";
    }

    @Override
    protected Form newForm(Contract c) {
        clients = ctx().clients().findAll();
        objects = ctx().objectRefs().findAll();
        Form f = new Form();
        if (c != null) {
            f.id = c.id();
            f.clientId = c.clientId();
            f.number = c.number();
            f.signedOn = c.signedOn();
            f.validFrom = c.validFrom();
            f.validTo = c.validTo();
            f.status = c.status();
            f.price = c.price() == null ? "" : c.price().toPlainString();
            f.objectIds = new ArrayList<>(c.objectIds());
        }
        return f;
    }

    @Override
    protected Contract build(Form f) {
        Client c = requireChoice(Lookups.byId(clients, f.clientId, Client::id), "Клиент");
        Contract result = new Contract(f.id, c.id(), c.name(), f.number == null ? null : f.number.trim(),
                f.signedOn, f.validFrom, f.validTo, f.status, price(f.price), List.copyOf(f.objectIds));
        ContractValidator.validate(result, ownObjectIds(c.id()));
        return result;
    }

    /** Объекты выбранного в форме клиента — флажки «Объекты». */
    public List<ObjectRef> getClientObjects() {
        Form f = getForm();
        if (f == null || f.clientId == null) {
            return List.of();
        }
        return objects.stream().filter(o -> o.clientId() == f.clientId).toList();
    }

    /** При смене клиента отметки чужих объектов снимаются. */
    public void clientChanged() {
        Form f = getForm();
        if (f != null) {
            Set<Long> own = f.clientId == null ? Set.of() : ownObjectIds(f.clientId);
            f.objectIds.removeIf(id -> !own.contains(id));
        }
    }

    private Set<Long> ownObjectIds(long clientId) {
        return objects.stream().filter(o -> o.clientId() == clientId).map(ObjectRef::id).collect(Collectors.toSet());
    }

    public List<Client> getClients() {
        return clients;
    }

    public ContractStatus[] getStatuses() {
        return ContractStatus.values();
    }

    public static class Form implements Serializable {
        private long id;
        private Long clientId;
        private String number;
        private LocalDate signedOn = LocalDate.now();
        private LocalDate validFrom = LocalDate.now();
        private LocalDate validTo;
        private ContractStatus status = ContractStatus.DRAFT;
        private String price;
        private List<Long> objectIds = new ArrayList<>();

        public Long getClientId() {
            return clientId;
        }

        public void setClientId(Long clientId) {
            this.clientId = clientId;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public LocalDate getSignedOn() {
            return signedOn;
        }

        public void setSignedOn(LocalDate signedOn) {
            this.signedOn = signedOn;
        }

        public LocalDate getValidFrom() {
            return validFrom;
        }

        public void setValidFrom(LocalDate validFrom) {
            this.validFrom = validFrom;
        }

        public LocalDate getValidTo() {
            return validTo;
        }

        public void setValidTo(LocalDate validTo) {
            this.validTo = validTo;
        }

        public ContractStatus getStatus() {
            return status;
        }

        public void setStatus(ContractStatus status) {
            this.status = status;
        }

        public String getPrice() {
            return price;
        }

        public void setPrice(String price) {
            this.price = price;
        }

        public List<Long> getObjectIds() {
            return objectIds;
        }

        public void setObjectIds(List<Long> objectIds) {
            this.objectIds = objectIds == null ? new ArrayList<>() : new ArrayList<>(objectIds);
        }
    }
}
