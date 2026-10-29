package ru.vlsu.security.ui.forms;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import ru.vlsu.security.dao.ClientDao;
import ru.vlsu.security.dao.ObjectRefDao;
import ru.vlsu.security.model.Client;
import ru.vlsu.security.model.Contract;
import ru.vlsu.security.model.ContractStatus;
import ru.vlsu.security.model.ObjectRef;
import ru.vlsu.security.service.ContractValidator;
import ru.vlsu.security.ui.table.EntityForm;
import ru.vlsu.security.ui.table.FormDialog;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ru.vlsu.security.validation.FormParsers.price;
import static ru.vlsu.security.validation.FormParsers.requireChoice;

public class ContractForm implements EntityForm<Contract> {

    private final ClientDao clients;
    private final ObjectRefDao objects;

    public ContractForm(ClientDao clients, ObjectRefDao objects) {
        this.clients = clients;
        this.objects = objects;
    }

    @Override
    public Optional<Contract> show(Window owner, Contract initial) {
        FormDialog<Contract> f = new FormDialog<>(owner, initial == null ? "Новый договор" : "Договор");
        ComboBox<Client> client = f.add("Клиент", Forms.combo(clients.findAll()));
        TextField number = f.add("Номер договора", new TextField());
        DatePicker signed = f.add("Дата подписания", new DatePicker(LocalDate.now()));
        DatePicker from = f.add("Действует с", new DatePicker(LocalDate.now()));
        DatePicker to = f.add("Действует по", new DatePicker());
        ComboBox<ContractStatus> status = f.add("Статус", Forms.combo(ContractStatus.values()));
        status.setValue(ContractStatus.DRAFT);
        TextField priceField = f.add("Цена, руб./мес.", new TextField());
        VBox objectBox = f.add("Объекты", new VBox(4));

        List<ObjectRef> allObjects = objects.findAll();
        List<CheckBox> checks = new ArrayList<>();
        Set<Long> initiallyChecked = new HashSet<>(initial == null ? List.of() : initial.objectIds());
        Runnable fillObjects = () -> {
            objectBox.getChildren().clear();
            checks.clear();
            Client c = client.getValue();
            if (c == null) {
                return;
            }
            for (ObjectRef o : allObjects) {
                if (o.clientId() == c.id()) {
                    CheckBox cb = new CheckBox(o.label());
                    cb.setUserData(o.id());
                    cb.setSelected(initiallyChecked.contains(o.id()));
                    checks.add(cb);
                    objectBox.getChildren().add(cb);
                }
            }
        };
        client.valueProperty().addListener((obs, o, n) -> fillObjects.run());

        long id = 0;
        if (initial != null) {
            id = initial.id();
            Forms.select(client, initial.clientId(), Client::id);
            number.setText(initial.number());
            signed.setValue(initial.signedOn());
            from.setValue(initial.validFrom());
            to.setValue(initial.validTo());
            status.setValue(initial.status());
            priceField.setText(initial.price() == null ? "" : initial.price().toPlainString());
        }
        long contractId = id;
        return f.show(() -> {
            Client c = requireChoice(client.getValue(), "Клиент");
            List<Long> selected = checks.stream().filter(CheckBox::isSelected).map(cb -> (Long) cb.getUserData())
                    .toList();
            Set<Long> own = new HashSet<>();
            allObjects.stream().filter(o -> o.clientId() == c.id()).forEach(o -> own.add(o.id()));
            Contract result = new Contract(contractId, c.id(), c.name(),
                    number.getText() == null ? null : number.getText().trim(), signed.getValue(), from.getValue(),
                    to.getValue(), status.getValue(), price(priceField.getText()), selected);
            ContractValidator.validate(result, own);
            return result;
        });
    }
}
