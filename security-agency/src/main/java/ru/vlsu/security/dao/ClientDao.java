package ru.vlsu.security.dao;

import ru.vlsu.security.model.Client;
import ru.vlsu.security.model.ClientType;

import javax.sql.DataSource;
import java.util.List;

public class ClientDao implements CrudDao<Client> {

    private final Jdbc jdbc;

    public ClientDao(DataSource ds) {
        this.jdbc = new Jdbc(ds);
    }

    @Override
    public List<Client> findAll() {
        return jdbc.query("""
                SELECT client_id, client_type, name, contact_person, phone, email
                FROM clients ORDER BY name
                """, rs -> new Client(rs.getLong(1), ClientType.fromCode(rs.getString(2)), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getString(6)));
    }

    @Override
    public long insert(Client c) {
        return jdbc.insert("""
                INSERT INTO clients (client_type, name, contact_person, phone, email)
                VALUES (?, ?, ?, ?, ?) RETURNING client_id
                """, c.type(), c.name(), c.contactPerson(), c.phone(), c.email());
    }

    @Override
    public void update(Client c) {
        jdbc.update("""
                UPDATE clients SET client_type = ?, name = ?, contact_person = ?, phone = ?, email = ?
                WHERE client_id = ?
                """, c.type(), c.name(), c.contactPerson(), c.phone(), c.email(), c.id());
    }

    @Override
    public void delete(long id) {
        jdbc.update("DELETE FROM clients WHERE client_id = ?", id);
    }
}
