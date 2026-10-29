package ru.vlsu.security.ui;

import javafx.scene.Parent;
import ru.vlsu.security.AppContext;
import ru.vlsu.security.dao.CrudDao;
import ru.vlsu.security.model.AlarmEventView;
import ru.vlsu.security.model.DispatchResult;
import ru.vlsu.security.model.DispatchView;
import ru.vlsu.security.model.User;
import ru.vlsu.security.monitoring.SensorType;
import ru.vlsu.security.security.Access;
import ru.vlsu.security.ui.forms.CompleteDispatchDialog;
import ru.vlsu.security.ui.table.ColumnSpec;
import ru.vlsu.security.ui.table.TableSpec;
import ru.vlsu.security.ui.table.TableViewController;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** «Тревоги и выезды» (оператор) и «Мои выезды» (сотрудник ГБР). */
final class AlarmScreens {

    private static final int REFRESH_SECONDS = 5;

    private static final Map<String, String> EVENT_STATUS = Map.of(
            "new", "Новое", "checking", "Проверка", "confirmed", "Подтверждено", "false_alarm", "Ложная тревога",
            "dispatching", "Выезд ГБР", "closed", "Закрыто");
    private static final Map<String, String> PRIORITY = Map.of(
            "low", "Низкий", "medium", "Средний", "high", "Высокий", "critical", "Критический");

    private AlarmScreens() {
    }

    static Parent alarms(AppContext ctx, User user, Access access) {
        TableSpec<AlarmEventView> spec = TableSpec.of("Тревоги и выезды", List.of(
                        new ColumnSpec<AlarmEventView>("Время", a -> AdminScreens.format(a.receivedAt()), 140),
                        new ColumnSpec<>("Объект", AlarmEventView::objectLabel, 250),
                        new ColumnSpec<>("Событие", a -> SensorType.labelForCode(a.eventType()), 110),
                        new ColumnSpec<>("Приоритет", a -> PRIORITY.getOrDefault(a.priority(), a.priority()), 100),
                        new ColumnSpec<>("Статус", a -> EVENT_STATUS.getOrDefault(a.status(), a.status()), 120),
                        new ColumnSpec<>("Оператор", AlarmEventView::operatorName, 140),
                        new ColumnSpec<>("ГБР", AlarmEventView::teamName, 70),
                        new ColumnSpec<>("Прибытие", a -> AdminScreens.format(a.arrivedAt()), 140),
                        new ColumnSpec<>("Завершение", a -> AdminScreens.format(a.completedAt()), 140),
                        new ColumnSpec<>("Результат", a -> resultTitle(a.result()), 160)),
                        readOnly(() -> ctx.alarms().findRecent(500)), null, AlarmEventView::eventId)
                .withoutAdd().withoutDelete()
                .withAction(new TableSpec.RowAction<>("Завершить выезд",
                        a -> a.dispatchId() != null && a.completedAt() == null,
                        (owner, a) -> CompleteDispatchDialog.show(owner).ifPresent(r ->
                                ctx.dispatches().complete(a.dispatchId(), r.result(), r.notes(), user.id()))))
                .withAction(new TableSpec.RowAction<>("Ложная тревога",
                        a -> a.dispatchId() == null && "new".equals(a.status()),
                        (owner, a) -> {
                            if (Dialogs.confirm("Отметить событие как ложную тревогу?")) {
                                ctx.alarms().markFalseAlarm(a.eventId(), user.id());
                            }
                        }));
        return TableViewController.create(spec, access, REFRESH_SECONDS);
    }

    static Parent myDispatches(AppContext ctx, User user, Access access) {
        TableSpec<DispatchView> spec = TableSpec.of("Мои выезды", List.of(
                        new ColumnSpec<DispatchView>("Выезд", DispatchView::dispatchId, 70),
                        new ColumnSpec<>("Группа", DispatchView::teamName, 90),
                        new ColumnSpec<>("Объект", DispatchView::objectLabel, 320),
                        new ColumnSpec<>("Событие", d -> SensorType.labelForCode(d.eventType()), 130),
                        new ColumnSpec<>("Отправлены", d -> AdminScreens.format(d.dispatchedAt()), 150),
                        new ColumnSpec<>("Прибыли", d -> AdminScreens.format(d.arrivedAt()), 150)),
                        readOnly(() -> ctx.dispatches().findActiveForMember(user.id())), null, DispatchView::dispatchId)
                .withoutAdd().withoutDelete()
                .withAction(new TableSpec.RowAction<>("Прибыли на место", d -> d.arrivedAt() == null,
                        (owner, d) -> ctx.dispatches().markArrived(d.dispatchId())))
                .withAction(new TableSpec.RowAction<>("Завершить выезд", d -> true,
                        (owner, d) -> CompleteDispatchDialog.show(owner).ifPresent(r ->
                                ctx.dispatches().complete(d.dispatchId(), r.result(), r.notes(), user.id()))));
        return TableViewController.create(spec, access, REFRESH_SECONDS);
    }

    private static String resultTitle(String code) {
        return code == null ? null : DispatchResult.fromCode(code).title();
    }

    /** Источник данных только для чтения: изменения идут через действия над строками. */
    static <T> CrudDao<T> readOnly(Supplier<List<T>> source) {
        return new CrudDao<>() {
            @Override
            public List<T> findAll() {
                return source.get();
            }

            @Override
            public long insert(T item) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void update(T item) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void delete(long id) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
