$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/Load-Env.ps1"
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    docker compose --env-file .env up -d --wait --wait-timeout 180
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao iniciar infraestrutura.' }
    docker compose --env-file .env cp scripts/001_mail_outbox.sql postgres:/tmp/001_mail_outbox.sql
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao copiar migracao.' }
    docker compose --env-file .env exec -T postgres psql -U postgres -d mygamelist -v ON_ERROR_STOP=1 -f /tmp/001_mail_outbox.sql
    if ($LASTEXITCODE -ne 0) { throw 'Falha na migracao. Backend nao iniciado.' }
    & ./mvnw.cmd spring-boot:run
} finally { Pop-Location }
