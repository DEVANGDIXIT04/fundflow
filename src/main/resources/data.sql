-- Dev seed data. Runs on every start because the dev profile recreates the
-- schema (ddl-auto=create); production disables it with SQL_INIT_MODE=never.

INSERT INTO fund (id, name, vintage_year, target_size, currency) VALUES
    (1, 'FundFlow Ventures I', 2023, 50000000.00, 'USD'),
    (2, 'FundFlow Growth II', 2025, 120000000.00, 'USD');

INSERT INTO investor (id, name, email) VALUES
    (1, 'Meridian Pension Trust', 'ops@meridianpension.example'),
    (2, 'Blue Harbor Family Office', 'invest@blueharbor.example'),
    (3, 'Cypress Endowment Fund', 'capital@cypressendowment.example'),
    (4, 'Aurora Sovereign Wealth', 'pe@aurorasw.example');

INSERT INTO commitment (id, fund_id, investor_id, amount) VALUES
    (1, 1, 1, 20000000.00),
    (2, 1, 2, 5000000.00),
    (3, 1, 3, 10000000.00),
    (4, 2, 2, 30000000.00),
    (5, 2, 4, 60000000.00);

-- The inserts above use explicit ids, which bypasses Postgres' identity
-- sequences. Advance them so rows created through the API don't collide.
SELECT setval(pg_get_serial_sequence('fund', 'id'), (SELECT MAX(id) FROM fund));
SELECT setval(pg_get_serial_sequence('investor', 'id'), (SELECT MAX(id) FROM investor));
SELECT setval(pg_get_serial_sequence('commitment', 'id'), (SELECT MAX(id) FROM commitment));
