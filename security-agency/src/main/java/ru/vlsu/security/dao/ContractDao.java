package ru.vlsu.security.dao;

import ru.vlsu.security.model.Contract;
import ru.vlsu.security.model.ContractStatus;

import javax.sql.DataSource;
import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/** Договоры и их объекты (contract_objects переписываются в той же транзакции). */
public class ContractDao implements CrudDao<Contract> {

    private final Jdbc jdbc;

    public ContractDao(DataSource ds) {
        this.jdbc = new Jdbc(ds);
    }

    @Override
    public List<Contract> findAll() {
        return jdbc.query("""
                SELECT k.contract_id, k.client_id, c.name, k.contract_number, k.signed_on, k.valid_from,
                       k.valid_to, k.status, k.price,
                       COALESCE(ARRAY(SELECT co.object_id FROM contract_objects co
                                      WHERE co.contract_id = k.contract_id ORDER BY co.object_id), '{}')
                FROM contracts k JOIN clients c ON c.client_id = k.client_id
                ORDER BY k.contract_id
                """, rs -> new Contract(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                rs.getObject(5, LocalDate.class), rs.getObject(6, LocalDate.class), rs.getObject(7, LocalDate.class),
                ContractStatus.fromCode(rs.getString(8)), rs.getBigDecimal(9), ids(rs.getArray(10))));
    }

    @Override
    public long insert(Contract k) {
        return jdbc.inTransaction(c -> {
            long id = Jdbc.insert(c, """
                    INSERT INTO contracts (client_id, contract_number, signed_on, valid_from, valid_to, status, price)
                    VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING contract_id
                    """, k.clientId(), k.number(), k.signedOn(), k.validFrom(), k.validTo(), k.status(), k.price());
            writeObjects(c, id, k.objectIds());
            return id;
        });
    }

    @Override
    public void update(Contract k) {
        jdbc.inTransaction(c -> {
            Jdbc.update(c, """
                    UPDATE contracts SET client_id = ?, contract_number = ?, signed_on = ?, valid_from = ?,
                           valid_to = ?, status = ?, price = ?
                    WHERE contract_id = ?
                    """, k.clientId(), k.number(), k.signedOn(), k.validFrom(), k.validTo(), k.status(), k.price(),
                    k.id());
            Jdbc.update(c, "DELETE FROM contract_objects WHERE contract_id = ?", k.id());
            writeObjects(c, k.id(), k.objectIds());
            return null;
        });
    }

    @Override
    public void delete(long id) {
        jdbc.update("DELETE FROM contracts WHERE contract_id = ?", id);
    }

    private static void writeObjects(Connection c, long contractId, List<Long> objectIds) throws SQLException {
        for (Long objectId : objectIds) {
            Jdbc.update(c, "INSERT INTO contract_objects (contract_id, object_id) VALUES (?, ?)", contractId, objectId);
        }
    }

    private static List<Long> ids(Array array) throws SQLException {
        return Arrays.stream((Long[]) array.getArray()).toList();
    }
}
