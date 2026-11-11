$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

function New-Secret([int]$bytes) {
    $buffer = New-Object byte[] $bytes
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($buffer)
    [Convert]::ToBase64String($buffer)
}

$envPath = Join-Path $PSScriptRoot ".env"

if (-not (Test-Path $envPath)) {
    Write-Host "Creating .env with generated secrets"
    $password = (New-Secret 24) -replace '[^A-Za-z0-9]', ''
    $grafana = (New-Secret 24) -replace '[^A-Za-z0-9]', ''
    $lines = "POSTGRES_PASSWORD=$password`nJWT_SECRET=$(New-Secret 48)`nGRAFANA_ADMIN_PASSWORD=$grafana`n"
    [IO.File]::WriteAllText($envPath, $lines)
}
elseif (-not (Select-String -Path $envPath -Pattern '^GRAFANA_ADMIN_PASSWORD=' -Quiet)) {
    Write-Host "Adding GRAFANA_ADMIN_PASSWORD to the existing .env"
    $grafana = (New-Secret 24) -replace '[^A-Za-z0-9]', ''
    $existing = [IO.File]::ReadAllText($envPath)
    $prefix = ""
    if ($existing.Length -gt 0 -and -not $existing.EndsWith("`n")) { $prefix = "`n" }
    [IO.File]::AppendAllText($envPath, "${prefix}GRAFANA_ADMIN_PASSWORD=$grafana`n")
}

docker compose up --build -d --wait --wait-timeout 180
if ($LASTEXITCODE -ne 0) {
    Write-Host "The stack did not become healthy in time. Check: docker compose ps, docker compose logs"
    exit 1
}

Write-Host ""
Write-Host "PulseWatch is up:"
Write-Host "  Web app:     http://localhost:8080"
Write-Host "  API:         http://localhost:8081"
Write-Host "  Prometheus:  http://localhost:9090"
Write-Host "  Grafana:     http://localhost:3001  (user: admin, password: GRAFANA_ADMIN_PASSWORD in .env)"
