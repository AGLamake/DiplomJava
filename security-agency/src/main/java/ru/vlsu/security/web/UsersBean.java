package ru.vlsu.security.web;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import ru.vlsu.security.dao.CrudDao;
import ru.vlsu.security.model.User;
import ru.vlsu.security.security.Role;
import ru.vlsu.security.security.Section;
import ru.vlsu.security.service.UserAdminService;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import static ru.vlsu.security.validation.FormParsers.optionalText;
import static ru.vlsu.security.validation.FormParsers.requireChoice;
import static ru.vlsu.security.validation.FormParsers.requireText;

/** Учётные записи создаются только регистрацией; администратор их изменяет и сбрасывает пароль. */
@Named
@ViewScoped
public class UsersBean extends CrudBean<User, UsersBean.Form> {

    private String newPassword;
    private String newPasswordRepeat;

    @Override
    public String getTitle() {
        return "Пользователи";
    }

    @Override
    protected Section section() {
        return Section.USERS;
    }

    @Override
    protected List<ColumnSpec<User>> columns() {
        return List.of(
                new ColumnSpec<>("Логин", User::login, 140),
                new ColumnSpec<>("ФИО", User::fullName, 260),
                new ColumnSpec<>("Роль", User::role, 150),
                new ColumnSpec<>("Активен", u -> u.active() ? "Да" : "Нет", 90),
                new ColumnSpec<>("Телефон", User::phone, 150),
                new ColumnSpec<>("E-mail", User::email, 200));
    }

    @Override
    protected List<User> load() {
        return ctx().users().findAll();
    }

    @Override
    protected long key(User row) {
        return row.id();
    }

    @Override
    protected CrudDao<User> dao() {
        throw new UnsupportedOperationException();
    }

    private UserAdminService service() {
        return new UserAdminService(ctx().users(), ctx().audit());
    }

    @Override
    protected void update(User item) {
        service().update(user(), item);
    }

    @Override
    public boolean isCanAdd() {
        return false;
    }

    @Override
    public boolean isCanDelete() {
        return false;
    }

    @Override
    protected String formTitle(boolean creating) {
        return "Пользователь";
    }

    @Override
    protected Form newForm(User u) {
        Form f = new Form();
        f.id = u.id();
        f.login = u.login();
        f.fullName = u.fullName();
        f.phone = u.phone();
        f.email = u.email();
        f.role = u.role();
        f.active = u.active();
        return f;
    }

    @Override
    protected User build(Form f) {
        return new User(f.id, f.login, requireText(f.fullName, "ФИО"), requireChoice(f.role, "Роль"), f.active,
                optionalText(f.phone), optionalText(f.email));
    }

    public Role[] getRoles() {
        return Role.values();
    }

    public void startResetPassword() {
        newPassword = null;
        newPasswordRepeat = null;
        if (isCanWrite() && getSelected() != null) {
            ui.callback("open", true);
        }
    }

    public void resetPassword() {
        User target = getSelected();
        if (target == null) {
            return;
        }
        boolean done = change(() -> {
            if (!Objects.equals(newPassword, newPasswordRepeat)) {
                throw new IllegalArgumentException("Пароли отличаются!");
            }
            char[] password = newPassword == null ? new char[0] : newPassword.toCharArray();
            service().resetPassword(user(), target.id(), password);
        });
        newPassword = null;
        newPasswordRepeat = null;
        if (done) {
            ui.info("Пароль пользователя " + target.login() + " изменён");
            ui.callback("saved", true);
        }
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getNewPasswordRepeat() {
        return newPasswordRepeat;
    }

    public void setNewPasswordRepeat(String newPasswordRepeat) {
        this.newPasswordRepeat = newPasswordRepeat;
    }

    public static class Form implements Serializable {
        private long id;
        private String login;
        private String fullName;
        private String phone;
        private String email;
        private Role role;
        private boolean active;

        public String getLogin() {
            return login;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Role getRole() {
            return role;
        }

        public void setRole(Role role) {
            this.role = role;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }
}
