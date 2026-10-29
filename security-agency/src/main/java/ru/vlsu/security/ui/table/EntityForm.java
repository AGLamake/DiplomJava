package ru.vlsu.security.ui.table;

import javafx.stage.Window;

import java.util.Optional;

/** Диалог редактирования записи; initialOrNull == null означает создание новой. */
public interface EntityForm<T> {

    Optional<T> show(Window owner, T initialOrNull);
}
