package ru.vlsu.security.dao;

import ru.vlsu.security.model.ObjectRef;

import javax.sql.DataSource;
import java.util.List;

/** Список всех охраняемых объектов (недвижимость и автомобили) с подписями «[id] название». */
public class ObjectRefDao {

    private final Jdbc jdbc;

    public ObjectRefDao(DataSource ds) {
        this.jdbc = new Jdbc(ds);
    }

    public List<ObjectRef> findAll() {
        return jdbc.query("""
                SELECT o.object_id, o.client_id,
                       '[' || o.object_id || '] ' || o.name || ' — ' || COALESCE(v.reg_number, o.address, '')
                FROM protected_objects o LEFT JOIN vehicles v ON v.object_id = o.object_id
                ORDER BY o.object_id
                """, rs -> new ObjectRef(rs.getLong(1), rs.getLong(2), rs.getString(3)));
    }
}
