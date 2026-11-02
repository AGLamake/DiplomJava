package ru.vlsu.security.web;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;
import org.primefaces.PrimeFaces;

/**
 * Сообщения пользователю и ответы на ajax-запросы (вместо Dialogs из JavaFX-версии).
 * Бины работают только через этот класс, поэтому в тестах его заменяет запись вызовов.
 */
@ApplicationScoped
public class FacesUi {

    public void info(String message) {
        add(FacesMessage.SEVERITY_INFO, message);
    }

    public void warn(String message) {
        add(FacesMessage.SEVERITY_WARN, message);
    }

    public void error(String message) {
        add(FacesMessage.SEVERITY_ERROR, message);
    }

    /** Параметр для oncomplete на странице (args.name). */
    public void callback(String name, Object value) {
        PrimeFaces.current().ajax().addCallbackParam(name, value);
    }

    /** Перерисовать компоненты в ответ на текущий ajax-запрос. */
    public void update(String... clientIds) {
        PrimeFaces.current().ajax().update(clientIds);
    }

    /** Новый идентификатор сессии после входа (защита от фиксации сессии). */
    public void renewSession() {
        ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest()).changeSessionId();
    }

    public void invalidateSession() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
    }

    private static void add(FacesMessage.Severity severity, String message) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, message, null));
    }
}
