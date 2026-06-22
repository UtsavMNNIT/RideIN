# =============================================================================
# Run the RideFlow backend natively with Maven, pointed at the Docker infra
# (Postgres + Redis + Kafka). Frontend is run separately with `npm run dev`.
#
#   PS D:\RideIN\backend> .\run-local.ps1
#
# Each service opens in its own minimized PowerShell window; close a window
# (or Ctrl-C in it) to stop that service. Re-run this script to restart.
# =============================================================================
$ErrorActionPreference = "Stop"
$backend = $PSScriptRoot
$root    = Split-Path $backend -Parent

# --- 1. Load .env so DB role passwords match how Postgres was initialized -----
$envFile = Join-Path $root ".env"
if (Test-Path $envFile) {
  Get-Content $envFile | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object {
    $k, $v = $_ -split '=', 2
    [Environment]::SetEnvironmentVariable($k.Trim(), $v.Trim())
  }
  Write-Host "Loaded env from $envFile"
} else {
  Write-Host "No .env found — services will use compose default passwords (rider_pass, etc.)"
}

# --- 2. Point services at the HOST-published infra ports ----------------------
# (override the in-container hostname defaults baked into each application.yml)
$env:POSTGRES_HOST            = "localhost"        # host 5432
$env:KAFKA_BOOTSTRAP_INTERNAL = "localhost:29092"  # Kafka EXTERNAL listener
$env:REDIS_GEO_HOST           = "localhost"; $env:REDIS_GEO_PORT   = "6379"
$env:REDIS_CACHE_HOST         = "localhost"; $env:REDIS_CACHE_PORT = "6380"  # host maps 6380->6379

# Gateway must route to services on localhost (not the compose service names).
$env:RIDER_SERVICE_URI    = "http://localhost:8081"
$env:DRIVER_SERVICE_URI   = "http://localhost:8083"
$env:LOCATION_SERVICE_URI = "http://localhost:8082"
$env:MATCHING_SERVICE_URI = "http://localhost:8084"
$env:PRICING_SERVICE_URI  = "http://localhost:8085"
$env:TRIP_SERVICE_URI     = "http://localhost:8087"
$env:PAYMENT_SERVICE_URI  = "http://localhost:8088"

# --- 3. Free the app-service ports if the dockerized stack is running ---------
Write-Host "Stopping dockerized app services (keeping infra)..."
docker compose -f "$root\docker-compose.yml" stop `
  api-gateway rider-service driver-service location-service matching-service `
  pricing-service trip-service notification-service payment-service frontend 2>$null

# --- 4. Make sure the infra is up ---------------------------------------------
Write-Host "Starting infra (Postgres, Redis x2, Kafka)..."
docker compose -f "$root\docker-compose.yml" up -d postgres redis-geo redis-cache kafka kafka-init

# --- 5. Launch each service via Maven, one minimized window each --------------
$services = @(
  "rider-service", "driver-service", "location-service", "matching-service",
  "pricing-service", "trip-service", "notification-service", "payment-service", "api-gateway"
)
foreach ($s in $services) {
  Write-Host "Starting $s ..."
  Start-Process powershell `
    -ArgumentList "-NoExit", "-Command", "cd '$backend'; mvn -pl $s spring-boot:run" `
    -WindowStyle Minimized
}

Write-Host ""
Write-Host "All 9 backend services starting. Gateway: http://localhost:8080"
Write-Host "Now run the frontend:  cd ..\frontend ; npm run dev"
