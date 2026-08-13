param(
    [string]$MysqlExe = "mysql",
    [string]$HostName = "127.0.0.1",
    [int]$Port = 3306,
    [string]$User = "root",
    [string]$Password = "",
    [string]$TestDatabase = "codex_production_center_migration_test",
    [switch]$KeepDatabase
)

$ErrorActionPreference = "Stop"
$migrationFile = (Resolve-Path (Join-Path $PSScriptRoot "..\migration-production-record-processing-center-2026-08-13.sql")).Path

if ($TestDatabase -notmatch '^codex_[a-zA-Z0-9_]+$') {
    throw "Refusing to recreate non-test database: $TestDatabase"
}

$previousMysqlPassword = $env:MYSQL_PWD
$env:MYSQL_PWD = $Password

function Invoke-MySql {
    param(
        [Parameter(Mandatory)] [string]$Sql,
        [switch]$WithoutDatabase,
        [switch]$ExpectFailure
    )

    $arguments = @(
        "--protocol=tcp",
        "--host=$HostName",
        "--port=$Port",
        "--user=$User",
        "--default-character-set=utf8mb4",
        "--batch",
        "--skip-column-names"
    )
    if (-not $WithoutDatabase) {
        $arguments += $TestDatabase
    }

    $output = $Sql | & $MysqlExe @arguments 2>&1
    $exitCode = $LASTEXITCODE
    if ($ExpectFailure) {
        if ($exitCode -eq 0) {
            throw "Expected MySQL command to fail, but it succeeded."
        }
    } elseif ($exitCode -ne 0) {
        throw "MySQL command failed ($exitCode): $($output -join [Environment]::NewLine)"
    }
    return @($output)
}

function Invoke-Migration {
    param([switch]$ExpectFailure)
    $sql = Get-Content -LiteralPath $migrationFile -Raw -Encoding UTF8
    Invoke-MySql -Sql $sql -ExpectFailure:$ExpectFailure
}

function Assert-Scalar {
    param(
        [Parameter(Mandatory)] [string]$Sql,
        [Parameter(Mandatory)] [string]$Expected,
        [Parameter(Mandatory)] [string]$Message
    )
    $actual = (Invoke-MySql -Sql $Sql | Select-Object -Last 1).ToString().Trim()
    if ($actual -ne $Expected) {
        throw "$Message. Expected '$Expected', got '$actual'."
    }
}

function Reset-Records {
    Invoke-MySql -Sql @"
DELETE FROM production_record;
UPDATE order_main SET center_id = CASE id WHEN 100 THEN 1 ELSE NULL END;
UPDATE device SET center_id = 1 WHERE id = 1000;
UPDATE sys_user SET center_id = CASE id WHEN 10 THEN 1 WHEN 20 THEN 2 ELSE center_id END;
"@ | Out-Null
}

