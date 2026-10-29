SET client_encoding = 'UTF8';  -- файл в UTF-8; без этой строки psql в консоли Windows исказит кириллицу

-- Демонстрационные данные. Пользователей нет: первый зарегистрированный становится администратором.

INSERT INTO clients (client_id, client_type, name, contact_person, phone, email) OVERRIDING SYSTEM VALUE VALUES
 (1, 'individual',   'Каприянов Андрей Фёдорович', NULL,                 '+79101234567', 'kapriyanov@mail.ru'),
 (2, 'individual',   'Смирнова Ольга Петровна',    NULL,                 '+73637325945', 'smirnova@yandex.ru'),
 (3, 'legal_entity', 'ООО «Ромашка»',              'Иванов Иван Иванович', '+74922334455', 'office@romashka.ru'),
 (4, 'legal_entity', 'ИП Петров С.В.',             'Петров Сергей Викторович', '+79157778899', NULL);
SELECT setval(pg_get_serial_sequence('clients', 'client_id'), 4);

INSERT INTO protected_objects (object_id, client_id, object_type, name, address, security_status) OVERRIDING SYSTEM VALUE VALUES
 (1, 2, 'real_estate', 'Квартира',       'г. Владимир, ул. Школьная, д. 7, кв. 104', 'armed'),
 (2, 3, 'real_estate', 'Офис',           'г. Владимир, ул. Большая Московская, д. 12', 'armed'),
 (3, 4, 'real_estate', 'Склад',          'г. Владимир, ул. Добросельская, д. 180', 'armed'),
 (4, 1, 'vehicle',     'Ford Fusion',    NULL, 'armed'),
 (5, 2, 'vehicle',     'Renault Logan',  NULL, 'armed'),
 (6, 3, 'vehicle',     'Lada Vesta',     NULL, 'armed');
SELECT setval(pg_get_serial_sequence('protected_objects', 'object_id'), 6);

INSERT INTO vehicles (vehicle_id, object_id, brand, model, reg_number, vin) OVERRIDING SYSTEM VALUE VALUES
 (1, 4, 'Ford',    'Fusion', 'М117РН', 'WF0UXXGAJU8A12345'),
 (2, 5, 'Renault', 'Logan',  'Т843СТ', 'X7LLSRB2HAH123456'),
 (3, 6, 'Lada',    'Vesta',  'А001АА', 'XTAGFK110KY123456');
SELECT setval(pg_get_serial_sequence('vehicles', 'vehicle_id'), 3);

INSERT INTO contracts (contract_id, client_id, contract_number, signed_on, valid_from, valid_to, status, price) OVERRIDING SYSTEM VALUE VALUES
 (1, 1, 'Д-2026/001', '2026-01-10', '2026-01-10', '2027-01-09', 'active', 1500.00),
 (2, 2, 'Д-2026/002', '2026-02-01', '2026-02-01', '2027-01-31', 'active', 2800.00),
 (3, 3, 'Д-2026/003', '2026-03-15', '2026-03-15', NULL,         'active', 9500.00);
SELECT setval(pg_get_serial_sequence('contracts', 'contract_id'), 3);

INSERT INTO contract_objects (contract_id, object_id) VALUES
 (1, 4), (2, 1), (2, 5), (3, 2), (3, 6);

INSERT INTO response_teams (team_id, name, phone, status) OVERRIDING SYSTEM VALUE VALUES
 (1, 'ГБР-1', '+74922000001', 'available'),
 (2, 'ГБР-2', '+74922000002', 'available');
SELECT setval(pg_get_serial_sequence('response_teams', 'team_id'), 2);

INSERT INTO vehicle_locations (vehicle_id, recorded_at, latitude, longitude, speed) VALUES
 (1, CURRENT_TIMESTAMP - INTERVAL '10 minutes', 56.129057, 40.406635, 0),
 (1, CURRENT_TIMESTAMP - INTERVAL '1 minute',  56.129100, 40.406700, 0),
 (2, CURRENT_TIMESTAMP - INTERVAL '10 minutes', 56.142000, 40.395000, 0),
 (2, CURRENT_TIMESTAMP - INTERVAL '1 minute',  56.142100, 40.395100, 0),
 (3, CURRENT_TIMESTAMP - INTERVAL '10 minutes', 56.117000, 40.352000, 0),
 (3, CURRENT_TIMESTAMP - INTERVAL '1 minute',  56.117100, 40.352100, 0);
