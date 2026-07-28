# =============================================================================
# setup-db.ps1 - Setup/migración de la base de datos Distribuidora
# =============================================================================
# Deja la base de datos lista para correr el backend.
#
# Casos que cubre (modo automático):
#  1. No tenés nada           → docker compose up -d (init script crea todo)
#  2. greenmarket-postgres    → renombra a distribuidora-postgres (preserva datos)
#  3. distribuidora-postgres  → ya está, no hace nada
#
# Opciones:
#  .\setup-db.ps1           # modo automático (detecta y arregla)
#  .\setup-db.ps1 -Migrate  # fuerza migración (renombra container)
#  .\setup-db.ps1 -Fresh    # borra todo y recrea limpio (PIERDE DATOS)
# =============================================================================

param(
  [switch]$Migrate,
  [switch]$Fresh
)

$ErrorActionPreference = 'Stop'

function Write-Step($msg)  { Write-Host "`n===> $msg" -ForegroundColor Cyan }
function Write-Ok($msg)    { Write-Host "  [OK] $msg" -ForegroundColor Green }
function Write-Warn($msg)  { Write-Host "  [WARN] $msg" -ForegroundColor Yellow }

# ---------------------------------------------------------------------------
# Detectar containers/volúmenes existentes
# ---------------------------------------------------------------------------
$running = docker ps -a --format "{{.Names}}" 2>$null
$hasOld   = $running -contains 'greenmarket-postgres'
$hasNew   = $running -contains 'distribuidora-postgres'

$volumes  = docker volume ls --format "{{.Name}}" 2>$null
$hasOldVol = $volumes -contains 'greenmarket_pgdata'
$hasNewVol = $volumes -contains 'distribuidora_pgdata'

Write-Host "Estado actual:"
Write-Host "  container greenmarket-postgres : $(if ($hasOld)   { 'existe' } else { '(no)' })"
Write-Host "  container distribuidora-postgres: $(if ($hasNew)   { 'existe' } else { '(no)' })"
Write-Host "  volume greenmarket_pgdata       : $(if ($hasOldVol){ 'existe' } else { '(no)' })"
Write-Host "  volume distribuidora_pgdata     : $(if ($hasNewVol){ 'existe' } else { '(no)' })"

# ---------------------------------------------------------------------------
# Modo -Fresh: bajar todo y recrear
# ---------------------------------------------------------------------------
if ($Fresh) {
  Write-Step "Modo FRESH: bajando containers y borrando volumes (datos perdidos)"
  if ($hasOld)   { docker rm -f greenmarket-postgres 2>$null | Out-Null;   Write-Ok "Container greenmarket-postgres borrado" }
  if ($hasNew)   { docker rm -f distribuidora-postgres 2>$null | Out-Null; Write-Ok "Container distribuidora-postgres borrado" }
  if ($hasOldVol){ docker volume rm greenmarket_pgdata 2>$null | Out-Null; Write-Ok "Volume greenmarket_pgdata borrado" }
  if ($hasNewVol){ docker volume rm distribuidora_pgdata 2>$null | Out-Null; Write-Ok "Volume distribuidora_pgdata borrado" }

  Write-Step "Levantando distribuidora-postgres (init script crea user/db)"
  docker compose up -d

  Write-Step "Esperando a que la DB esté healthy"
  for ($i=0; $i -lt 30; $i++) {
    $status = docker inspect --format='{{.State.Health.Status}' distribuidora-postgres 2>$null
    if ($status -eq 'healthy') { Write-Ok "DB healthy"; break }
    Start-Sleep -Seconds 2
  }
  Write-Host "`nListo. Ahora podés levantar el backend con:" -ForegroundColor Green
  Write-Host "  cd D:\programacion\distribuidora && .\mvnw spring-boot:run" -ForegroundColor White
  exit 0
}

