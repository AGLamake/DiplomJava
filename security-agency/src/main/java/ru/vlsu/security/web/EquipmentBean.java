package ru.vlsu.security.web;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import ru.vlsu.security.dao.CrudDao;
import ru.vlsu.security.model.Equipment;
import ru.vlsu.security.model.EquipmentStatus;
import ru.vlsu.security.model.EquipmentType;
import ru.vlsu.security.model.ObjectRef;
import ru.vlsu.security.security.Section;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

import static ru.vlsu.security.validation.FormParsers.optionalText;
import static ru.vlsu.security.validation.FormParsers.requireChoice;

@Named
@ViewScoped
public class EquipmentBean extends CrudBean<Equipment, EquipmentBean.Form> {

    private List<ObjectRef> objects = List.of();
    private List<EquipmentType> types = List.of();

    @Override
    public String getTitle() {
        return "Оборудование";
    }

    @Override
    protected Section section() {
        return Section.EQUIPMENT;
    }

    @Override
    protected List<ColumnSpec<Equipment>> columns() {
        return List.of(
                new ColumnSpec<>("ID", Equipment::id, 60),
                new ColumnSpec<>("Объект", Equipment::objectName, 220),
                new ColumnSpec<>("Тип", Equipment::typeName, 240),
                new ColumnSpec<>("Серийный номер", Equipment::serialNumber, 160),
                new ColumnSpec<>("Установлено", e -> Formats.date(e.installationDate()), 110),
                new ColumnSpec<>("Статус", Equipment::status, 130));
    }

    @Override
    protected List<Equipment> load() {
        return ctx().equipment().findAll();
    }

    @Override
    protected long key(Equipment row) {
        return row.id();
    }

    @Override
    protected CrudDao<Equipment> dao() {
        return ctx().equipment();
    }

    @Override
    protected String formTitle(boolean creating) {
        return creating ? "Новое оборудование" : "Оборудование";
    }

    @Override
    protected Form newForm(Equipment e) {
        objects = ctx().objectRefs().findAll();
        types = ctx().equipmentTypes().findAll();
        Form f = new Form();
        if (e != null) {
            f.id = e.id();
            f.objectId = e.objectId();
            f.typeId = e.typeId();
            f.serialNumber = e.serialNumber();
            f.installationDate = e.installationDate();
            f.status = e.status();
        }
        return f;
    }

    @Override
    protected Equipment build(Form f) {
        ObjectRef o = requireChoice(Lookups.byId(objects, f.objectId, ObjectRef::id), "Объект");
        EquipmentType t = requireChoice(Lookups.byId(types, f.typeId, EquipmentType::id), "Тип");
        return new Equipment(f.id, o.id(), o.label(), t.id(), t.name(), optionalText(f.serialNumber),
                f.installationDate, requireChoice(f.status, "Статус"));
    }

    public List<ObjectRef> getObjects() {
        return objects;
    }

    public List<EquipmentType> getTypes() {
        return types;
    }

    public EquipmentStatus[] getStatuses() {
        return EquipmentStatus.values();
    }

    public static class Form implements Serializable {
        private long id;
        private Long objectId;
        private Long typeId;
        private String serialNumber;
        private LocalDate installationDate = LocalDate.now();
        private EquipmentStatus status = EquipmentStatus.INSTALLED;

        public Long getObjectId() {
            return objectId;
        }

        public void setObjectId(Long objectId) {
            this.objectId = objectId;
        }

        public Long getTypeId() {
            return typeId;
        }

        public void setTypeId(Long typeId) {
            this.typeId = typeId;
        }

        public String getSerialNumber() {
            return serialNumber;
        }

        public void setSerialNumber(String serialNumber) {
            this.serialNumber = serialNumber;
        }

        public LocalDate getInstallationDate() {
            return installationDate;
        }

        public void setInstallationDate(LocalDate installationDate) {
            this.installationDate = installationDate;
        }

        public EquipmentStatus getStatus() {
            return status;
        }

        public void setStatus(EquipmentStatus status) {
            this.status = status;
        }
    }
}
