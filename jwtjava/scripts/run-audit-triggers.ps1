# Chạy add_audit_triggers.sql trên MySQL Docker (jwtjava).
# Usage: .\scripts\run-audit-triggers.ps1
#        .\scripts\run-audit-triggers.ps1 -ContainerName jwtjava-mysql -DbPassword secret

param(
    [string]$ContainerName = "",
    [string]$DbUser = "root",
    [string]$DbPassword = "root",
    [string]$Database = "jwtjava"
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$sqlFile = Join-Path $repoRoot "jwtjava\src\main\resources\sql\add_audit_triggers.sql"

if (-not (Test-Path $sqlFile)) {
    Write-Error "Không tìm thấy file SQL: $sqlFile"
}

try {
    docker info *> $null
} catch {
    Write-Error @"
Docker chưa chạy. Hãy mở Docker Desktop, đợi icon Docker xanh, rồi chạy lại script này.
"@
}

if ([string]::IsNullOrWhiteSpace($ContainerName)) {
    $containers = docker ps --format "{{.Names}}|{{.Ports}}" | Where-Object { $_ -match ":3307->" -or $_ -match "3307/tcp" }
    if ($containers.Count -eq 0) {
        Write-Error @"
Không tìm thấy container MySQL map port 3307.
Chạy: docker ps
Hoặc chỉ định tên: .\scripts\run-audit-triggers.ps1 -ContainerName <ten_container>
"@
    }
    $ContainerName = ($containers[0] -split '\|')[0]
}

Write-Host "Container: $ContainerName"
Write-Host "Database:  $Database"
Write-Host "SQL file:  $sqlFile"

docker cp $sqlFile "${ContainerName}:/tmp/add_audit_triggers.sql"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

docker exec $ContainerName mysql -u $DbUser "-p$DbPassword" $Database -e "source /tmp/add_audit_triggers.sql"
if ($LASTEXITCODE -ne 0) {
    Write-Warning "Có thể một phần đã chạy trước đó (duplicate column/index). Kiểm tra bên dưới."
}

Write-Host ""
Write-Host "=== Kiểm tra ===" -ForegroundColor Cyan
docker exec $ContainerName mysql -u $DbUser "-p$DbPassword" $Database -e "SHOW TABLES LIKE '%history%'; SHOW TRIGGERS LIKE 'customer_orders';"

Write-Host ""
Write-Host "Xong. Nếu thấy order_status_history + trigger trg_orders_status_audit_insert là OK." -ForegroundColor Green