# ---------------------------------------------------------------------------
# Modo -Migrate: forzar la migración (renombra container + volume)
# ---------------------------------------------------------------------------
if ($Migrate) {
  if (-not $hasOld) {
    Write-Host "No hay greenmarket-postgres que migrar. Nada que hacer." -ForegroundColor Yellow
    exit 0
  }
  Write-Step "Migrando greenmarket-postgres → distribuidora-postgres (preserva datos)"

  docker stop greenmarket-postgres | Out-Null
  docker rename greenmarket-postgres distribuidora-postgres
  Write-Ok "Container renombrado"

  if ($hasOldVol) {
    docker volume rename greenmarket_pgdata distribuidora_pgdata
    Write-Ok "Volume renombrado"
  }

  docker start distribuidora-postgres
  Write-Ok "Container levantado"
  Write-Step "Verificando credenciales distribuidora en el volume"
  $realUser = docker exec distribuidora-postgres bash -c "echo \$POSTGRES_USER" 2>$null
  if (-not $realUser) { $realUser = 'greenmarket' }
  Write-Host "  El container fue inicializado con POSTGRES_USER='$realUser'."
  Write-Host "  Ejecutá manualmente este SQL si la DB 'distribuidora' no existe:"
  Write-Host "    docker exec -it distribuidora-postgres psql -U $realUser -d postgres" -ForegroundColor White
  Write-Host "    CREATE USER distribuidora WITH PASSWORD 'distribuidora' CREATEDB;" -ForegroundColor White
  Write-Host "    CREATE DATABASE distribuidora OWNER distribuidora;" -ForegroundColor White
  Write-Host "    GRANT ALL ON DATABASE distribuidora TO distribuidora;" -ForegroundColor White
  Write-Host "`nDespués de eso, el backend conecta sin problemas." -ForegroundColor Green
  exit 0
}

# ---------------------------------------------------------------------------
# Modo automático
# ---------------------------------------------------------------------------
if ($hasNew) {
  Write-Step "distribuidora-postgres ya está corriendo. No hay nada que hacer."
  Write-Host "`nSi la DB 'distribuidora' no existe adentro, conectate y creala:" -ForegroundColor Yellow
  Write-Host "  docker exec -it distribuidora-postgres psql -U distribuidora -d postgres" -ForegroundColor White
  Write-Host "  CREATE DATABASE distribuidora OWNER distribuidora;" -ForegroundColor White
  exit 0
}

if ($hasOld) {
  Write-Step "Detecté greenmarket-postgres. Lo voy a renombrar a distribuidora-postgres (preserva datos)"

  docker stop greenmarket-postgres | Out-Null
  docker rename greenmarket-postgres distribuidora-postgres
  Write-Ok "Container renombrado"

  if ($hasOldVol) {
    docker volume rename greenmarket_pgdata distribuidora_pgdata
    Write-Ok "Volume renombrado"
  }

  docker start distribuidora-postgres
  Write-Ok "Container distribuidora-postgres levantado"

  Write-Step "Verificando que la DB 'distribuidora' exista"
  Start-Sleep -Seconds 3
  $realUser = docker exec distribuidora-postgres bash -c "echo \$POSTGRES_USER" 2>$null
  if (-not $realUser) { $realUser = 'greenmarket' }
  $hasDb = docker exec distribuidora-postgres psql -U $realUser -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='distribuidora'" 2>$null
  if ($hasDb -ne '1') {
    Write-Warn "La DB 'distribuidora' no existe en el volume. Creándola ahora."
    docker exec -i distribuidora-postgres psql -U $realUser -d postgres <<EOF
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'distribuidora') THEN
    CREATE ROLE distribuidora WITH LOGIN PASSWORD 'distribuidora' CREATEDB;
  ELSE
    ALTER ROLE distribuidora WITH LOGIN PASSWORD 'distribuidora';
  END IF;
END
\$\$;
CREATE DATABASE distribuidora OWNER distribuidora;
GRANT ALL ON DATABASE distribuidora TO distribuidora;
EOF
    Write-Ok "DB 'distribuidora' creada con user/password distribuidora"
  } else {
    Write-Ok "DB 'distribuidora' ya existe"
    # Asegurar que el user existe y tiene la password correcta
    docker exec -i distribuidora-postgres psql -U $realUser -d postgres <<EOF
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'distribuidora') THEN
    CREATE ROLE distribuidora WITH LOGIN PASSWORD 'distribuidora' CREATEDB;
  ELSE
    ALTER ROLE distribuidora WITH LOGIN PASSWORD 'distribuidora';
  END IF;
END
\$\$;
GRANT ALL ON DATABASE distribuidora TO distribuidora;
EOF
    Write-Ok "User 'distribuidora' configurado"
  }

  Write-Host "`nListo. Ahora podés levantar el backend con:" -ForegroundColor Green
  Write-Host "  cd D:\programacion\distribuidora && .\mvnw spring-boot:run" -ForegroundColor White
  exit 0
}

# No hay nada
Write-Step "No hay container verde corriendo. Levantando distribuidora-postgres"
docker compose up -d
Write-Ok "Container distribuidora-postgres levantado (init script crea user/db)"
Write-Host "`nListo. Ahora podés levantar el backend con:" -ForegroundColor Green
Write-Host "  cd D:\programacion\distribuidora && .\mvnw spring-boot:run" -ForegroundColor White
