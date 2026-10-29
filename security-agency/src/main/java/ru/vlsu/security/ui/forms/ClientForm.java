package ru.vlsu.security.ui.forms;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import ru.vlsu.security.model.Client;
import ru.vlsu.security.model.ClientType;
import ru.vlsu.security.ui.table.EntityForm;
import ru.vlsu.security.ui.table.FormDialog;

import java.util.Optional;

import static ru.vlsu.security.validation.FormParsers.optionalText;
import static ru.vlsu.security.validation.FormParsers.requireChoice;
import static ru.vlsu.security.validation.FormParsers.requireText;

public class ClientForm implements EntityForm<Client> {

    @Override
    public Optional<Client> show(Window owner, Client initial) {
        FormDialog<Client> f = new FormDialog<>(owner, initial == null ? "Новый клиент" : "Клиент");
        ComboBox<ClientType> type = f.add("Тип", Forms.combo(ClientType.values()));
        TextField name = f.add("Наименование / ФИО", new TextField());
        TextField contact = f.add("Контактное лицо", new TextField());
        TextField phone = f.add("Телефон", new TextField());
        TextField email = f.add("E-mail", new TextField());
        if (initial != null) {
            type.setValue(initial.type());
            name.setText(initial.name());
            contact.setText(initial.contactPerson());
            phone.setText(initial.phone());
            email.setText(initial.email());
        } else {
            type.setValue(ClientType.INDIVIDUAL);
        }
        long id = initial == null ? 0 : initial.id();
        return f.show(() -> new Client(id, requireChoice(type.getValue(), "Тип"),
                requireText(name.getText(), "Наименование / ФИО"), optionalText(contact.getText()),
                requireText(phone.getText(), "Телефон"), optionalText(email.getText())));
    }
}
