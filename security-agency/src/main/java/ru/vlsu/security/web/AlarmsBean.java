package ru.vlsu.security.web;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import ru.vlsu.security.model.AlarmEventView;
import ru.vlsu.security.model.DispatchResult;
import ru.vlsu.security.monitoring.SensorType;
import ru.vlsu.security.security.Section;

import java.util.List;
import java.util.Map;

import static ru.vlsu.security.validation.FormParsers.optionalText;
import static ru.vlsu.security.validation.FormParsers.requireChoice;

/** «Тревоги и выезды»: обновляется раз в 5 секунд. */
@Named
@ViewScoped
public class AlarmsBean extends TableBean<AlarmEventView> {

    private static final Map<String, String> EVENT_STATUS = Map.of(
            "new", "Новое", "checking", "Проверка", "confirmed", "Подтверждено", "false_alarm", "Ложная тревога",
            "dispatching", "Выезд ГБР", "closed", "Закрыто");
    private static final Map<String, String> PRIORITY = Map.of(
            "low", "Низкий", "medium", "Средний", "high", "Высокий", "critical", "Критический");

    private CompletionForm completion = new CompletionForm();

    @Override
    public String getTitle() {
        return "Тревоги и выезды";
    }

    @Override
    protected Section section() {
        return Section.ALARMS;
    }

    @Override
    protected List<ColumnSpec<AlarmEventView>> columns() {
        return List.of(
                new ColumnSpec<>("Время", a -> Formats.dateTime(a.receivedAt()), 140),
                new ColumnSpec<>("Объект", AlarmEventView::objectLabel, 250),
                new ColumnSpec<>("Событие", a -> SensorType.labelForCode(a.eventType()), 110),
                new ColumnSpec<>("Приоритет", a -> PRIORITY.getOrDefault(a.priority(), a.priority()), 100),
                new ColumnSpec<>("Статус", a -> EVENT_STATUS.getOrDefault(a.status(), a.status()), 120),
                new ColumnSpec<>("Оператор", AlarmEventView::operatorName, 140),
                new ColumnSpec<>("ГБР", AlarmEventView::teamName, 70),
                new ColumnSpec<>("Прибытие", a -> Formats.dateTime(a.arrivedAt()), 140),
                new ColumnSpec<>("Завершение", a -> Formats.dateTime(a.completedAt()), 140),
                new ColumnSpec<>("Результат", a -> a.result() == null ? null
                        : DispatchResult.fromCode(a.result()).title(), 160));
    }

    @Override
    protected List<AlarmEventView> load() {
        return ctx().alarms().findRecent(500);
    }

    @Override
    protected long key(AlarmEventView row) {
        return row.eventId();
    }

    public boolean isCanComplete() {
        AlarmEventView a = getSelected();
        return a != null && a.dispatchId() != null && a.completedAt() == null;
    }

    public boolean isCanMarkFalseAlarm() {
        AlarmEventView a = getSelected();
        return a != null && a.dispatchId() == null && "new".equals(a.status());
    }

    public void startComplete() {
        if (isCanWrite() && isCanComplete()) {
            completion = new CompletionForm();
            ui.callback("open", true);
        }
    }

    public void complete() {
        AlarmEventView a = getSelected();
        if (a == null || !isCanComplete()) {
            return;
        }
        if (change(() -> ctx().dispatches().complete(a.dispatchId(),
                requireChoice(completion.getResult(), "Результат"), optionalText(completion.getNotes()),
                user().id()))) {
            refresh();
            ui.callback("saved", true);
        }
    }

    public void markFalseAlarm() {
        AlarmEventView a = getSelected();
        if (a != null && isCanMarkFalseAlarm()
                && change(() -> ctx().alarms().markFalseAlarm(a.eventId(), user().id()))) {
            refresh();
        }
    }

    public CompletionForm getCompletion() {
        return completion;
    }
}
