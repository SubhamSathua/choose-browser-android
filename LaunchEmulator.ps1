Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# ===== Config =====
$AmdName = "Pixel_4a"
$AssembleTask = ":app:assembleNormalDebug"
$PackageName = "com.hyper.choosebrowsernew"
$DeviceTimeoutSec = 90
$FallbackSdk = "E:\InstalledApplications\AppData\Android\SDK"
$ApkPath = Join-Path $PSScriptRoot "app\build\outputs\apk\normal\debug\app-normal-debug.apk"

# Prefer env vars if valid, otherwise fallback to known SDK path.
$candidateSdk = if (-not [string]::IsNullOrWhiteSpace($env:ANDROID_SDK_ROOT)) {
    $env:ANDROID_SDK_ROOT
} elseif (-not [string]::IsNullOrWhiteSpace($env:ANDROID_HOME)) {
    $env:ANDROID_HOME
} else {
    $FallbackSdk
}

$candidateAdb = Join-Path $candidateSdk "platform-tools\adb.exe"
$SDK = if (Test-Path $candidateAdb) { $candidateSdk } else { $FallbackSdk }

$ADB = Join-Path $SDK "platform-tools\adb.exe"
$EMU = Join-Path $SDK "emulator\emulator.exe"
$GRADLE = Join-Path $PSScriptRoot "gradlew.bat"

if (-not (Test-Path $ADB)) { throw "adb not found at: $ADB" }
if (-not (Test-Path $EMU)) { throw "emulator not found at: $EMU" }
if (-not (Test-Path $GRADLE)) { throw "gradlew.bat not found at: $GRADLE" }

function Get-RunningEmulatorSerial {
    $lines = & $ADB devices
    foreach ($line in $lines) {
        if ($line -match '^(emulator-\d+)\s+device$') {
            return $Matches[1]
        }
    }
    return $null
}

function Wait-ForEmulatorDevice([int]$timeoutSec) {
    $start = Get-Date
    while ($true) {
        $serial = Get-RunningEmulatorSerial
        if ($serial) { return $serial }

        $elapsed = (Get-Date) - $start
        if ($elapsed.TotalSeconds -ge $timeoutSec) {
            throw "Timed out waiting for emulator device state after $timeoutSec seconds."
        }
        Start-Sleep -Seconds 2
    }
}

function Wait-ForDeviceProperties([string]$serial, [int]$timeoutSec) {
    $start = Get-Date
    while ($true) {
        $sdkProp = (& $ADB -s $serial shell getprop ro.build.version.sdk 2>$null)
        $sdk = ($sdkProp -join "").Trim()
        if ($sdk -match '^\d+$') { return }

        $elapsed = (Get-Date) - $start
        if ($elapsed.TotalSeconds -ge $timeoutSec) {
            throw "Timed out waiting for device properties from $serial after $timeoutSec seconds."
        }
        Start-Sleep -Seconds 2
    }
}

Write-Host "Using SDK: $SDK"
& $ADB start-server | Out-Null

# Start emulator only if none is running.
$serial = Get-RunningEmulatorSerial
if (-not $serial) {
    Write-Host "No running emulator device found. Starting AVD: $AmdName"
    Start-Process -FilePath $EMU -ArgumentList '-avd', $AmdName, '-no-snapshot-load', '-scale', '0.7' | Out-Null
    $serial = Wait-ForEmulatorDevice -timeoutSec $DeviceTimeoutSec
} else {
    Write-Host "Using running emulator: $serial"
}

Write-Host "Emulator device ready: $serial"
Write-Host "Waiting for emulator properties..."
Wait-ForDeviceProperties -serial $serial -timeoutSec $DeviceTimeoutSec

Write-Host "Building APK via $AssembleTask ..."
& $GRADLE $AssembleTask
if ($LASTEXITCODE -ne 0) { throw "Gradle assemble failed with exit code $LASTEXITCODE" }

if (-not (Test-Path $ApkPath)) {
    throw "APK not found at: $ApkPath"
}

Write-Host "Installing APK via adb install -r ..."
& $ADB -s $serial install -r $ApkPath
if ($LASTEXITCODE -ne 0) { throw "adb install failed with exit code $LASTEXITCODE" }

Write-Host "Launching package: $PackageName"
& $ADB shell monkey -p $PackageName -c android.intent.category.LAUNCHER 1
if ($LASTEXITCODE -ne 0) {
    throw "App launch failed for package: $PackageName"
}

Write-Host "Done. App launched on emulator." -ForegroundColor Green