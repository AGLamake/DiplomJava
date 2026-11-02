package ru.vlsu.security.web;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import ru.vlsu.security.dao.CrudDao;
import ru.vlsu.security.model.Client;
import ru.vlsu.security.model.ClientType;
import ru.vlsu.security.security.Section;

import java.io.Serializable;
import java.util.List;

import static ru.vlsu.security.validation.FormParsers.optionalText;
import static ru.vlsu.security.validation.FormParsers.requireChoice;
import static ru.vlsu.security.validation.FormParsers.requireText;

@Named
@ViewScoped
public class ClientsBean extends CrudBean<Client, ClientsBean.Form> {

    @Override
    public String getTitle() {
        return "Клиенты";
    }

    @Override
    protected Section section() {
        return Section.CLIENTS;
    }

    @Override
    protected List<ColumnSpec<Client>> columns() {
        return List.of(
                new ColumnSpec<>("ID", Client::id, 60),
                new ColumnSpec<>("Тип", Client::type, 110),
                new ColumnSpec<>("Наименование / ФИО", Client::name, 280),
                new ColumnSpec<>("Контактное лицо", Client::contactPerson, 220),
                new ColumnSpec<>("Телефон", Client::phone, 150),
                new ColumnSpec<>("E-mail", Client::email, 200));
    }

    @Override
    protected List<Client> load() {
        return ctx().clients().findAll();
    }

    @Override
    protected long key(Client row) {
        return row.id();
    }

    @Override
    protected CrudDao<Client> dao() {
        return ctx().clients();
    }

    @Override
    protected String formTitle(boolean creating) {
        return creating ? "Новый клиент" : "Клиент";
    }

    @Override
    protected Form newForm(Client c) {
        Form f = new Form();
        if (c != null) {
            f.id = c.id();
            f.type = c.type();
            f.name = c.name();
            f.contactPerson = c.contactPerson();
            f.phone = c.phone();
            f.email = c.email();
        }
        return f;
    }

    @Override
    protected Client build(Form f) {
        return new Client(f.id, requireChoice(f.type, "Тип"), requireText(f.name, "Наименование / ФИО"),
                optionalText(f.contactPerson), requireText(f.phone, "Телефон"), optionalText(f.email));
    }

    public ClientType[] getTypes() {
        return ClientType.values();
    }

    public static class Form implements Serializable {
        private long id;
        private ClientType type = ClientType.INDIVIDUAL;
        private String name;
        private String contactPerson;
        private String phone;
        private String email;

        public ClientType getType() {
            return type;
        }

        public void setType(ClientType type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getContactPerson() {
            return contactPerson;
        }

        public void setContactPerson(String contactPerson) {
            this.contactPerson = contactPerson;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
