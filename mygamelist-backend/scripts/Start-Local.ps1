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
    $hasUsers = docker compose --env-file .env exec -T postgres psql -U postgres -d mygamelist -tAc "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND table_name='tb_users'"
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao verificar schema existente.' }
    $schemaCount = ($hasUsers -join '').Trim()
    if ($schemaCount -notin @('0', '1')) { throw 'Resposta inesperada ao verificar schema. Backend nao iniciado.' }
    if ($schemaCount -eq '1') {
        docker compose --env-file .env cp scripts/003_themes_text.sql postgres:/tmp/003_themes_text.sql
        if ($LASTEXITCODE -ne 0) { throw 'Falha ao copiar migracao de temas e textos.' }
        docker compose --env-file .env exec -T postgres psql -U postgres -d mygamelist -v ON_ERROR_STOP=1 -f /tmp/003_themes_text.sql
        if ($LASTEXITCODE -ne 0) { throw 'Falha na migracao de temas e textos.' }
        docker compose --env-file .env cp scripts/004_recommendations_themes.sql postgres:/tmp/004_recommendations_themes.sql
        if ($LASTEXITCODE -ne 0) { throw 'Falha ao copiar migracao de recomendacoes.' }
        docker compose --env-file .env exec -T postgres psql -U postgres -d mygamelist -v ON_ERROR_STOP=1 -f /tmp/004_recommendations_themes.sql
        if ($LASTEXITCODE -ne 0) { throw 'Falha na migracao de recomendacoes.' }
    }
    & ./mvnw.cmd spring-boot:run
} finally { Pop-Location }
