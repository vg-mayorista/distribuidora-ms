-- =============================================================================
-- Init script: asegura que el user/db de Distribuidora exista.
-- Se ejecuta UNA SOLA VEZ cuando el volumen del postgres se inicializa por
-- primera vez (ver docker-entrypoint-initdb.d de la imagen oficial).
-- Es idempotente: si ya existe el rol/db no rompe.
-- =============================================================================

-- Si el rol distribuidora no existe, lo creamos. Si existe, lo modificamos
-- para asegurarnos que tenga la password correcta.
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'distribuidora') THEN
    CREATE ROLE distribuidora WITH LOGIN PASSWORD 'distribuidora' CREATEDB;
  ELSE
    ALTER ROLE distribuidora WITH LOGIN PASSWORD 'distribuidora';
  END IF;
END
$$;

-- Aseguramos que pueda crear bases de datos
ALTER ROLE distribuidora CREATEDB;

-- Si la DB distribuidora no existe, la creamos con dueño distribuidora
SELECT 'CREATE DATABASE distribuidora OWNER distribuidora'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'distribuidora')\gexec
