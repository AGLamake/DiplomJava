package ru.vlsu.security.dao;

import java.util.List;

public interface CrudDao<T> {

    List<T> findAll();

    long insert(T item);

    void update(T item);

    void delete(long id);
}
