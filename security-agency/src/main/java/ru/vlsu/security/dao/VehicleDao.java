package ru.vlsu.security.dao;

import ru.vlsu.security.model.ObjectStatus;
import ru.vlsu.security.model.SecurityStatus;
import ru.vlsu.security.model.VehicleObject;

import javax.sql.DataSource;
import java.util.List;

/** Автомобили: protected_objects (object_type = 'vehicle') + vehicles, изменяются в одной транзакции. */
public class VehicleDao implements CrudDao<VehicleObject> {

    private final Jdbc jdbc;

    public VehicleDao(DataSource ds) {
        this.jdbc = new Jdbc(ds);
    }

    @Override
    public List<VehicleObject> findAll() {
        return jdbc.query("""
                SELECT o.object_id, o.client_id, c.name, v.brand, v.model, v.reg_number, v.vin,
                       o.object_status, o.security_status
                FROM protected_objects o
                JOIN vehicles v ON v.object_id = o.object_id
                JOIN clients c ON c.client_id = o.client_id
                ORDER BY o.object_id
                """, rs -> new VehicleObject(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                rs.getString(5), rs.getString(6), rs.getString(7), ObjectStatus.fromCode(rs.getString(8)),
                SecurityStatus.fromCode(rs.getString(9))));
    }

    @Override
    public long insert(VehicleObject v) {
        return jdbc.inTransaction(c -> {
            long objectId = Jdbc.insert(c, """
                    INSERT INTO protected_objects (client_id, object_type, name, object_status, security_status)
                    VALUES (?, 'vehicle', ?, ?, ?) RETURNING object_id
                    """, v.clientId(), name(v), v.objectStatus(), v.securityStatus());
            Jdbc.insert(c, """
                    INSERT INTO vehicles (object_id, brand, model, reg_number, vin)
                    VALUES (?, ?, ?, ?, ?) RETURNING vehicle_id
                    """, objectId, v.brand(), v.model(), v.regNumber(), v.vin());
            return objectId;
        });
    }

    @Override
    public void update(VehicleObject v) {
        jdbc.inTransaction(c -> {
            Jdbc.update(c, """
                    UPDATE protected_objects SET client_id = ?, name = ?, object_status = ?
                    WHERE object_id = ? AND object_type = 'vehicle'
                    """, v.clientId(), name(v), v.objectStatus(), v.objectId());
            Jdbc.update(c, "UPDATE vehicles SET brand = ?, model = ?, reg_number = ?, vin = ? WHERE object_id = ?",
                    v.brand(), v.model(), v.regNumber(), v.vin(), v.objectId());
            return null;
        });
    }

    @Override
    public void delete(long objectId) {
        jdbc.update("DELETE FROM protected_objects WHERE object_id = ? AND object_type = 'vehicle'", objectId);
    }

    private static String name(VehicleObject v) {
        return v.brand() + " " + v.model();
    }
}
