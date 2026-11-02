package ru.vlsu.security.web;

import java.util.List;
import java.util.Objects;
import java.util.function.ToLongFunction;

/** Выбор записи справочника по id из выпадающего списка. */
final class Lookups {

    private Lookups() {
    }

    static <T> T byId(List<T> items, Long id, ToLongFunction<T> idOf) {
        if (id == null) {
            return null;
        }
        return items.stream().filter(i -> Objects.equals(idOf.applyAsLong(i), id)).findFirst().orElse(null);
    }
}
