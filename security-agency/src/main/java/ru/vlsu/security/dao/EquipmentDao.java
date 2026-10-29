package ru.vlsu.security.dao;

import ru.vlsu.security.model.Equipment;
import ru.vlsu.security.model.EquipmentStatus;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;

public class EquipmentDao implements CrudDao<Equipment> {

    private final Jdbc jdbc;

    public EquipmentDao(DataSource ds) {
        this.jdbc = new Jdbc(ds);
    }

    @Override
    public List<Equipment> findAll() {
        return jdbc.query("""
                SELECT e.equipment_id, e.object_id, '[' || o.object_id || '] ' || o.name, e.equipment_type_id, t.name,
                       e.serial_number, e.installation_date, e.status
                FROM equipment e
                JOIN protected_objects o ON o.object_id = e.object_id
                JOIN equipment_types t ON t.equipment_type_id = e.equipment_type_id
                ORDER BY e.equipment_id
                """, rs -> new Equipment(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getLong(4),
                rs.getString(5), rs.getString(6), rs.getObject(7, LocalDate.class),
                EquipmentStatus.fromCode(rs.getString(8))));
    }

    @Override
    public long insert(Equipment e) {
        return jdbc.insert("""
                INSERT INTO equipment (object_id, equipment_type_id, serial_number, installation_date, status)
                VALUES (?, ?, ?, ?, ?) RETURNING equipment_id
                """, e.objectId(), e.typeId(), e.serialNumber(), e.installationDate(), e.status());
    }

    @Override
    public void update(Equipment e) {
        jdbc.update("""
                UPDATE equipment SET object_id = ?, equipment_type_id = ?, serial_number = ?, installation_date = ?,
                       status = ?
                WHERE equipment_id = ?
                """, e.objectId(), e.typeId(), e.serialNumber(), e.installationDate(), e.status(), e.id());
    }

    @Override
    public void delete(long id) {
        jdbc.update("DELETE FROM equipment WHERE equipment_id = ?", id);
    }
}
