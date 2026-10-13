$ErrorActionPreference = 'Stop'
$backendRoot = Split-Path $PSScriptRoot -Parent
$envFile = Join-Path $backendRoot '.env'
if (-not (Test-Path $envFile)) { throw "Crie e preencha $envFile antes de executar." }
Get-Content $envFile -Encoding UTF8 | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith('#')) {
        $parts = $line -split '=', 2
        if ($parts.Count -ne 2 -or $parts[0] -notmatch '^[A-Z][A-Z0-9_]*$') { throw 'Linha invalida no .env' }
        [Environment]::SetEnvironmentVariable($parts[0], $parts[1].Trim(), 'Process')
    }
}
foreach ($key in @('DATABASE_PASSWORD','JWT_SECRET','RAWG_API_KEY','SMTP_USERNAME','SMTP_PASSWORD','REDIS_PASSWORD','RABBITMQ_PASSWORD','OUTBOX_KEY')) {
    $value = [Environment]::GetEnvironmentVariable($key, 'Process')
    if ([string]::IsNullOrWhiteSpace($value) -or $value -eq 'CHANGE_ME') { throw "Preencha $key no .env" }
}
if ([Convert]::FromBase64String($env:OUTBOX_KEY).Length -ne 32) { throw 'OUTBOX_KEY precisa representar 32 bytes em Base64.' }
if ([Text.Encoding]::UTF8.GetByteCount($env:JWT_SECRET) -lt 32) { throw 'JWT_SECRET precisa ter pelo menos 32 bytes.' }
