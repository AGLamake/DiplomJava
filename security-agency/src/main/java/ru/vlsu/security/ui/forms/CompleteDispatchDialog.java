package ru.vlsu.security.ui.forms;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.stage.Window;
import ru.vlsu.security.model.DispatchResult;
import ru.vlsu.security.ui.table.FormDialog;

import java.util.Optional;

import static ru.vlsu.security.validation.FormParsers.optionalText;
import static ru.vlsu.security.validation.FormParsers.requireChoice;

/** Диалог завершения выезда ГБР: результат и примечание. */
public final class CompleteDispatchDialog {

    public record Result(DispatchResult result, String notes) {
    }

    private CompleteDispatchDialog() {
    }

    public static Optional<Result> show(Window owner) {
        FormDialog<Result> f = new FormDialog<>(owner, "Завершение выезда");
        ComboBox<DispatchResult> result = f.add("Результат", Forms.combo(DispatchResult.values()));
        TextArea notes = f.add("Примечание", new TextArea());
        notes.setPrefRowCount(3);
        return f.show(() -> new Result(requireChoice(result.getValue(), "Результат"), optionalText(notes.getText())));
    }
}
