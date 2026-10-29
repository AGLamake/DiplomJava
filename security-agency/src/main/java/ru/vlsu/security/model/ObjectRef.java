package ru.vlsu.security.model;

/** Краткая ссылка на охраняемый объект для выпадающих списков. */
public record ObjectRef(long id, long clientId, String label) {

    @Override
    public String toString() {
        return label;
    }
}
