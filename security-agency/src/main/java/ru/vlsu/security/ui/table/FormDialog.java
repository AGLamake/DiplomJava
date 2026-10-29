package ru.vlsu.security.ui.table;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;
import ru.vlsu.security.ui.Dialogs;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Диалог-форма «подпись — поле». Если сборка значения бросает IllegalArgumentException,
 * сообщение показывается пользователю, а форма остаётся открытой.
 */
public final class FormDialog<T> {

    private final Dialog<T> dialog = new Dialog<>();
    private final GridPane grid = new GridPane();
    private int row;

    public FormDialog(Window owner, String title) {
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(16));
        ColumnConstraints labels = new ColumnConstraints();
        ColumnConstraints fields = new ColumnConstraints(320);
        fields.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labels, fields);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Сохранить");
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Отмена");
    }

    public <N extends Node> N add(String label, N field) {
        grid.add(new Label(label), 0, row);
        grid.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
        if (field instanceof javafx.scene.control.Control c) {
            c.setMaxWidth(Double.MAX_VALUE);
        }
        row++;
        return field;
    }

    public Optional<T> show(Supplier<T> builder) {
        Object[] result = new Object[1];
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, e -> {
            try {
                result[0] = builder.get();
            } catch (IllegalArgumentException ex) {
                Dialogs.warning(ex.getMessage());
                e.consume();
            }
        });
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? cast(result[0]) : null);
        return dialog.showAndWait();
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) {
        return (T) o;
    }
}
