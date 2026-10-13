$ErrorActionPreference='Stop'
. "$PSScriptRoot/Load-Env.ps1"
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    docker compose --env-file .env up -d --wait --wait-timeout 180
    if($LASTEXITCODE -ne 0){throw 'Infraestrutura indisponivel.'}
    $exists=@(docker compose --env-file .env exec -T postgres psql -U postgres -d postgres -tAc "SELECT COUNT(*) FROM pg_database WHERE datname='mygamelist_critical_tests'")
    if($LASTEXITCODE -ne 0){throw 'Falha ao consultar banco de teste.'}
    $databaseCount = $exists -join ''
    if ($databaseCount -notmatch '^\s*[01]\s*$') { throw 'Resposta inesperada ao consultar banco de teste.' }
    if ($databaseCount -match '^\s*0\s*$') {
        docker compose --env-file .env exec -T postgres createdb -U postgres mygamelist_critical_tests
        if($LASTEXITCODE -ne 0){throw 'Falha ao criar banco de teste.'}
    }
    $env:RUN_CRITICAL_TESTS='true'
    & ./mvnw.cmd '-Dtest=CriticalFlowsTest' test
    if($LASTEXITCODE -ne 0){throw 'Testes criticos falharam. Consulte target/surefire-reports.'}
} finally {
    Remove-Item Env:RUN_CRITICAL_TESTS -ErrorAction SilentlyContinue
    Pop-Location
}
