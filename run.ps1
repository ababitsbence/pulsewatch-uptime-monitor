$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

function New-Secret([int]$bytes) {
    $buffer = New-Object byte[] $bytes
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($buffer)
    [Convert]::ToBase64String($buffer)
}

if (-not (Test-Path ".env")) {
    Write-Host "Creating .env with generated secrets"
    $password = (New-Secret 24) -replace '[^A-Za-z0-9]', ''
    $lines = "POSTGRES_PASSWORD=$password`nJWT_SECRET=$(New-Secret 48)`n"
    [IO.File]::WriteAllText((Join-Path $PSScriptRoot ".env"), $lines)
}

docker compose up --build -d
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Waiting for the web app..."
for ($i = 0; $i -lt 90; $i++) {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8080/" -TimeoutSec 2
        if ($response.StatusCode -eq 200) {
            Write-Host "PulseWatch is up: http://localhost:8080  (API: http://localhost:8081)"
            exit 0
        }
    } catch {
        Start-Sleep -Seconds 2
    }
}

Write-Host "The web app did not become healthy in time. Check: docker compose logs"
exit 1