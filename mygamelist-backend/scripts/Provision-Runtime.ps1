$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/Load-Env.ps1"
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    docker compose --env-file .env up -d --wait postgres
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao iniciar PostgreSQL.' }
    $tables = docker compose --env-file .env exec -T postgres psql -U postgres -d mygamelist -tAc "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND table_name='tb_users'"
    if ($LASTEXITCODE -ne 0 -or ($tables -join '').Trim() -ne '1') { throw 'Inicialize o schema pelo Start-Local antes de provisionar o usuario de runtime.' }
    if ([string]::IsNullOrWhiteSpace($env:DATABASE_RUNTIME_PASSWORD)) {
        $bytes = New-Object byte[] 32
        $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
        try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
        $env:DATABASE_RUNTIME_PASSWORD = [Convert]::ToBase64String($bytes)
    }
    $env:MGL_RUNTIME_PASSWORD = $env:DATABASE_RUNTIME_PASSWORD
    docker compose --env-file .env cp scripts/006_runtime_role.sql postgres:/tmp/006_runtime_role.sql
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao copiar SQL de runtime.' }
    # Value is inherited from the process environment, not interpolated into SQL text.
    docker compose --env-file .env exec -T -e MGL_RUNTIME_PASSWORD postgres psql -U postgres -d mygamelist -v ON_ERROR_STOP=1 -f /tmp/006_runtime_role.sql
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao provisionar usuario. .env nao alterado.' }
    $contents = Get-Content .env -Raw -Encoding UTF8
    $settings = [ordered]@{ DATABASE_USERNAME='mygamelist_app'; DATABASE_RUNTIME_PASSWORD=$env:DATABASE_RUNTIME_PASSWORD; SPRING_PROFILES_ACTIVE='hardened'; JPA_DDL_AUTO='validate'; COMMUNITY_AUTO_MIGRATE='false' }
    foreach ($entry in $settings.GetEnumerator()) {
        $pattern = '(?m)^' + [regex]::Escape($entry.Key) + '=.*$'
        $contents = [regex]::Replace($contents, $pattern, '')
        $contents = $contents.TrimEnd() + "`r`n" + $entry.Key + '=' + $entry.Value + "`r`n"
    }
    [IO.File]::WriteAllText((Join-Path (Get-Location) '.env'), $contents, (New-Object Text.UTF8Encoding($false)))
    Write-Host 'Usuario de runtime configurado. Reinicie o backend via Start-Local.ps1.'
} finally {
    Remove-Item Env:MGL_RUNTIME_PASSWORD -ErrorAction SilentlyContinue
    Pop-Location
}
