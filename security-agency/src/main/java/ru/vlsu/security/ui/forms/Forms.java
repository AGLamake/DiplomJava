package ru.vlsu.security.ui.forms;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;

import java.util.List;
import java.util.Objects;
import java.util.function.ToLongFunction;

/** Мелкие помощники для форм. */
final class Forms {

    private Forms() {
    }

    @SafeVarargs
    static <T> ComboBox<T> combo(T... values) {
        return new ComboBox<>(FXCollections.observableArrayList(values));
    }

    static <T> ComboBox<T> combo(List<T> values) {
        return new ComboBox<>(FXCollections.observableArrayList(values));
    }

    /** Выбирает в списке элемент с указанным id (записи сравниваются по id, а не по equals). */
    static <T> void select(ComboBox<T> box, Long id, ToLongFunction<T> idOf) {
        if (id == null) {
            return;
        }
        box.getItems().stream().filter(i -> Objects.equals(idOf.applyAsLong(i), id)).findFirst()
                .ifPresent(box::setValue);
    }
}
