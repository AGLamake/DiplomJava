package ru.vlsu.security.web;

import jakarta.inject.Inject;
import ru.vlsu.security.dao.DataAccessException;
import ru.vlsu.security.dao.DbErrors;
import ru.vlsu.security.model.MonitoredObject;
import ru.vlsu.security.model.SecurityStatus;
import ru.vlsu.security.monitoring.JournalLine;
import ru.vlsu.security.monitoring.MonitoringListener;
import ru.vlsu.security.monitoring.MonitoringSession;
import ru.vlsu.security.monitoring.ObjectKind;
import ru.vlsu.security.monitoring.SensorType;
import ru.vlsu.security.security.Section;
import ru.vlsu.security.service.NoTeamAvailableException;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Мониторинг автомобиля или недвижимости (бывший MonitorController).
 * Живёт всю сессию, поэтому таймеры ГБР/МЧС не сбрасываются при переходе между разделами.
 * Таймер отсчитывается по реальному времени: poll() «догоняет» все прошедшие секунды.
 */
public abstract class MonitorBean implements MonitoringListener, Serializable {

    /** Не догонять больше часа (например, после долгого простоя вкладки). */
    private static final int MAX_CATCH_UP_SECONDS = 3600;

    @Inject
    AppResources app;
    @Inject
    UserSession userSession;
    @Inject
    FacesUi ui;

    private final ObjectKind kind;
    private List<MonitoredObject> objects = List.of();
    private Long objectId;
    private MonitoredObject current;
    private MonitoringSession session;
    private final List<String> journal = new ArrayList<>();
    private String status = "Выберите объект";
    private Long lastEventId;
    private boolean armed;
    private boolean ignitionBlocked;
    private long lastTickMillis;
    private String shownState = "";

    protected MonitorBean(ObjectKind kind) {
        this.kind = kind;
    }

    /** Текущее время; переопределяется в тестах. */
    long now() {
        return System.currentTimeMillis();
    }

    private Section section() {
        return kind == ObjectKind.CAR ? Section.CAR_MONITOR : Section.NDV_MONITOR;
    }

    private boolean canWrite() {
        return userSession.access(section()).canWrite();
    }

    /** Раздел доступен пользователю — для фонового отсчёта таймеров из шаблона страницы. */
    public boolean isAvailable() {
        return userSession.access(section()).canRead();
    }

    /** Список объектов под мониторингом; вызывается при открытии страницы. */
    public void reload() {
        try {
            objects = app.ctx().alarms().listActive(kind);
        } catch (DataAccessException e) {
            ui.error(DbErrors.message(e));
        }
    }

    /** Выбор объекта в списке. */
    public void select() {
        MonitoredObject object = objects.stream().filter(o -> objectId != null && o.objectId() == objectId)
                .findFirst().orElse(null);
        if (object == null || (current != null && current.objectId() == object.objectId() && session != null)) {
            return;
        }
        current = object;
        lastEventId = null;
        journal.clear();
        armed = object.securityStatus() != SecurityStatus.DISARMED;
        ignitionBlocked = false;
        session = new MonitoringSession(kind, object.label(), userSession.getUser().fullName(), object.ownerName(),
                armed, app.ctx().config().dispatchEtaSeconds(), app.ctx().config().mchsEtaSeconds(), this);
        status = object.securityStatus().title();
        lastTickMillis = now();
    }

    /** Нажатие кнопки датчика на схеме; если датчик сработал, кнопка подсвечивается (args.fired). */
    public void trigger(SensorType sensor) {
        if (canWrite() && session != null && session.trigger(sensor)) {
            ui.callback("fired", true);
        }
    }

    public void armedChanged() {
        if (session == null || !canWrite()) {
            armed = session != null && session.armed();
            return;
        }
        boolean value = armed;
        if (db(() -> app.ctx().alarms().setSecurityStatus(current.objectId(),
                value ? SecurityStatus.ARMED : SecurityStatus.DISARMED, userSession.getUser().id()))) {
            session.setArmed(value);
        }
        armed = session.armed();
    }

    public void ignitionChanged() {
        if (session == null || !canWrite()) {
            ignitionBlocked = session != null && session.ignitionBlocked();
            return;
        }
        boolean value = ignitionBlocked;
        if (db(() -> app.ctx().alarms().logIgnition(current.objectId(), value, userSession.getUser().id()))) {
            session.setIgnitionBlocked(value);
        }
        ignitionBlocked = session.ignitionBlocked();
    }

