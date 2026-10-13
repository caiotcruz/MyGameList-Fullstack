$ErrorActionPreference = 'Stop'
$backendRoot = Split-Path $PSScriptRoot -Parent
$envFile = Join-Path $backendRoot '.env'
if (-not (Test-Path $envFile)) {
    $oldFile = Join-Path (Split-Path $backendRoot -Parent) '.env'
    if (Test-Path $oldFile) { Copy-Item $oldFile $envFile }
    else { Copy-Item (Join-Path $backendRoot '.env.example') $envFile }
}
$text = [IO.File]::ReadAllText($envFile)
function New-Secret {
    $bytes = New-Object byte[] 32
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return [Convert]::ToBase64String($bytes)
}
foreach ($key in @('JWT_SECRET','REDIS_PASSWORD','RABBITMQ_PASSWORD','OUTBOX_KEY')) {
    $match = [regex]::Match($text, "(?m)^${key}=([^`r`n]*)")
    if (-not $match.Success) { $text += "`r`n${key}=$(New-Secret)" }
    elseif ([string]::IsNullOrWhiteSpace($match.Groups[1].Value) -or $match.Groups[1].Value.Trim() -eq 'CHANGE_ME') {
        $text = [regex]::Replace($text, "(?m)^${key}=[^`r`n]*", "${key}=$(New-Secret)")
    }
}
if ($text -notmatch '(?m)^RABBITMQ_USERNAME=') { $text += "`r`nRABBITMQ_USERNAME=mygamelist" }
[IO.File]::WriteAllText($envFile, $text+"`r`n", (New-Object Text.UTF8Encoding($false)))
Write-Host 'Ambiente preparado. Confira DATABASE_PASSWORD, RAWG_API_KEY, SMTP_USERNAME e SMTP_PASSWORD no .env.'
Write-Host 'Segredos existentes foram preservados. Guarde OUTBOX_KEY junto do backup, separadamente dos dados.'
