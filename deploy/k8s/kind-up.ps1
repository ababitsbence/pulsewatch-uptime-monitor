# Creates a local kind cluster and deploys PulseWatch into it, using images built from this checkout.
# Safe to run again: it reuses the cluster and the secret, rebuilds the images and restarts the pods.
$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$cluster = "pulsewatch"
$ctx = "kind-$cluster"
$ns = "pulsewatch"

function Invoke-Checked {
    param([string]$Command, [string[]]$Arguments)
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed: $Command $($Arguments -join ' ')"
    }
}

function New-Secret([int]$bytes) {
    $buffer = New-Object byte[] $bytes
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($buffer)
    [Convert]::ToBase64String($buffer)
}

foreach ($tool in "docker", "kind", "kubectl") {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
        throw "$tool is not installed or not on the PATH. Install kind and kubectl with: winget install Kubernetes.kind Kubernetes.kubectl"
    }
}

# kind prints "No kind clusters found." to stderr when there are none. Windows PowerShell treats
# redirected stderr output as an error and would stop the script, so tolerate it for this one call.
$previousPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$clusters = @(& kind get clusters 2>&1 | ForEach-Object { "$_" })
$ErrorActionPreference = $previousPreference
$clusterExisted = $clusters -contains $cluster
if (-not $clusterExisted) {
    Write-Host "Creating kind cluster '$cluster'"
    Invoke-Checked kind @("create", "cluster", "--name", $cluster)
}

Write-Host "Building images"
Invoke-Checked docker @("build", "-t", "pulsewatch-api:kind", (Join-Path $root "backend"))
Invoke-Checked docker @("build", "-t", "pulsewatch-web:kind", (Join-Path $root "frontend"))

Write-Host "Loading images into the cluster"
Invoke-Checked kind @("load", "docker-image", "pulsewatch-api:kind", "pulsewatch-web:kind", "--name", $cluster)

Invoke-Checked kubectl @("--context", $ctx, "apply", "-f", (Join-Path $root "deploy\k8s\base\namespace.yaml"))

$secret = & kubectl --context $ctx -n $ns get secret pulsewatch-secrets --ignore-not-found -o name
if (-not $secret) {
    Write-Host "Creating the pulsewatch-secrets Secret with generated values"
    $dbPassword = (New-Secret 24) -replace '[^A-Za-z0-9]', ''
    Invoke-Checked kubectl @("--context", $ctx, "-n", $ns, "create", "secret", "generic", "pulsewatch-secrets",
        "--from-literal=postgres-password=$dbPassword",
        "--from-literal=jwt-secret=$(New-Secret 48)")
}

Invoke-Checked kubectl @("--context", $ctx, "apply", "-k", (Join-Path $root "deploy\k8s\overlays\kind"))

if ($clusterExisted) {
    # The image tag is always "kind", so a changed image needs a restart to be picked up
    Invoke-Checked kubectl @("--context", $ctx, "-n", $ns, "rollout", "restart", "deployment/api", "deployment/web")
}

Write-Host "Waiting for the workloads to become ready"
Invoke-Checked kubectl @("--context", $ctx, "-n", $ns, "rollout", "status", "statefulset/db", "--timeout=240s")
Invoke-Checked kubectl @("--context", $ctx, "-n", $ns, "rollout", "status", "deployment/api", "--timeout=240s")
Invoke-Checked kubectl @("--context", $ctx, "-n", $ns, "rollout", "status", "deployment/web", "--timeout=240s")

Write-Host ""
Write-Host "PulseWatch is running in the kind cluster. To open it:"
Write-Host "  kubectl --context $ctx -n $ns port-forward svc/web 8080:8080"
Write-Host "  then open http://localhost:8080"
Write-Host "To remove everything: kind delete cluster --name $cluster"