package ru.vlsu.security.ui.forms;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import ru.vlsu.security.dao.ClientDao;
import ru.vlsu.security.model.Client;
import ru.vlsu.security.model.ObjectStatus;
import ru.vlsu.security.model.RealEstate;
import ru.vlsu.security.model.SecurityStatus;
import ru.vlsu.security.ui.table.EntityForm;
import ru.vlsu.security.ui.table.FormDialog;

import java.util.Optional;

import static ru.vlsu.security.ui.table.FormParsers.requireChoice;
import static ru.vlsu.security.ui.table.FormParsers.requireText;

public class RealEstateForm implements EntityForm<RealEstate> {

    private final ClientDao clients;

    public RealEstateForm(ClientDao clients) {
        this.clients = clients;
    }

    @Override
    public Optional<RealEstate> show(Window owner, RealEstate initial) {
        FormDialog<RealEstate> f = new FormDialog<>(owner, initial == null ? "Новый объект" : "Объект недвижимости");
        ComboBox<Client> client = f.add("Клиент", Forms.combo(clients.findAll()));
        TextField name = f.add("Название", new TextField());
        TextField address = f.add("Адрес", new TextField());
        ComboBox<ObjectStatus> status = f.add("Статус объекта", Forms.combo(ObjectStatus.values()));
        status.setValue(ObjectStatus.ACTIVE);
        SecurityStatus security = SecurityStatus.DISARMED;
        long id = 0;
        if (initial != null) {
            id = initial.id();
            Forms.select(client, initial.clientId(), Client::id);
            name.setText(initial.name());
            address.setText(initial.address());
            status.setValue(initial.objectStatus());
            security = initial.securityStatus();
        }
        long objectId = id;
        SecurityStatus securityStatus = security;
        return f.show(() -> {
            Client c = requireChoice(client.getValue(), "Клиент");
            return new RealEstate(objectId, c.id(), c.name(), requireText(name.getText(), "Название"),
                    requireText(address.getText(), "Адрес"), requireChoice(status.getValue(), "Статус объекта"),
                    securityStatus);
        });
    }
}
