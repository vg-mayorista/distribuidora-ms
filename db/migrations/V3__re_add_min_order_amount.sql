-- =============================================================================
-- Migration V3 — Re-add min_order_amount (coexists with min_packs_per_line)
-- Idempotent: safe to run multiple times.
--
-- Change:
--   * business_config adds min_order_amount NUMERIC(12,2) NOT NULL
--     DEFAULT 30000.00 alongside the existing min_packs_per_line column.
--   * Coexists with the per-line pack minimum: el pedido tiene que cumplir
--     AMBAS reglas (>= 5 packs por línea Y >= $30k de subtotal).
-- =============================================================================

BEGIN;

ALTER TABLE business_config
    ADD COLUMN IF NOT EXISTS min_order_amount NUMERIC(12,2) NOT NULL DEFAULT 30000.00;

COMMIT;
