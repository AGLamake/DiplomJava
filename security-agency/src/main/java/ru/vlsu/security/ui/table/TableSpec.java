package ru.vlsu.security.ui.table;

import ru.vlsu.security.dao.CrudDao;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

/** Описание CRUD-экрана: колонки, DAO, форма редактирования и дополнительные действия. */
public record TableSpec<T>(String title, List<ColumnSpec<T>> columns, CrudDao<T> dao, EntityForm<T> form,
                           ToLongFunction<T> idOf, boolean canAdd, boolean canDelete,
                           List<RowAction<T>> actions) {

    /** Дополнительная кнопка над выбранной строкой (доступна только при праве на изменение). */
    public record RowAction<T>(String title, Predicate<T> enabledFor, Consumer<T> run) {
    }

    public static <T> TableSpec<T> of(String title, List<ColumnSpec<T>> columns, CrudDao<T> dao,
                                      EntityForm<T> form, ToLongFunction<T> idOf) {
        return new TableSpec<>(title, columns, dao, form, idOf, true, true, List.of());
    }

    public TableSpec<T> withoutAdd() {
        return new TableSpec<>(title, columns, dao, form, idOf, false, canDelete, actions);
    }

    public TableSpec<T> withoutDelete() {
        return new TableSpec<>(title, columns, dao, form, idOf, canAdd, false, actions);
    }

    public TableSpec<T> withAction(RowAction<T> action) {
        List<RowAction<T>> list = new java.util.ArrayList<>(actions);
        list.add(action);
        return new TableSpec<>(title, columns, dao, form, idOf, canAdd, canDelete, List.copyOf(list));
    }
}
