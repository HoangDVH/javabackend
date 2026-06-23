# Chạy index + trigger trên MySQL Docker local.
# Usage: .\scripts\run-all-db-migrations.ps1
#        .\scripts\run-all-db-migrations.ps1 -ContainerName mysql-8.0.46

param(
    [string]$ContainerName = "mysql-8.0.46",
    [string]$DbUser = "root",
    [string]$DbPassword = "root",
    [string]$Database = "jwtjava"
)

$moduleRoot = Split-Path $PSScriptRoot -Parent
$indexesSql = Join-Path $moduleRoot "src\main\resources\sql\add_query_indexes.sql"
$triggersSql = Join-Path $moduleRoot "src\main\resources\sql\add_audit_triggers.sql"

docker start $ContainerName 2>$null | Out-Null
Start-Sleep -Seconds 3

docker cp $indexesSql "${ContainerName}:/tmp/add_query_indexes.sql"
docker exec $ContainerName mysql -u $DbUser "-p$DbPassword" $Database -e "source /tmp/add_query_indexes.sql"

docker cp $triggersSql "${ContainerName}:/tmp/add_audit_triggers.sql"
docker exec $ContainerName mysql -u $DbUser "-p$DbPassword" $Database -e "source /tmp/add_audit_triggers.sql"

Write-Host "=== Indexes ===" -ForegroundColor Cyan
docker exec $ContainerName mysql -u $DbUser "-p$DbPassword" $Database -N -e `
    "SELECT TABLE_NAME, INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='$Database' AND INDEX_NAME LIKE 'idx_%' GROUP BY TABLE_NAME, INDEX_NAME;"

Write-Host "=== Triggers ===" -ForegroundColor Cyan
docker exec $ContainerName mysql -u $DbUser "-p$DbPassword" $Database -e "SHOW TRIGGERS FROM $Database;"
