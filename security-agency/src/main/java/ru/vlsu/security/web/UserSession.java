package ru.vlsu.security.web;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import ru.vlsu.security.model.User;
import ru.vlsu.security.security.Access;
import ru.vlsu.security.security.Permissions;
import ru.vlsu.security.security.Section;

import java.io.Serializable;
import java.util.List;

/** Вошедший пользователь и его права (бывший MainController). */
@Named
@SessionScoped
public class UserSession implements Serializable {

    @Inject
    FacesUi ui;

    private User user;

    public void login(User user) {
        this.user = user;
    }

    public String logout() {
        user = null;
        ui.invalidateSession();
        return Pages.LOGIN + "?faces-redirect=true";
    }

    public User getUser() {
        return user;
    }

    public boolean isLoggedIn() {
        return user != null;
    }

    public Access access(Section section) {
        return user == null ? Access.NONE : Permissions.access(user.role(), section);
    }

    public List<Section> getSections() {
        return user == null ? List.of() : Permissions.visibleSections(user.role());
    }

    public String path(Section section) {
        return Pages.path(section);
    }

    public String getUserLabel() {
        return user == null ? "" : "Пользователь: " + user.fullName() + " (" + user.role().title() + ")";
    }
}
