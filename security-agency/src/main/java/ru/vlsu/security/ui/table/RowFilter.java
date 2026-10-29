package ru.vlsu.security.ui.table;

import java.util.List;
import java.util.Locale;

/** Поиск по таблице: подстрока в любой колонке без учёта регистра (вместо DataGridViewFinder). */
public final class RowFilter {

    private RowFilter() {
    }

    public static <T> boolean matches(T row, List<ColumnSpec<T>> columns, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        for (ColumnSpec<T> column : columns) {
            Object value = column.getter().apply(row);
            if (value != null && value.toString().toLowerCase(Locale.ROOT).contains(q)) {
                return true;
            }
        }
        return false;
    }
}
