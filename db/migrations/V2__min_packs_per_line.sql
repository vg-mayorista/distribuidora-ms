-- =============================================================================
-- Migration V2 — Per-line min packs rule
-- Idempotent: safe to run multiple times.
--
-- Changes:
--   * business_config drops min_order_amount and min_order_units (no longer
--     enforced; la regla de negocio ahora es por línea, no por carrito).
--   * business_config adds min_packs_per_line INT NOT NULL DEFAULT 5.
--   * Default seeder updated so any business_config existente también queda
--     en 5.
-- =============================================================================

BEGIN;

ALTER TABLE business_config
    ADD COLUMN IF NOT EXISTS min_packs_per_line INTEGER NOT NULL DEFAULT 5;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'business_config_min_packs_per_line_check'
    ) THEN
        ALTER TABLE business_config
            ADD CONSTRAINT business_config_min_packs_per_line_check
            CHECK (min_packs_per_line >= 1);
    END IF;
END
$$;

ALTER TABLE business_config DROP COLUMN IF EXISTS min_order_amount;
ALTER TABLE business_config DROP COLUMN IF EXISTS min_order_units;

COMMIT;
