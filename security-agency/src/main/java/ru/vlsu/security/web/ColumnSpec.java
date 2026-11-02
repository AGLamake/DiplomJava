package ru.vlsu.security.web;

import java.util.function.Function;

/** Колонка таблицы: заголовок, значение из строки и ширина в пикселях. */
public record ColumnSpec<T>(String title, Function<T, Object> getter, int width) {

    /** Текст ячейки; перечисления показываются русским названием через toString(). */
    public String text(T row) {
        Object value = getter.apply(row);
        return value == null ? "" : value.toString();
    }
}
