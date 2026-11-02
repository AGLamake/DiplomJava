package ru.vlsu.security.web;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import ru.vlsu.security.security.Section;
import ru.vlsu.security.service.AuditService.AuditEntry;

import java.util.List;

@Named
@ViewScoped
public class AuditBean extends TableBean<AuditEntry> {

    @Override
    public String getTitle() {
        return "Журнал действий";
    }

    @Override
    protected Section section() {
        return Section.AUDIT;
    }

    @Override
    protected List<ColumnSpec<AuditEntry>> columns() {
        return List.of(
                new ColumnSpec<>("Время", a -> Formats.dateTime(a.createdAt()), 150),
                new ColumnSpec<>("Пользователь", AuditEntry::userLogin, 130),
                new ColumnSpec<>("Действие", AuditEntry::action, 150),
                new ColumnSpec<>("Сущность", AuditEntry::entityType, 110),
                new ColumnSpec<>("ID", AuditEntry::entityId, 70),
                new ColumnSpec<>("Подробности", AuditEntry::details, 420));
    }

    @Override
    protected List<AuditEntry> load() {
        return ctx().audit().findRecent(1000);
    }

    @Override
    protected long key(AuditEntry row) {
        return row.id();
    }
}
