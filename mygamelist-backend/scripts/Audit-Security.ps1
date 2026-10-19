$ErrorActionPreference = 'Stop'
$backendRoot = Split-Path $PSScriptRoot -Parent
$envFile = Join-Path $backendRoot '.env'

# The audit only needs the NVD key, not SMTP/database/runtime credentials.
# Load it inside this process so Maven receives it even with powershell -File.
if (Test-Path $envFile) {
    $entries = @(Get-Content $envFile -Encoding UTF8 | Where-Object { $_ -match '^\s*NVD_API_KEY\s*=' })
    if ($entries.Count -gt 1) { throw 'Ha mais de uma NVD_API_KEY no .env. Mantenha somente uma.' }
    if ($entries.Count -eq 1) {
        $value = ($entries[0] -split '=', 2)[1].Trim()
        if ($value.Length -ge 2) {
            if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
                ($value.StartsWith("'") -and $value.EndsWith("'"))) {
                $value = $value.Substring(1, $value.Length - 2)
            }
        }
        $env:NVD_API_KEY = $value
    }
}
if ([string]::IsNullOrWhiteSpace($env:NVD_API_KEY)) {
    throw 'Configure NVD_API_KEY no .env do backend ou no ambiente antes de executar. Nao envie a chave em logs.'
}
$previousTelemetry = $env:SCARF_NO_ANALYTICS
$env:SCARF_NO_ANALYTICS = 'true'
Push-Location $backendRoot
try {
    & .\mvnw.cmd -Psecurity-audit -DskipTests '-DnvdApiKeyEnvironmentVariable=NVD_API_KEY' verify
    if ($LASTEXITCODE -ne 0) {
        throw 'Auditoria nao aprovada. Confira o erro Maven e target/dependency-check-report.html quando gerado.'
    }
    Write-Host 'Auditoria aprovada. Relatorio: target/dependency-check-report.html'
} finally {
    Pop-Location
    if ($null -eq $previousTelemetry) {
        Remove-Item Env:SCARF_NO_ANALYTICS -ErrorAction SilentlyContinue
    } else { $env:SCARF_NO_ANALYTICS = $previousTelemetry }
}
