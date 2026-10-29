package ru.vlsu.security.ui.table;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RowFilterTest {

    record R(String a, Integer b) {
    }

    private static final List<ColumnSpec<R>> COLS = List.of(
            new ColumnSpec<>("A", R::a, 100),
            new ColumnSpec<>("B", R::b, 100));

    @Test
    void emptyQuery_matchesAll() {
        R row = new R("x", 1);
        assertTrue(RowFilter.matches(row, COLS, ""));
        assertTrue(RowFilter.matches(row, COLS, "   "));
        assertTrue(RowFilter.matches(row, COLS, null));
    }

    @Test
    void cyrillicCaseInsensitive() {
        assertTrue(RowFilter.matches(new R("М117РН", 1), COLS, "м117рн"));
    }

    @Test
    void trimsQuery() {
        assertTrue(RowFilter.matches(new R("Иванов И.И.", 1), COLS, " иванов "));
    }

    @Test
    void matchesAnyColumn() {
        assertTrue(RowFilter.matches(new R("x", 42), COLS, "42"));
        assertFalse(RowFilter.matches(new R("x", 42), COLS, "43"));
    }

    @Test
    void nullCell_ignored() {
        assertFalse(RowFilter.matches(new R(null, 1), COLS, "abc"));
    }
}
