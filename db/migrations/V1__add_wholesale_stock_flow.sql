-- =============================================================================
-- Migration V1 — Add wholesale vs stock order flow support
-- Idempotent: safe to run multiple times.
--
-- Changes:
--   * orders.type                  (STOCK | WHOLESALE) default STOCK
--   * delivery_methods.applies_to_order_type (WHOLESALE | STOCK | BOTH) default BOTH
--   * new table delivery_windows (configurable weekly cutoff windows)
--   * seed two default windows: Mar 18:00 → Mié, Jue 18:00 → Vie
--   * reconcile existing delivery methods: Envío Express → STOCK
-- =============================================================================

BEGIN;

-- orders.type
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS type VARCHAR(20) NOT NULL DEFAULT 'STOCK';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'orders_type_check'
    ) THEN
        ALTER TABLE orders
            ADD CONSTRAINT orders_type_check
            CHECK (type IN ('STOCK', 'WHOLESALE'));
    END IF;
END
$$;

-- delivery_methods.applies_to_order_type
ALTER TABLE delivery_methods
    ADD COLUMN IF NOT EXISTS applies_to_order_type VARCHAR(20) NOT NULL DEFAULT 'BOTH';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'delivery_methods_scope_check'
    ) THEN
        ALTER TABLE delivery_methods
            ADD CONSTRAINT delivery_methods_scope_check
            CHECK (applies_to_order_type IN ('WHOLESALE', 'STOCK', 'BOTH'));
    END IF;
END
$$;

-- Envío Express → STOCK only (idempotent)
UPDATE delivery_methods
SET applies_to_order_type = 'STOCK'
WHERE name ILIKE '%express%' AND applies_to_order_type <> 'STOCK';

-- delivery_windows table
CREATE TABLE IF NOT EXISTS delivery_windows (
    id UUID PRIMARY KEY,
    cutoff_day_of_week SMALLINT NOT NULL CHECK (cutoff_day_of_week BETWEEN 1 AND 7),
    cutoff_time TIME WITHOUT TIME ZONE NOT NULL,
    delivery_day_of_week SMALLINT NOT NULL CHECK (delivery_day_of_week BETWEEN 1 AND 7),
    description VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE INDEX IF NOT EXISTS idx_delivery_windows_active
    ON delivery_windows (cutoff_day_of_week, cutoff_time)
    WHERE active = TRUE;

-- Seed windows only if there is no (cutoff_day_of_week, cutoff_time, delivery_day_of_week) tuple.
INSERT INTO delivery_windows (id, cutoff_day_of_week, cutoff_time, delivery_day_of_week, description, active)
SELECT gen_random_uuid(), 2, '18:00:00', 3, 'Pedidos hasta Mar 18 h → entrega Mié', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM delivery_windows
    WHERE cutoff_day_of_week = 2 AND cutoff_time = '18:00:00' AND delivery_day_of_week = 3
);

INSERT INTO delivery_windows (id, cutoff_day_of_week, cutoff_time, delivery_day_of_week, description, active)
SELECT gen_random_uuid(), 4, '18:00:00', 5, 'Pedidos hasta Jue 18 h → entrega Vie', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM delivery_windows
    WHERE cutoff_day_of_week = 4 AND cutoff_time = '18:00:00' AND delivery_day_of_week = 5
);

COMMIT;
