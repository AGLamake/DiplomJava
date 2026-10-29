package ru.vlsu.security.ui.table;

import java.util.function.Function;

/** Колонка таблицы: заголовок, значение из строки и ширина. */
public record ColumnSpec<T>(String title, Function<T, Object> getter, double width) {
}
