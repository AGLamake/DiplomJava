package ru.vlsu.security.ui.forms;

import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.stage.Window;
import ru.vlsu.security.dao.ObjectRefDao;
import ru.vlsu.security.dao.UserDao;
import ru.vlsu.security.model.InstallationRequest;
import ru.vlsu.security.model.ObjectRef;
import ru.vlsu.security.model.RequestStatus;
import ru.vlsu.security.model.User;
import ru.vlsu.security.security.Role;
import ru.vlsu.security.ui.table.EntityForm;
import ru.vlsu.security.ui.table.FormDialog;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static ru.vlsu.security.validation.FormParsers.optionalText;
import static ru.vlsu.security.validation.FormParsers.requireChoice;

public class InstallationRequestForm implements EntityForm<InstallationRequest> {

    private static final LocalTime WORK_START = LocalTime.of(9, 0);

    private final ObjectRefDao objects;
    private final UserDao users;
    private final User currentUser;

    public InstallationRequestForm(ObjectRefDao objects, UserDao users, User currentUser) {
        this.objects = objects;
        this.users = users;
        this.currentUser = currentUser;
    }

    @Override
    public Optional<InstallationRequest> show(Window owner, InstallationRequest initial) {
        FormDialog<InstallationRequest> f = new FormDialog<>(owner, initial == null ? "Новая заявка" : "Заявка на монтаж");
        ComboBox<ObjectRef> object = f.add("Объект", Forms.combo(objects.findAll()));
        ComboBox<User> engineer = f.add("Исполнитель", Forms.combo(users.findByRole(Role.ENGINEER)));
        engineer.setPromptText("Не назначен");
        DatePicker scheduled = f.add("Дата выезда", new DatePicker());
        ComboBox<RequestStatus> status = f.add("Статус", Forms.combo(RequestStatus.values()));
        status.setValue(RequestStatus.NEW);
        TextArea description = f.add("Описание", new TextArea());
        description.setPrefRowCount(3);
        long id = 0;
        long createdBy = currentUser.id();
        if (initial != null) {
            id = initial.id();
            createdBy = initial.createdBy();
            Forms.select(object, initial.objectId(), ObjectRef::id);
            Forms.select(engineer, initial.assignedTo(), User::id);
            if (initial.scheduledAt() != null) {
                scheduled.setValue(initial.scheduledAt().atZoneSameInstant(ZoneId.systemDefault()).toLocalDate());
            }
            status.setValue(initial.status());
            description.setText(initial.description());
        }
        long requestId = id;
        long author = createdBy;
        return f.show(() -> {
            ObjectRef o = requireChoice(object.getValue(), "Объект");
            User e = engineer.getValue();
            OffsetDateTime when = scheduled.getValue() == null ? null
                    : scheduled.getValue().atTime(WORK_START).atZone(ZoneId.systemDefault()).toOffsetDateTime();
            return new InstallationRequest(requestId, o.id(), o.label(), author, null, e == null ? null : e.id(),
                    e == null ? null : e.fullName(), null, when, null, requireChoice(status.getValue(), "Статус"),
                    optionalText(description.getText()));
        });
    }
}
