package ru.vlsu.security.web;

import ru.vlsu.security.dao.CrudDao;
import ru.vlsu.security.dao.DataAccessException;
import ru.vlsu.security.dao.DbErrors;

/**
 * Таблица с добавлением, изменением и удалением через диалог-форму (вместо EntityForm + FormDialog).
 * F — изменяемая форма, к полям которой привязаны поля ввода диалога.
 */
public abstract class CrudBean<T, F> extends TableBean<T> {

    private F form;
    private boolean creating;

    protected abstract CrudDao<T> dao();

    /** Форма для новой записи (initialOrNull == null) или для изменения существующей. */
    protected abstract F newForm(T initialOrNull);

    /** Запись из формы; IllegalArgumentException — сообщение пользователю, диалог остаётся открытым. */
    protected abstract T build(F form);

    protected abstract String formTitle(boolean creating);

    protected void insert(T item) {
        dao().insert(item);
    }

    protected void update(T item) {
        dao().update(item);
    }

    protected void remove(long id) {
        dao().delete(id);
    }

    public boolean isCanAdd() {
        return isCanWrite();
    }

    public boolean isCanDelete() {
        return isCanWrite();
    }

    public void startAdd() {
        if (isCanAdd()) {
            open(null);
        }
    }

    public void startEdit() {
        if (isCanWrite() && getSelected() != null) {
            open(getSelected());
        }
    }

    private void open(T initial) {
        try {
            form = newForm(initial);
            creating = initial == null;
            ui.callback("open", true);
        } catch (DataAccessException e) {
            ui.error(DbErrors.message(e));
        }
    }

    public void save() {
        if (form == null) {
            return;
        }
        boolean saved = change(() -> {
            T item = build(form);
            if (creating) {
                insert(item);
            } else {
                update(item);
            }
        });
        if (saved) {
            form = null;
            refresh();
            ui.callback("saved", true);
        }
    }

    public void delete() {
        T selected = getSelected();
        if (selected != null && isCanDelete() && change(() -> remove(key(selected)))) {
            refresh();
        }
    }

    public F getForm() {
        return form;
    }

    public String getFormTitle() {
        return formTitle(creating);
    }
}
