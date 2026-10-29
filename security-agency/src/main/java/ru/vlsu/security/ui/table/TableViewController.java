package ru.vlsu.security.ui.table;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import ru.vlsu.security.dao.DataAccessException;
import ru.vlsu.security.dao.DbErrors;
import ru.vlsu.security.security.Access;
import ru.vlsu.security.ui.Dialogs;
import ru.vlsu.security.ui.Screens;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Общий экран таблицы: поиск, добавление, изменение, удаление (вместо DataGridView + TableAdapter). */
public final class TableViewController<T> {

    private final TableSpec<T> spec;
    private final Access access;
    private final ObservableList<T> items = FXCollections.observableArrayList();
    private final FilteredList<T> filtered = new FilteredList<>(items);
    private final TableView<T> table = new TableView<>(filtered);
    private final TextField search = new TextField();
    private final List<Button> rowButtons = new ArrayList<>();
    private final List<TableSpec.RowAction<T>> rowButtonActions = new ArrayList<>();

    private TableViewController(TableSpec<T> spec, Access access) {
        this.spec = spec;
        this.access = access;
    }

    public static <T> Parent create(TableSpec<T> spec, Access access) {
        return new TableViewController<>(spec, access).build();
    }

    private Parent build() {
        Label title = new Label(spec.title());
        title.getStyleClass().add("screen-title");
        search.setPromptText("Поиск…");
        search.setPrefWidth(300);
        search.textProperty().addListener((obs, o, n) ->
                filtered.setPredicate(row -> RowFilter.matches(row, spec.columns(), n)));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(12, title, spacer, search);
        top.setAlignment(Pos.CENTER_LEFT);

        for (ColumnSpec<T> c : spec.columns()) {
            TableColumn<T, Object> col = new TableColumn<>(c.title());
            col.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(c.getter().apply(cd.getValue())));
            col.setPrefWidth(c.width());
            table.getColumns().add(col);
        }
        table.setPlaceholder(new Label("Нет записей"));
        table.setRowFactory(tv -> {
            TableRow<T> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty() && access.canWrite() && spec.form() != null) {
                    onEdit();
                }
            });
            return row;
        });

        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_LEFT);
        if (access.canWrite()) {
            if (spec.canAdd() && spec.form() != null) {
                buttons.getChildren().add(button("Добавить", this::onAdd));
            }
            if (spec.form() != null) {
                buttons.getChildren().add(rowButton("Изменить", this::onEdit, null));
            }
            if (spec.canDelete()) {
                buttons.getChildren().add(rowButton("Удалить", this::onDelete, null));
            }
            for (TableSpec.RowAction<T> action : spec.actions()) {
                buttons.getChildren().add(rowButton(action.title(), () -> runAction(action), action));
            }
        }
        buttons.getChildren().add(button("Обновить", this::refresh));
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> updateButtons());

        BorderPane root = new BorderPane(table);
        root.setTop(top);
        root.setBottom(buttons);
        BorderPane.setMargin(top, new Insets(0, 0, 10, 0));
        BorderPane.setMargin(buttons, new Insets(10, 0, 0, 0));
        root.setPadding(new Insets(16));
        refresh();
        return Screens.withLifecycle(root, new Screens.Lifecycle() {
            @Override
            public void onShown() {
                refresh();
            }
        });
    }

    private Button button(String text, Runnable action) {
        Button b = new Button(text);
        b.setOnAction(e -> action.run());
        return b;
    }

    private Button rowButton(String text, Runnable action, TableSpec.RowAction<T> rowAction) {
        Button b = button(text, action);
        b.setDisable(true);
        rowButtons.add(b);
        rowButtonActions.add(rowAction);
        return b;
    }

    private void updateButtons() {
        T selected = table.getSelectionModel().getSelectedItem();
        for (int i = 0; i < rowButtons.size(); i++) {
            TableSpec.RowAction<T> a = rowButtonActions.get(i);
            boolean enabled = selected != null && (a == null || a.enabledFor().test(selected));
            rowButtons.get(i).setDisable(!enabled);
        }
    }

    public void refresh() {
        T selected = table.getSelectionModel().getSelectedItem();
        try {
            items.setAll(spec.dao().findAll());
        } catch (DataAccessException e) {
            Dialogs.error(DbErrors.message(e));
        }
        if (selected != null) {
            long id = spec.idOf().applyAsLong(selected);
            items.stream().filter(i -> spec.idOf().applyAsLong(i) == id).findFirst()
                    .ifPresent(i -> table.getSelectionModel().select(i));
        }
        updateButtons();
    }

    private void onAdd() {
        Optional<T> created = spec.form().show(table.getScene().getWindow(), null);
        created.ifPresent(item -> {
            if (save(() -> spec.dao().insert(item))) {
                refresh();
            }
        });
    }

    private void onEdit() {
        T selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        spec.form().show(table.getScene().getWindow(), selected).ifPresent(item -> {
            if (save(() -> spec.dao().update(item))) {
                refresh();
            }
        });
    }

    private void onDelete() {
        T selected = table.getSelectionModel().getSelectedItem();
        if (selected == null || !Dialogs.confirm("Удалить выбранную запись?")) {
            return;
        }
        if (save(() -> spec.dao().delete(spec.idOf().applyAsLong(selected)))) {
            refresh();
        }
    }

    private void runAction(TableSpec.RowAction<T> action) {
        T selected = table.getSelectionModel().getSelectedItem();
        if (selected != null && save(() -> action.run().accept(table.getScene().getWindow(), selected))) {
            refresh();
        }
    }

    private static boolean save(Runnable r) {
        try {
            r.run();
            return true;
        } catch (DataAccessException e) {
            Dialogs.error(DbErrors.message(e));
        } catch (IllegalArgumentException | IllegalStateException e) {
            Dialogs.warning(e.getMessage());
        }
        return false;
    }
}
