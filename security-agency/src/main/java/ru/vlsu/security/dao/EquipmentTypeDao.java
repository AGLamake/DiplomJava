package ru.vlsu.security.dao;

import ru.vlsu.security.model.EquipmentType;

import javax.sql.DataSource;
import java.util.List;

public class EquipmentTypeDao {

    private final Jdbc jdbc;

    public EquipmentTypeDao(DataSource ds) {
        this.jdbc = new Jdbc(ds);
    }

    public List<EquipmentType> findAll() {
        return jdbc.query("SELECT equipment_type_id, name, category FROM equipment_types ORDER BY name",
                rs -> new EquipmentType(rs.getLong(1), rs.getString(2), rs.getString(3)));
    }
}
