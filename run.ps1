# ==========================================================================
# Cloud Identity Behavior Fingerprinting System — Run Helper (Windows)
# --------------------------------------------------------------------------
# Loads environment variables from a local ".env" file (if present), then
# builds and runs the Spring Boot application.
#
# Usage:
#   .\run.ps1              # load .env, build, and run
#   .\run.ps1 -SkipBuild   # load .env and run without rebuilding
#   .\run.ps1 -BuildOnly   # load .env and build only (mvn clean package)
#
# First time:  Copy .env.example to .env and fill in your values.
# ==========================================================================

param(
    [switch]$SkipBuild,
    [switch]$BuildOnly
)

$ErrorActionPreference = "Stop"
Set-Location -Path $PSScriptRoot

# ----- 1. Load .env (KEY=VALUE lines; '#' comments and blanks ignored) -----
$envFile = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFile) {
    Write-Host "[run] Loading environment from .env ..." -ForegroundColor Cyan
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq "" -or $line.StartsWith("#")) { return }
        $idx = $line.IndexOf("=")
        if ($idx -lt 1) { return }
        $key = $line.Substring(0, $idx).Trim()
        $val = $line.Substring($idx + 1).Trim()
        # Strip surrounding quotes if present
        if ($val.StartsWith('"') -and $val.EndsWith('"')) { $val = $val.Trim('"') }
        [System.Environment]::SetEnvironmentVariable($key, $val, "Process")
        Write-Host ("[run]   set {0}" -f $key) -ForegroundColor DarkGray
    }
} else {
    Write-Host "[run] No .env found — copy .env.example to .env and fill it in." -ForegroundColor Yellow
    Write-Host "[run] Continuing with system environment / application.yml defaults." -ForegroundColor Yellow
}

# ----- 2. Quick visibility into AWS wiring state -----
if ($env:AWS_ACCESS_KEY_ID) {
    Write-Host "[run] AWS static credentials detected (region=$($env:AWS_REGION))." -ForegroundColor Green
} else {
    Write-Host "[run] No static AWS keys — default credential chain will be used." -ForegroundColor Yellow
}

# ----- 3. Build -----
if (-not $SkipBuild) {
    Write-Host "[run] Building (mvn clean package)..." -ForegroundColor Cyan
    mvn clean package
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed with exit code $LASTEXITCODE" }
}

if ($BuildOnly) {
    Write-Host "[run] Build complete (BuildOnly). Exiting." -ForegroundColor Green
    return
}

# ----- 4. Run -----
$port = if ($env:SERVER_PORT) { $env:SERVER_PORT } else { "8080" }
Write-Host "[run] Starting Spring Boot (http://localhost:$port)..." -ForegroundColor Cyan
mvn spring-boot:run
