package ru.vlsu.security.web;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import ru.vlsu.security.model.DispatchView;
import ru.vlsu.security.monitoring.SensorType;
import ru.vlsu.security.security.Section;

import java.util.List;

import static ru.vlsu.security.validation.FormParsers.optionalText;
import static ru.vlsu.security.validation.FormParsers.requireChoice;

/** «Мои выезды» сотрудника ГБР: обновляется раз в 5 секунд. */
@Named
@ViewScoped
public class MyDispatchesBean extends TableBean<DispatchView> {

    private CompletionForm completion = new CompletionForm();

    @Override
    public String getTitle() {
        return "Мои выезды";
    }

    @Override
    protected Section section() {
        return Section.MY_DISPATCHES;
    }

    @Override
    protected List<ColumnSpec<DispatchView>> columns() {
        return List.of(
                new ColumnSpec<>("Выезд", DispatchView::dispatchId, 70),
                new ColumnSpec<>("Группа", DispatchView::teamName, 90),
                new ColumnSpec<>("Объект", DispatchView::objectLabel, 320),
                new ColumnSpec<>("Событие", d -> SensorType.labelForCode(d.eventType()), 130),
                new ColumnSpec<>("Отправлены", d -> Formats.dateTime(d.dispatchedAt()), 150),
                new ColumnSpec<>("Прибыли", d -> Formats.dateTime(d.arrivedAt()), 150));
    }

    @Override
    protected List<DispatchView> load() {
        return ctx().dispatches().findActiveForMember(user().id());
    }

    @Override
    protected long key(DispatchView row) {
        return row.dispatchId();
    }

    public boolean isCanMarkArrived() {
        return getSelected() != null && getSelected().arrivedAt() == null;
    }

    public void markArrived() {
        DispatchView d = getSelected();
        if (d != null && isCanMarkArrived() && change(() -> ctx().dispatches().markArrived(d.dispatchId()))) {
            refresh();
        }
    }

    public void startComplete() {
        if (isCanWrite() && getSelected() != null) {
            completion = new CompletionForm();
            ui.callback("open", true);
        }
    }

    public void complete() {
        DispatchView d = getSelected();
        if (d == null) {
            return;
        }
        if (change(() -> ctx().dispatches().complete(d.dispatchId(),
                requireChoice(completion.getResult(), "Результат"), optionalText(completion.getNotes()),
                user().id()))) {
            refresh();
            ui.callback("saved", true);
        }
    }

    public CompletionForm getCompletion() {
        return completion;
    }
}
