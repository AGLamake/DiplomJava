package ru.vlsu.security.web;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.vlsu.security.model.User;
import ru.vlsu.security.security.Permissions;

import java.io.IOException;
import java.util.Optional;

/** Пускает к разделам только вошедших пользователей с правом на просмотр. */
@WebFilter("*.xhtml")
public class AuthFilter extends HttpFilter {

    enum Decision { ALLOW, LOGIN, HOME, FORBIDDEN, UNAVAILABLE }

    static final String RESOURCES = "/jakarta.faces.resource/";
    static final String FORBIDDEN_MESSAGE = "Недостаточно прав для просмотра раздела";

    static Decision decide(String path, User user, boolean dbReady) {
        if (path.startsWith(RESOURCES) || path.equals("/error.xhtml")) {
            return Decision.ALLOW;
        }
        if (!dbReady) {
            return Decision.UNAVAILABLE;
        }
        if (path.equals(Pages.LOGIN)) {
            return user == null ? Decision.ALLOW : Decision.HOME;
        }
        if (user == null) {
            return Decision.LOGIN;
        }
        if (path.equals("/index.xhtml")) {
            return Decision.HOME;
        }
        return Pages.section(path)
                .map(s -> Permissions.access(user.role(), s).canRead() ? Decision.ALLOW : Decision.FORBIDDEN)
                .orElse(Decision.ALLOW);
    }

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        AppResources app = CDI.current().select(AppResources.class).get();
        User user = Optional.ofNullable(req.getSession(false))
                .map(s -> CDI.current().select(UserSession.class).get().getUser())
                .orElse(null);
        switch (decide(req.getServletPath(), user, app.isReady())) {
            case ALLOW -> chain.doFilter(req, res);
            case LOGIN -> redirect(req, res, Pages.LOGIN);
            case HOME -> redirect(req, res, Pages.home(user.role()));
            case FORBIDDEN -> res.sendError(HttpServletResponse.SC_FORBIDDEN, FORBIDDEN_MESSAGE);
            case UNAVAILABLE -> res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, app.unavailableMessage());
        }
    }

    /** Для ajax-запросов (например, после истечения сессии) обычный 302 не сработает — нужен partial-response. */
    private static void redirect(HttpServletRequest req, HttpServletResponse res, String path) throws IOException {
        String url = req.getContextPath() + path;
        if ("partial/ajax".equals(req.getHeader("Faces-Request"))) {
            res.setContentType("text/xml");
            res.setCharacterEncoding("UTF-8");
            res.getWriter().write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><partial-response><redirect url=\""
                    + url + "\"></redirect></partial-response>");
        } else {
            res.sendRedirect(url);
        }
    }
}
