package ru.vlsu.security.ui;

import javafx.scene.Parent;
import ru.vlsu.security.AppContext;
import ru.vlsu.security.dao.CrudDao;
import ru.vlsu.security.model.User;
import ru.vlsu.security.security.Access;
import ru.vlsu.security.service.AuditService.AuditEntry;
import ru.vlsu.security.service.UserAdminService;
import ru.vlsu.security.ui.forms.UserForm;
import ru.vlsu.security.ui.table.ColumnSpec;
import ru.vlsu.security.ui.table.TableSpec;
import ru.vlsu.security.ui.table.TableViewController;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Разделы администратора: «Пользователи» и «Журнал действий». */
final class AdminScreens {

    static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private AdminScreens() {
    }

    static String format(OffsetDateTime t) {
        return t == null ? null : t.atZoneSameInstant(java.time.ZoneId.systemDefault()).format(DATE_TIME);
    }

    static Parent users(AppContext ctx, User actor, Access access) {
        UserAdminService service = new UserAdminService(ctx.users(), ctx.audit());
        CrudDao<User> dao = new CrudDao<>() {
            @Override
            public List<User> findAll() {
                return ctx.users().findAll();
            }

            @Override
            public long insert(User item) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void update(User item) {
                service.update(actor, item);
            }

            @Override
            public void delete(long id) {
                throw new UnsupportedOperationException();
            }
        };
        TableSpec<User> spec = TableSpec.of("Пользователи", List.of(
                        new ColumnSpec<User>("Логин", User::login, 140),
                        new ColumnSpec<>("ФИО", User::fullName, 260),
                        new ColumnSpec<>("Роль", User::role, 150),
                        new ColumnSpec<>("Активен", u -> u.active() ? "Да" : "Нет", 90),
                        new ColumnSpec<>("Телефон", User::phone, 150),
                        new ColumnSpec<>("E-mail", User::email, 200)),
                        dao, new UserForm(), User::id)
                .withoutAdd().withoutDelete()
                .withAction(new TableSpec.RowAction<>("Сбросить пароль", u -> true, (owner, u) ->
                    UserForm.askNewPassword(owner, u)
                            .ifPresent(p -> service.resetPassword(actor, u.id(), p))));
        return TableViewController.create(spec, access);
    }

    static Parent audit(AppContext ctx, Access access) {
        CrudDao<AuditEntry> dao = new CrudDao<>() {
            @Override
            public List<AuditEntry> findAll() {
                return ctx.audit().findRecent(1000);
            }

            @Override
            public long insert(AuditEntry item) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void update(AuditEntry item) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void delete(long id) {
                throw new UnsupportedOperationException();
            }
        };
        TableSpec<AuditEntry> spec = TableSpec.of("Журнал действий", List.of(
                new ColumnSpec<AuditEntry>("Время", a -> format(a.createdAt()), 150),
                new ColumnSpec<>("Пользователь", AuditEntry::userLogin, 130),
                new ColumnSpec<>("Действие", AuditEntry::action, 150),
                new ColumnSpec<>("Сущность", AuditEntry::entityType, 110),
                new ColumnSpec<>("ID", AuditEntry::entityId, 70),
                new ColumnSpec<>("Подробности", AuditEntry::details, 420)),
                dao, null, AuditEntry::id).withoutAdd().withoutDelete();
        return TableViewController.create(spec, Access.READ);
    }
}
