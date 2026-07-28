# Base de datos

## Quick start (instalación limpia)

```powershell
# 1. Levantar el postgres (init script crea user/db distribuidora)
docker compose up -d

# 2. Esperar a que esté healthy
docker ps   # ver STATUS = "healthy"

# 3. Levantar el backend
cd ..
.\mvnw spring-boot:run
```

## Credenciales por defecto

| Campo | Valor |
|---|---|
| Host | `localhost` |
| Puerto | `5433` |
| Database | `distribuidora` |
| User | `distribuidora` |
| Password | `distribuidora` |

Las podés cambiar vía `.env` (copia `env.example`) o pasando las env vars `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` al backend.

## Migración desde `greenmarket-postgres` (versión vieja)

Si ya tenés el container viejo del fork de green-market con credenciales `greenmarket/greenmarket/greenmarket`:

```powershell
# Migrá las credenciales sin perder datos
.\setup-db.ps1 -Migrate
```

Esto crea el user `distribuidora` y la DB `distribuidora` adentro del container existente. El backend conecta igual.

## Empezar de cero (borrando datos)

```powershell
# Borra container + volume y recrea limpio
.\setup-db.ps1 -Fresh
```

⚠️ Esto **borra todos los datos** del postgres (pedidos, productos, usuarios, etc.).

## Init script

`init/01-create-distribuidora.sql` se ejecuta automáticamente **una sola vez** cuando el volume se inicializa por primera vez (es el comportamiento estándar de la imagen `postgres:16-alpine` vía `docker-entrypoint-initdb.d`).

Es idempotente: usa `IF NOT EXISTS` para que sea seguro ejecutarlo varias veces.
