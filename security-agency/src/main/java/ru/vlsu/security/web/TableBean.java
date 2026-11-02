package ru.vlsu.security.web;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import ru.vlsu.security.AppContext;
import ru.vlsu.security.dao.DataAccessException;
import ru.vlsu.security.dao.DbErrors;
import ru.vlsu.security.model.User;
import ru.vlsu.security.security.Section;

import java.io.Serializable;
import java.util.List;

/**
 * Экран-таблица: загрузка, поиск, выбор строки (вместо TableViewController).
 * Страница строится шаблоном WEB-INF/templates/table.xhtml.
 */
public abstract class TableBean<T> implements Serializable {

    static final String NO_RIGHTS = "Недостаточно прав для изменения данных";

    @Inject
    AppResources app;
    @Inject
    UserSession session;
    @Inject
    FacesUi ui;

    private transient List<ColumnSpec<T>> columns;
    private List<T> all = List.of();
    private List<T> rows = List.of();
    private String query;
    private T selected;

    public abstract String getTitle();

    protected abstract Section section();

    protected abstract List<ColumnSpec<T>> columns();

    protected abstract List<T> load();

    protected abstract long key(T row);

    @PostConstruct
    public void init() {
        refresh();
    }

    protected AppContext ctx() {
        return app.ctx();
    }

    protected User user() {
        return session.getUser();
    }

    public boolean isCanWrite() {
        return session.access(section()).canWrite();
    }

    public List<ColumnSpec<T>> getColumns() {
        if (columns == null) {
            columns = columns();
        }
        return columns;
    }

    /** Ключ строки для p:dataTable (rowKey); отдельное имя, чтобы EL не путался с мостовым методом key(Object). */
    @SuppressWarnings("unchecked")
    public long keyOf(Object row) {
        return key((T) row);
    }

    /** Перечитывает данные, сохраняя выбранную строку. */
    public void refresh() {
        Long selectedKey = selected == null ? null : key(selected);
        try {
            all = load();
        } catch (DataAccessException e) {
            ui.error(DbErrors.message(e));
        }
        selected = selectedKey == null ? null
                : all.stream().filter(r -> key(r) == selectedKey).findFirst().orElse(null);
        applyFilter();
    }

    public void applyFilter() {
        rows = all.stream().filter(r -> RowFilter.matches(r, getColumns(), query)).toList();
    }

    /** Изменение данных; ошибки показываются пользователю. true — изменение выполнено. */
    protected boolean change(Runnable r) {
        if (!isCanWrite()) {
            ui.warn(NO_RIGHTS);
            return false;
        }
        try {
            r.run();
            return true;
        } catch (DataAccessException e) {
            ui.error(DbErrors.message(e));
        } catch (IllegalArgumentException | IllegalStateException e) {
            ui.warn(e.getMessage());
        }
        return false;
    }

    public List<T> getRows() {
        return rows;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public T getSelected() {
        return selected;
    }

    public void setSelected(T selected) {
        this.selected = selected;
    }
}
