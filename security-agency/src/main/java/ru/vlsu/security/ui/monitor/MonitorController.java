package ru.vlsu.security.ui.monitor;

import atlantafx.base.controls.ToggleSwitch;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import ru.vlsu.security.AppContext;
import ru.vlsu.security.dao.DataAccessException;
import ru.vlsu.security.dao.DbErrors;
import ru.vlsu.security.model.MonitoredObject;
import ru.vlsu.security.model.SecurityStatus;
import ru.vlsu.security.model.User;
import ru.vlsu.security.monitoring.JournalLine;
import ru.vlsu.security.monitoring.MonitoringListener;
import ru.vlsu.security.monitoring.MonitoringSession;
import ru.vlsu.security.monitoring.ObjectKind;
import ru.vlsu.security.monitoring.SensorType;
import ru.vlsu.security.security.Access;
import ru.vlsu.security.service.NoTeamAvailableException;
import ru.vlsu.security.ui.Dialogs;
import ru.vlsu.security.ui.Screens;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Экран мониторинга автомобиля или недвижимости (панели CarPANEL / NdvPANEL WinForms-версии). */
public final class MonitorController implements MonitoringListener {

    private final ObjectKind kind;
    private final AppContext ctx;
    private final User user;
    private final Access access;

    private final ComboBox<MonitoredObject> objectBox = new ComboBox<>();
    private final Label ownerLabel = new Label("—");
    private final Label statusLabel = new Label("Выберите объект");
    private final Label gbrLabel = new Label("Время до ГБР: —");
    private final Label mchsLabel = new Label("Время до МЧС: —");
    private final ObservableList<String> journal = FXCollections.observableArrayList();
    private final ListView<String> journalView = new ListView<>(journal);
    private final ToggleSwitch armedSwitch = new ToggleSwitch("Охрана включена");
    private final ToggleSwitch ignitionSwitch = new ToggleSwitch("Глушение двигателя");
    private final Button gbrButton = new Button("Отправить группу быстрого реагирования");
    private final Button mchsButton = new Button("Вызвать МЧС");
    private final List<Button> sensorButtons = new ArrayList<>();
    private final Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));

    private MonitoringSession session;
    private MonitoredObject current;
    private Long lastEventId;
    private boolean updatingSwitches;

    private MonitorController(ObjectKind kind, AppContext ctx, User user, Access access) {
        this.kind = kind;
        this.ctx = ctx;
        this.user = user;
        this.access = access;
    }

    public static Parent create(ObjectKind kind, AppContext ctx, User user, Access access) {
        return new MonitorController(kind, ctx, user, access).build();
    }

    private Parent build() {
        Pane scheme = new Pane();
        scheme.setPrefSize(592, 355);
        scheme.setMinSize(592, 355);
        String image = kind == ObjectKind.CAR ? "car.png" : "flat.png";
        scheme.getChildren().add(new ImageView(new Image(
                MonitorController.class.getResource("/ru/vlsu/security/ui/images/" + image).toExternalForm())));
        for (SensorPoint p : SensorPoint.forKind(kind)) {
            Button b = new Button(p.sensor().label());
            b.getStyleClass().add("sensor");
            b.setLayoutX(p.x() - 10);
            b.setLayoutY(p.y() - 11);
            b.setOnAction(e -> onSensor(p.sensor(), b));
            sensorButtons.add(b);
            scheme.getChildren().add(b);
        }
        scheme.setStyle("-fx-background-color: white; -fx-border-color: #CFD8DC;");

        armedSwitch.selectedProperty().addListener((obs, o, n) -> onArmedChanged(n));
        ignitionSwitch.selectedProperty().addListener((obs, o, n) -> onIgnitionChanged(n));
        gbrButton.setOnAction(e -> onRequestGbr());
        mchsButton.setOnAction(e -> onCallMchs());
        gbrButton.getStyleClass().add("accent");

        VBox controls = new VBox(10, armedSwitch);
        if (kind == ObjectKind.CAR) {
            controls.getChildren().add(ignitionSwitch);
        }
        HBox actionButtons = new HBox(8, gbrButton);
        if (kind == ObjectKind.NDV) {
            actionButtons.getChildren().add(mchsButton);
        }
        controls.getChildren().add(actionButtons);

        Label title = new Label(kind == ObjectKind.CAR ? "Мониторинг автомобиля" : "Мониторинг недвижимости");
        title.getStyleClass().add("screen-title");
        VBox left = new VBox(12, title, scheme, controls);

        objectBox.setPromptText("Выберите объект");
        objectBox.setMaxWidth(Double.MAX_VALUE);
        objectBox.valueProperty().addListener((obs, o, n) -> onObjectSelected(n));
        statusLabel.getStyleClass().add("status-label");
        HBox timers = new HBox(24, gbrLabel);
        if (kind == ObjectKind.NDV) {
            timers.getChildren().add(mchsLabel);
        }
        journalView.getStyleClass().add("journal");
        journalView.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                }
            };
            cell.setWrapText(true);
            cell.prefWidthProperty().bind(lv.widthProperty().subtract(24));
            return cell;
        });
        journalView.setPlaceholder(new Label("Событий нет"));
        VBox.setVgrow(journalView, Priority.ALWAYS);
        VBox right = new VBox(8, new Label("Объект"), objectBox, new HBox(6, new Label("Владелец:"), ownerLabel),
                new HBox(6, new Label("Статус:"), statusLabel), timers, new Label("Журнал событий"), journalView);
        HBox.setHgrow(right, Priority.ALWAYS);

        HBox content = new HBox(16, left, right);
        BorderPane root = new BorderPane(content);
        root.setPadding(new Insets(16));
        content.setAlignment(Pos.TOP_LEFT);

        reloadObjects();
        updateControls();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        return Screens.withLifecycle(root, new Screens.Lifecycle() {
            @Override
            public void onShown() {
                reloadObjects();
            }

            @Override
            public void dispose() {
                timeline.stop();
            }
        });
    }

    private void reloadObjects() {
        try {
            MonitoredObject selected = objectBox.getValue();
            List<MonitoredObject> list = ctx.alarms().listActive(kind);
            if (objectBox.getItems().equals(list)) {
                return;
            }
            objectBox.getItems().setAll(list);
            if (selected != null) {
                list.stream().filter(o -> o.objectId() == selected.objectId()).findFirst()
                        .ifPresent(o -> objectBox.getSelectionModel().select(o));
            }
        } catch (DataAccessException e) {
            Dialogs.error(DbErrors.message(e));
        }
    }

    private void onObjectSelected(MonitoredObject object) {
        if (object == null || (current != null && current.objectId() == object.objectId() && session != null)) {
            return;
        }
        current = object;
        lastEventId = null;
        journal.clear();
        boolean armed = object.securityStatus() != SecurityStatus.DISARMED;
        session = new MonitoringSession(kind, object.label(), user.fullName(), object.ownerName(), armed,
                ctx.config().dispatchEtaSeconds(), ctx.config().mchsEtaSeconds(), this);
        ownerLabel.setText(object.ownerName());
        statusLabel.setText(object.securityStatus().title());
        gbrLabel.setText("Время до ГБР: —");
        mchsLabel.setText("Время до МЧС: —");
        updatingSwitches = true;
        armedSwitch.setSelected(armed);
        ignitionSwitch.setSelected(false);
        updatingSwitches = false;
        updateControls();
    }

    private void updateControls() {
        boolean write = access.canWrite();
        boolean selected = session != null;
        boolean armed = selected && session.armed();
        sensorButtons.forEach(b -> b.setDisable(!write || !armed));
        armedSwitch.setDisable(!write || !selected);
        ignitionSwitch.setDisable(!write || !armed);
        gbrButton.setDisable(!write || !selected);
        mchsButton.setDisable(!write || !selected);
    }

    private void onSensor(SensorType sensor, Button button) {
        if (session != null && session.trigger(sensor)) {
            button.getStyleClass().add("fired");
            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(e -> button.getStyleClass().remove("fired"));
            pause.play();
        }
    }

    private void onArmedChanged(boolean armed) {
        if (updatingSwitches || session == null) {
            return;
        }
        if (db(() -> ctx.alarms().setSecurityStatus(current.objectId(),
                armed ? SecurityStatus.ARMED : SecurityStatus.DISARMED, user.id()))) {
            session.setArmed(armed);
        }
        updateControls();
    }

    private void onIgnitionChanged(boolean blocked) {
        if (updatingSwitches || session == null) {
            return;
        }
        if (db(() -> ctx.alarms().logIgnition(current.objectId(), blocked, user.id()))) {
            session.setIgnitionBlocked(blocked);
        }
    }

    private void onRequestGbr() {
        if (session != null) {
            session.requestGbr();
        }
    }

    private void onCallMchs() {
        if (session != null && session.callMchs()) {
            db(() -> ctx.alarms().logMchs(current.objectId(), user.id()));
        }
    }

    private void tick() {
        if (session != null) {
            session.tick();
            if (session.mchsSecondsLeft() > 0) {
                mchsLabel.setText("Время до МЧС: " + minutes(session.mchsSecondsLeft()) + " мин");
            } else {
                mchsLabel.setText("Время до МЧС: —");
            }
        }
    }

    private static int minutes(int seconds) {
        return (seconds + 59) / 60;
    }

    private static boolean db(Runnable r) {
        try {
            r.run();
            return true;
        } catch (DataAccessException e) {
            Dialogs.error(DbErrors.message(e));
            return false;
        }
    }

    // --- MonitoringListener ---

    @Override
    public void onJournal(String message) {
        journal.add(JournalLine.format(LocalDateTime.now(), message));
        journalView.scrollTo(journal.size() - 1);
    }

    @Override
    public void onStatus(String status) {
        statusLabel.setText(status);
    }

    @Override
    public void onAlarm(SensorType sensor, String message) {
        try {
            lastEventId = ctx.alarms().recordAlarm(current.objectId(), sensor, user.id(), message);
        } catch (DataAccessException e) {
            Dialogs.error(DbErrors.message(e));
        }
    }

    @Override
    public boolean onDispatchRequested() {
        try {
            ctx.dispatches().dispatch(current.objectId(), lastEventId, user.id());
            return true;
        } catch (NoTeamAvailableException e) {
            return false;
        } catch (DataAccessException e) {
            Dialogs.error(DbErrors.message(e));
            return false;
        }
    }

    @Override
    public void onGbrTimer(int secondsLeft) {
        if (secondsLeft == 0) {
            gbrLabel.setText("Время до ГБР: —");
            try {
                Optional<Long> dispatch = ctx.dispatches().activeDispatchForObject(current.objectId());
                dispatch.ifPresent(ctx.dispatches()::markArrived);
            } catch (DataAccessException e) {
                Dialogs.error(DbErrors.message(e));
            }
        } else {
            gbrLabel.setText("Время до ГБР: " + minutes(secondsLeft) + " мин");
        }
    }
}
