$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path $PSScriptRoot -Parent
$backendDir = Join-Path $repoRoot 'afterend\campussecondhand'
$reviewDir = Join-Path $repoRoot '.review'
New-Item -ItemType Directory -Force -Path $reviewDir | Out-Null
$javaTempDir = Join-Path $reviewDir 'tmp'
New-Item -ItemType Directory -Force -Path $javaTempDir | Out-Null
$runtimeDir = Join-Path $reviewDir 'runtime'
New-Item -ItemType Directory -Force -Path $runtimeDir | Out-Null
$runtimeJar = Join-Path $runtimeDir 'campussecondhand-0.0.1-SNAPSHOT.jar'
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.3.1'
Push-Location $backendDir
try {
    & 'D:\apache-maven-3.9.12\bin\mvn.cmd' -o '-Dtest=OrderTradeServiceTest,MoneyAmountTest,AuthControllerTest,JwtUtilTest,RequestValueTest,FavoriteServiceTest,ImageValueTest,LayeringTest' package -q
    if ($LASTEXITCODE -ne 0) { throw 'Backend build failed.' }
    # Keep the existing service available until the replacement has passed its build.
    $listeners = @(Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue)
    foreach ($processId in @($listeners.OwningProcess | Select-Object -Unique)) {
        $existing = Get-CimInstance Win32_Process -Filter "ProcessId = $processId"
        if ($existing.Name -ne 'java.exe' -or $existing.CommandLine -notmatch 'campussecondhand-0\.0\.1-SNAPSHOT\.jar') {
            throw 'Port 8080 belongs to another application; refusing to stop it.'
        }
        Stop-Process -Id $processId -Force
    }
    Copy-Item -LiteralPath (Join-Path $backendDir 'target\campussecondhand-0.0.1-SNAPSHOT.jar') -Destination $runtimeJar -Force
    $backendProcess = Start-Process -FilePath "$env:JAVA_HOME\bin\java.exe" -ArgumentList @("-Djava.io.tmpdir=$javaTempDir", "-Djdk.net.unixdomain.tmpdir=$javaTempDir", '-jar', ('"' + $runtimeJar + '"')) -WorkingDirectory $backendDir -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $reviewDir 'backend.out.log') -RedirectStandardError (Join-Path $reviewDir 'backend.err.log')
    $ready = $false
    for ($attempt = 0; $attempt -lt 30; $attempt++) {
        Start-Sleep -Milliseconds 500
        try {
            $response = Invoke-RestMethod 'http://127.0.0.1:8080/api/stats' -TimeoutSec 2
            if ($response.code -eq 200) { $ready = $true; break }
        } catch { }
    }
    if (-not $ready) { throw 'Backend did not become ready; inspect .review/backend.err.log.' }
    @{ ready = $ready; processId = $backendProcess.Id; tests = 24 } | ConvertTo-Json
} finally { Pop-Location }
