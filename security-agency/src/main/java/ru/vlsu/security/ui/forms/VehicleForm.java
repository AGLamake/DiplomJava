package ru.vlsu.security.ui.forms;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import ru.vlsu.security.dao.ClientDao;
import ru.vlsu.security.model.Client;
import ru.vlsu.security.model.ObjectStatus;
import ru.vlsu.security.model.SecurityStatus;
import ru.vlsu.security.model.VehicleObject;
import ru.vlsu.security.ui.table.EntityForm;
import ru.vlsu.security.ui.table.FormDialog;

import java.util.Locale;
import java.util.Optional;

import static ru.vlsu.security.validation.FormParsers.optionalText;
import static ru.vlsu.security.validation.FormParsers.requireChoice;
import static ru.vlsu.security.validation.FormParsers.requireText;

public class VehicleForm implements EntityForm<VehicleObject> {

    private final ClientDao clients;

    public VehicleForm(ClientDao clients) {
        this.clients = clients;
    }

    @Override
    public Optional<VehicleObject> show(Window owner, VehicleObject initial) {
        FormDialog<VehicleObject> f = new FormDialog<>(owner, initial == null ? "Новый автомобиль" : "Автомобиль");
        ComboBox<Client> client = f.add("Клиент", Forms.combo(clients.findAll()));
        TextField brand = f.add("Марка", new TextField());
        TextField model = f.add("Модель", new TextField());
        TextField reg = f.add("Гос. номер", new TextField());
        TextField vin = f.add("VIN", new TextField());
        ComboBox<ObjectStatus> status = f.add("Статус объекта", Forms.combo(ObjectStatus.values()));
        status.setValue(ObjectStatus.ACTIVE);
        long id = 0;
        SecurityStatus security = SecurityStatus.DISARMED;
        if (initial != null) {
            id = initial.objectId();
            security = initial.securityStatus();
            Forms.select(client, initial.clientId(), Client::id);
            brand.setText(initial.brand());
            model.setText(initial.model());
            reg.setText(initial.regNumber());
            vin.setText(initial.vin());
            status.setValue(initial.objectStatus());
        }
        long objectId = id;
        SecurityStatus securityStatus = security;
        return f.show(() -> {
            Client c = requireChoice(client.getValue(), "Клиент");
            String vinValue = optionalText(vin.getText());
            return new VehicleObject(objectId, c.id(), c.name(), requireText(brand.getText(), "Марка"),
                    requireText(model.getText(), "Модель"),
                    requireText(reg.getText(), "Гос. номер").toUpperCase(Locale.ROOT),
                    vinValue == null ? null : vinValue.toUpperCase(Locale.ROOT),
                    requireChoice(status.getValue(), "Статус объекта"), securityStatus);
        });
    }
}
