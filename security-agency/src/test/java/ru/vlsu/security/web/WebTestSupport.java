package ru.vlsu.security.web;

import ru.vlsu.security.AppContext;
import ru.vlsu.security.TestDb;
import ru.vlsu.security.config.AppConfig;
import ru.vlsu.security.model.User;
import ru.vlsu.security.security.PasswordHasher;
import ru.vlsu.security.security.Role;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Бины без контейнера: зависимости подставляются вручную, сообщения и ответы ajax записываются. */
final class WebTestSupport {

    private WebTestSupport() {
    }

    static final class RecordingUi extends FacesUi {
        final List<String> infos = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();
        final List<String> errors = new ArrayList<>();
        final Map<String, Object> callbacks = new HashMap<>();
        final List<String> updated = new ArrayList<>();

        @Override
        public void info(String message) {
            infos.add(message);
        }

        @Override
        public void warn(String message) {
            warnings.add(message);
        }

        @Override
        public void error(String message) {
            errors.add(message);
        }

        @Override
        public void callback(String name, Object value) {
            callbacks.put(name, value);
        }

        @Override
        public void update(String... clientIds) {
            updated.addAll(List.of(clientIds));
        }

        @Override
        public void renewSession() {
        }

        @Override
        public void invalidateSession() {
        }
    }

    static AppContext freshContext(int etaSeconds) {
        TestDb.reset();
        return AppContext.create(TestDb.dataSource(), new AppConfig(etaSeconds, etaSeconds));
    }

    static User user(AppContext ctx, String login, String fullName, Role role) {
        long id = ctx.users().insert(login, PasswordHasher.hash("123456".toCharArray()), fullName, role, true);
        return ctx.users().findById(id).orElseThrow();
    }

    static UserSession session(User user) {
        UserSession s = new UserSession();
        s.ui = new RecordingUi();
        s.login(user);
        return s;
    }

    /** Подставляет зависимости и вызывает @PostConstruct. */
    static <B extends TableBean<?>> B wire(B bean, AppContext ctx, User user, RecordingUi ui) {
        bean.app = AppResources.forTests(ctx);
        bean.session = session(user);
        bean.ui = ui;
        bean.init();
        return bean;
    }

    static <T> T rowWhere(TableBean<T> bean, java.util.function.Predicate<T> p) {
        return bean.getRows().stream().filter(p).findFirst().orElseThrow();
    }
}
