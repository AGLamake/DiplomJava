package ru.vlsu.security.ui.forms;

import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import ru.vlsu.security.dao.EquipmentTypeDao;
import ru.vlsu.security.dao.ObjectRefDao;
import ru.vlsu.security.model.Equipment;
import ru.vlsu.security.model.EquipmentStatus;
import ru.vlsu.security.model.EquipmentType;
import ru.vlsu.security.model.ObjectRef;
import ru.vlsu.security.ui.table.EntityForm;
import ru.vlsu.security.ui.table.FormDialog;

import java.time.LocalDate;
import java.util.Optional;

import static ru.vlsu.security.validation.FormParsers.optionalText;
import static ru.vlsu.security.validation.FormParsers.requireChoice;

public class EquipmentForm implements EntityForm<Equipment> {

    private final ObjectRefDao objects;
    private final EquipmentTypeDao types;

    public EquipmentForm(ObjectRefDao objects, EquipmentTypeDao types) {
        this.objects = objects;
        this.types = types;
    }

    @Override
    public Optional<Equipment> show(Window owner, Equipment initial) {
        FormDialog<Equipment> f = new FormDialog<>(owner, initial == null ? "Новое оборудование" : "Оборудование");
        ComboBox<ObjectRef> object = f.add("Объект", Forms.combo(objects.findAll()));
        ComboBox<EquipmentType> type = f.add("Тип", Forms.combo(types.findAll()));
        TextField serial = f.add("Серийный номер", new TextField());
        DatePicker installed = f.add("Дата установки", new DatePicker(LocalDate.now()));
        ComboBox<EquipmentStatus> status = f.add("Статус", Forms.combo(EquipmentStatus.values()));
        status.setValue(EquipmentStatus.INSTALLED);
        long id = 0;
        if (initial != null) {
            id = initial.id();
            Forms.select(object, initial.objectId(), ObjectRef::id);
            Forms.select(type, initial.typeId(), EquipmentType::id);
            serial.setText(initial.serialNumber());
            installed.setValue(initial.installationDate());
            status.setValue(initial.status());
        }
        long equipmentId = id;
        return f.show(() -> {
            ObjectRef o = requireChoice(object.getValue(), "Объект");
            EquipmentType t = requireChoice(type.getValue(), "Тип");
            return new Equipment(equipmentId, o.id(), o.label(), t.id(), t.name(), optionalText(serial.getText()),
                    installed.getValue(), requireChoice(status.getValue(), "Статус"));
        });
    }
}