    public void requestGbr() {
        if (session != null && canWrite()) {
            session.requestGbr();
        }
    }

    public void callMchs() {
        if (session != null && canWrite() && session.callMchs()) {
            db(() -> app.ctx().alarms().logMchs(current.objectId(), userSession.getUser().id()));
        }
    }

    /** Отсчёт таймеров за прошедшее с прошлого вызова время. */
    public void catchUp() {
        if (session == null) {
            return;
        }
        long elapsed = (now() - lastTickMillis) / 1000;
        lastTickMillis += elapsed * 1000;
        for (long i = 0; i < Math.min(elapsed, MAX_CATCH_UP_SECONDS); i++) {
            session.tick();
        }
    }

    /** Ежесекундный опрос со страницы: перерисовывает только то, что изменилось. */
    public void poll() {
        catchUp();
        String state = status + '|' + getGbrText() + '|' + getMchsText() + '|' + journal.size();
        if (!state.equals(shownState)) {
            shownState = state;
            ui.update("monitorForm:info", "monitorForm:journal");
        }
    }

    private boolean db(Runnable r) {
        try {
            r.run();
            return true;
        } catch (DataAccessException e) {
            ui.error(DbErrors.message(e));
            return false;
        }
    }

    private static String minutesText(int seconds) {
        return seconds > 0 ? (seconds + 59) / 60 + " мин" : "—";
    }

    // --- MonitoringListener ---

    @Override
    public void onJournal(String message) {
        journal.add(JournalLine.format(LocalDateTime.now(), message));
    }

    @Override
    public void onStatus(String status) {
        this.status = status;
    }

    @Override
    public void onAlarm(SensorType sensor, String message) {
        try {
            lastEventId = app.ctx().alarms().recordAlarm(current.objectId(), sensor, userSession.getUser().id(),
                    message);
        } catch (DataAccessException e) {
            ui.error(DbErrors.message(e));
        }
    }

    @Override
    public boolean onDispatchRequested() {
        try {
            app.ctx().dispatches().dispatch(current.objectId(), lastEventId, userSession.getUser().id());
            return true;
        } catch (NoTeamAvailableException e) {
            return false;
        } catch (DataAccessException e) {
            ui.error(DbErrors.message(e));
            return false;
        }
    }

    @Override
    public void onGbrTimer(int secondsLeft) {
        if (secondsLeft == 0) {
            try {
                Optional<Long> dispatch = app.ctx().dispatches().activeDispatchForObject(current.objectId());
                dispatch.ifPresent(app.ctx().dispatches()::markArrived);
            } catch (DataAccessException e) {
                ui.error(DbErrors.message(e));
            }
        }
    }

    // --- Свойства страницы ---

    public String getTitle() {
        return kind == ObjectKind.CAR ? "Мониторинг автомобиля" : "Мониторинг недвижимости";
    }

    public boolean isCar() {
        return kind == ObjectKind.CAR;
    }

    public String getScheme() {
        return kind == ObjectKind.CAR ? "images/car.png" : "images/flat.png";
    }

    public List<SensorPoint> getSensors() {
        return SensorPoint.forKind(kind);
    }

    public List<MonitoredObject> getObjects() {
        return objects;
    }

    public Long getObjectId() {
        return objectId;
    }

    public void setObjectId(Long objectId) {
        this.objectId = objectId;
    }

    public String getOwnerName() {
        return current == null ? "—" : current.ownerName();
    }

    public String getStatus() {
        return status;
    }

    public String getGbrText() {
        return session == null ? "—" : minutesText(session.gbrSecondsLeft());
    }

    public String getMchsText() {
        return session == null ? "—" : minutesText(session.mchsSecondsLeft());
    }

    public List<String> getJournal() {
        return journal;
    }

    public boolean isArmed() {
        return armed;
    }

    public void setArmed(boolean armed) {
        this.armed = armed;
    }

    public boolean isIgnitionBlocked() {
        return ignitionBlocked;
    }

    public void setIgnitionBlocked(boolean ignitionBlocked) {
        this.ignitionBlocked = ignitionBlocked;
    }

    public boolean isSensorsDisabled() {
        return !canWrite() || session == null || !session.armed();
    }

    public boolean isArmedDisabled() {
        return !canWrite() || session == null;
    }

    public boolean isIgnitionDisabled() {
        return isSensorsDisabled();
    }

    public boolean isActionsDisabled() {
        return !canWrite() || session == null;
    }
}
