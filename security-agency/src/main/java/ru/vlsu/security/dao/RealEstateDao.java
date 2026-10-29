package ru.vlsu.security.dao;

import ru.vlsu.security.model.ObjectStatus;
import ru.vlsu.security.model.RealEstate;
import ru.vlsu.security.model.SecurityStatus;

import javax.sql.DataSource;
import java.util.List;

/** Объекты недвижимости: protected_objects с object_type = 'real_estate'. */
public class RealEstateDao implements CrudDao<RealEstate> {

    private final Jdbc jdbc;

    public RealEstateDao(DataSource ds) {
        this.jdbc = new Jdbc(ds);
    }

    @Override
    public List<RealEstate> findAll() {
        return jdbc.query("""
                SELECT o.object_id, o.client_id, c.name, o.name, o.address, o.object_status, o.security_status
                FROM protected_objects o JOIN clients c ON c.client_id = o.client_id
                WHERE o.object_type = 'real_estate'
                ORDER BY o.object_id
                """, rs -> new RealEstate(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                rs.getString(5), ObjectStatus.fromCode(rs.getString(6)), SecurityStatus.fromCode(rs.getString(7))));
    }

    @Override
    public long insert(RealEstate r) {
        return jdbc.insert("""
                INSERT INTO protected_objects (client_id, object_type, name, address, object_status, security_status)
                VALUES (?, 'real_estate', ?, ?, ?, ?) RETURNING object_id
                """, r.clientId(), r.name(), r.address(), r.objectStatus(), r.securityStatus());
    }

    /** Статус охраны здесь не меняется — только на экране мониторинга. */
    @Override
    public void update(RealEstate r) {
        jdbc.update("""
                UPDATE protected_objects SET client_id = ?, name = ?, address = ?, object_status = ?
                WHERE object_id = ? AND object_type = 'real_estate'
                """, r.clientId(), r.name(), r.address(), r.objectStatus(), r.id());
    }

    @Override
    public void delete(long id) {
        jdbc.update("DELETE FROM protected_objects WHERE object_id = ? AND object_type = 'real_estate'", id);
    }
}
