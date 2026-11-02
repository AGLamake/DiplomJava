package ru.vlsu.security.web;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import ru.vlsu.security.dao.CrudDao;
import ru.vlsu.security.model.InstallationRequest;
import ru.vlsu.security.model.ObjectRef;
import ru.vlsu.security.model.RequestStatus;
import ru.vlsu.security.model.User;
import ru.vlsu.security.security.Role;
import ru.vlsu.security.security.Section;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static ru.vlsu.security.validation.FormParsers.optionalText;
import static ru.vlsu.security.validation.FormParsers.requireChoice;

@Named
@ViewScoped
public class InstallRequestsBean extends CrudBean<InstallationRequest, InstallRequestsBean.Form> {

    private static final LocalTime WORK_START = LocalTime.of(9, 0);

    private List<ObjectRef> objects = List.of();
    private List<User> engineers = List.of();

    @Override
    public String getTitle() {
        return "Заявки на монтаж";
    }

    @Override
    protected Section section() {
        return Section.INSTALL_REQUESTS;
    }

    @Override
    protected List<ColumnSpec<InstallationRequest>> columns() {
        return List.of(
                new ColumnSpec<>("ID", InstallationRequest::id, 60),
                new ColumnSpec<>("Объект", InstallationRequest::objectName, 200),
                new ColumnSpec<>("Создал", InstallationRequest::createdByName, 160),
                new ColumnSpec<>("Исполнитель", InstallationRequest::assignedToName, 160),
                new ColumnSpec<>("Создана", r -> Formats.dateTime(r.createdAt()), 140),
                new ColumnSpec<>("Выезд", r -> Formats.dateTime(r.scheduledAt()), 140),
                new ColumnSpec<>("Статус", InstallationRequest::status, 110),
                new ColumnSpec<>("Описание", InstallationRequest::description, 260));
    }

    @Override
    protected List<InstallationRequest> load() {
        return ctx().installRequests().findAll();
    }

    @Override
    protected long key(InstallationRequest row) {
        return row.id();
    }

    @Override
    protected CrudDao<InstallationRequest> dao() {
        return ctx().installRequests();
    }

    @Override
    protected String formTitle(boolean creating) {
        return creating ? "Новая заявка" : "Заявка на монтаж";
    }

    @Override
    protected Form newForm(InstallationRequest r) {
        objects = ctx().objectRefs().findAll();
        engineers = ctx().users().findByRole(Role.ENGINEER);
        Form f = new Form();
        f.createdBy = user().id();
        if (r != null) {
            f.id = r.id();
            f.createdBy = r.createdBy();
            f.objectId = r.objectId();
            f.engineerId = r.assignedTo();
            if (r.scheduledAt() != null) {
                f.scheduled = r.scheduledAt().atZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
            }
            f.status = r.status();
            f.description = r.description();
        }
        return f;
    }

    @Override
    protected InstallationRequest build(Form f) {
        ObjectRef o = requireChoice(Lookups.byId(objects, f.objectId, ObjectRef::id), "Объект");
        User e = Lookups.byId(engineers, f.engineerId, User::id);
        OffsetDateTime when = f.scheduled == null ? null
                : f.scheduled.atTime(WORK_START).atZone(ZoneId.systemDefault()).toOffsetDateTime();
        return new InstallationRequest(f.id, o.id(), o.label(), f.createdBy, null, e == null ? null : e.id(),
                e == null ? null : e.fullName(), null, when, null, requireChoice(f.status, "Статус"),
                optionalText(f.description));
    }

    public List<ObjectRef> getObjects() {
        return objects;
    }

    public List<User> getEngineers() {
        return engineers;
    }

    public RequestStatus[] getStatuses() {
        return RequestStatus.values();
    }

    public static class Form implements Serializable {
        private long id;
        private long createdBy;
        private Long objectId;
        private Long engineerId;
        private LocalDate scheduled;
        private RequestStatus status = RequestStatus.NEW;
        private String description;

        public Long getObjectId() {
            return objectId;
        }

        public void setObjectId(Long objectId) {
            this.objectId = objectId;
        }

        public Long getEngineerId() {
            return engineerId;
        }

        public void setEngineerId(Long engineerId) {
            this.engineerId = engineerId;
        }

        public LocalDate getScheduled() {
            return scheduled;
        }

        public void setScheduled(LocalDate scheduled) {
            this.scheduled = scheduled;
        }

        public RequestStatus getStatus() {
            return status;
        }

        public void setStatus(RequestStatus status) {
            this.status = status;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