try {
    Invoke-MySql -WithoutDatabase -Sql @"
DROP DATABASE IF EXISTS $TestDatabase;
CREATE DATABASE $TestDatabase CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE $TestDatabase;
CREATE TABLE processing_center (id BIGINT PRIMARY KEY, center_name VARCHAR(100), status INT, is_deleted TINYINT);
CREATE TABLE sys_user (id BIGINT PRIMARY KEY, center_id BIGINT, is_deleted TINYINT);
CREATE TABLE order_main (id BIGINT PRIMARY KEY, center_id BIGINT, is_deleted TINYINT);
CREATE TABLE device (id BIGINT PRIMARY KEY, center_id BIGINT, is_deleted TINYINT);
CREATE TABLE production_record (
    id BIGINT PRIMARY KEY,
    record_no VARCHAR(100),
    order_id BIGINT,
    producer_id BIGINT,
    print_device_id BIGINT,
    processing_center_id BIGINT,
    processing_center_name VARCHAR(100),
    update_time DATETIME NULL,
    is_deleted TINYINT
);
INSERT INTO processing_center VALUES
    (1, 'Alpha Center', 1, 0),
    (2, 'Beta Center', 1, 0),
    (3, 'Disabled Center', 0, 0);
INSERT INTO sys_user VALUES (10, 1, 0), (20, 2, 0);
INSERT INTO order_main VALUES (100, 1, 0), (200, NULL, 0);
INSERT INTO device VALUES (1000, 1, 0);
"@ | Out-Null

    Write-Host "[1/6] normal backfill, existing-ID preservation, and claimed-only scope"
    Invoke-MySql -Sql @"
INSERT INTO production_record VALUES
    (1, 'PR-ORDER', 100, 10, NULL, NULL, NULL, NULL, 0),
    (2, 'PR-DEVICE', 200, 10, 1000, NULL, '', NULL, 0),
    (3, 'PR-KEEP-ID', 100, 10, 1000, 1, '', NULL, 0),
    (4, 'PR-UNCLAIMED', 100, NULL, NULL, NULL, NULL, NULL, 0);
"@ | Out-Null
    Invoke-Migration | Out-Null
    Assert-Scalar -Sql "SELECT COUNT(*) FROM production_record WHERE id IN (1,2,3) AND processing_center_id=1 AND processing_center_name='Alpha Center';" -Expected "3" -Message "Backfill did not repair every claimed record"
    Assert-Scalar -Sql "SELECT COUNT(*) FROM production_record WHERE id=4 AND processing_center_id IS NULL AND processing_center_name IS NULL;" -Expected "1" -Message "Unclaimed record was modified"

    Write-Host "[2/6] second-run idempotency"
    $secondRun = Invoke-Migration
    if (($secondRun -join "`n") -notmatch "0\s+0\s+processing-center backfill committed") {
        throw "Second run did not report zero candidates."
    }
    Assert-Scalar -Sql "SELECT processing_center_id FROM production_record WHERE id=3;" -Expected "1" -Message "Existing center ID was overwritten"

    Write-Host "[3/6] conflicting sources abort without modification"
    Reset-Records
    Invoke-MySql -Sql @"
UPDATE device SET center_id=2 WHERE id=1000;
INSERT INTO production_record VALUES (11, 'PR-CONFLICT', 100, 10, 1000, NULL, NULL, NULL, 0);
"@ | Out-Null
    Invoke-Migration -ExpectFailure | Out-Null
    Assert-Scalar -Sql "SELECT COUNT(*) FROM production_record WHERE id=11 AND processing_center_id IS NULL AND processing_center_name IS NULL;" -Expected "1" -Message "Conflict path modified data"

    Write-Host "[4/6] unresolved candidate aborts the whole batch"
    Reset-Records
    Invoke-MySql -Sql @"
INSERT INTO production_record VALUES
    (21, 'PR-VALID-BUT-ROLLBACK', 100, 10, NULL, NULL, NULL, NULL, 0),
    (22, 'PR-UNRESOLVED', NULL, 999, NULL, NULL, NULL, NULL, 0);
"@ | Out-Null
    Invoke-Migration -ExpectFailure | Out-Null
    Assert-Scalar -Sql "SELECT COUNT(*) FROM production_record WHERE id IN (21,22) AND processing_center_id IS NULL AND processing_center_name IS NULL;" -Expected "2" -Message "Unresolved path did not preserve the batch"

    Write-Host "[5/6] disabled center aborts without modification"
    Reset-Records
    Invoke-MySql -Sql @"
UPDATE order_main SET center_id=3 WHERE id=100;
UPDATE sys_user SET center_id=3 WHERE id=10;
INSERT INTO production_record VALUES (31, 'PR-DISABLED-CENTER', 100, 10, NULL, NULL, NULL, NULL, 0);
"@ | Out-Null
    Invoke-Migration -ExpectFailure | Out-Null
    Assert-Scalar -Sql "SELECT COUNT(*) FROM production_record WHERE id=31 AND processing_center_id IS NULL AND processing_center_name IS NULL;" -Expected "1" -Message "Invalid-center path modified data"

    Write-Host "[6/6] fixed candidate set under a concurrent claim"
    Reset-Records
    Invoke-MySql -Sql @"
INSERT INTO production_record VALUES
    (41, 'PR-LOCKED-CANDIDATE', 100, 10, NULL, NULL, NULL, NULL, 0),
    (42, 'PR-CONCURRENT-CLAIM', 100, NULL, NULL, NULL, NULL, NULL, 0);
"@ | Out-Null

    $jobArguments = @("--protocol=tcp", "--host=$HostName", "--port=$Port", "--user=$User", "--default-character-set=utf8mb4", "--batch", "--skip-column-names", $TestDatabase)
    $locker = Start-Job -ScriptBlock {
        param($Exe, $Arguments, $MysqlPassword)
        $env:MYSQL_PWD = $MysqlPassword
        "START TRANSACTION; SELECT id FROM production_record WHERE id=41 FOR UPDATE; SELECT SLEEP(4); COMMIT;" | & $Exe @Arguments
        if ($LASTEXITCODE -ne 0) { throw "Locker connection failed: $LASTEXITCODE" }
    } -ArgumentList $MysqlExe, $jobArguments, $Password
    Start-Sleep -Milliseconds 500

    $migrationJob = Start-Job -ScriptBlock {
        param($Exe, $Arguments, $MysqlPassword, $MigrationPath)
        $env:MYSQL_PWD = $MysqlPassword
        Get-Content -LiteralPath $MigrationPath -Raw -Encoding UTF8 | & $Exe @Arguments
        if ($LASTEXITCODE -ne 0) { throw "Concurrent migration failed: $LASTEXITCODE" }
    } -ArgumentList $MysqlExe, $jobArguments, $Password, $migrationFile
    Start-Sleep -Milliseconds 1000
    Invoke-MySql -Sql "UPDATE production_record SET producer_id=10 WHERE id=42;" | Out-Null
    Wait-Job -Job $locker, $migrationJob | Out-Null
    Receive-Job -Job $locker, $migrationJob -ErrorAction Stop | Out-Null
    Remove-Job -Job $locker, $migrationJob

    Assert-Scalar -Sql "SELECT COUNT(*) FROM production_record WHERE id=41 AND processing_center_id=1 AND processing_center_name='Alpha Center';" -Expected "1" -Message "Locked candidate was not migrated"
    Assert-Scalar -Sql @"
SELECT COUNT(*)
FROM production_record
WHERE id=42
  AND producer_id=10
  AND ((processing_center_id IS NULL AND processing_center_name IS NULL)
       OR (processing_center_id=1 AND processing_center_name='Alpha Center'));
"@ -Expected "1" -Message "Concurrent claim produced a partial or inconsistent center snapshot"
    Invoke-Migration | Out-Null
    Assert-Scalar -Sql "SELECT COUNT(*) FROM production_record WHERE id=42 AND processing_center_id=1 AND processing_center_name='Alpha Center';" -Expected "1" -Message "Concurrent claim was not repaired on the next run"

    Write-Host "PASS: migration verified against MySQL 8-compatible server."
}
finally {
    if (-not $KeepDatabase) {
        try {
            Invoke-MySql -WithoutDatabase -Sql "DROP DATABASE IF EXISTS $TestDatabase;" | Out-Null
        } catch {
            Write-Warning "Failed to remove test database '$TestDatabase': $_"
        }
    }
    $env:MYSQL_PWD = $previousMysqlPassword
}
