# Choose Browser Version Manager
# Manages version.properties, version_history.json, and app-updates manifest
param (
    [ValidateSet("major", "minor", "patch", "custom", "")]
    [string]$Type = "",
    [string]$CustomVersion = "",
    [string]$CommitMessage = "",
    [switch]$NoCommit,
    [switch]$NoPush,
    [switch]$UpdateManifest = $true
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

$PropsPath = Join-Path $ScriptDir "version.properties"
$HistoryPath = Join-Path $ScriptDir "version_history.json"
$ManifestPath = Join-Path $ScriptDir "app-updates\choose-browser\update.json"

if (-not (Test-Path $PropsPath)) {
    Write-Error "version.properties not found at $PropsPath"
    exit 1
}

# 1. Read current version from version.properties
$propsContent = Get-Content -Path $PropsPath -Raw
$currentMajor = 4
$currentX = 1
$currentSub = 1

if ($propsContent -match "VERSION_MAJOR=(\d+)") { $currentMajor = [int]$Matches[1] }
if ($propsContent -match "VERSION_X=(\d+)") { $currentX = [int]$Matches[1] }
if ($propsContent -match "VERSION_SUB=(\d+)") { $currentSub = [int]$Matches[1] }

$formattedCurrent = "{0}.{1:D2}.{2}" -f $currentMajor, $currentX, $currentSub
$currentCode = 26031000 + ($currentMajor * 10000) + ($currentX * 100) + $currentSub

# 2. Read version history
$history = @()
if (Test-Path $HistoryPath) {
    try {
        $historyJson = Get-Content -Path $HistoryPath -Raw
        $history = ConvertFrom-Json $historyJson
        if ($history -isnot [System.Collections.IEnumerable]) {
            $history = @($history)
        }
    } catch {
        $history = @()
    }
}

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "     CHOOSE BROWSER VERSION MANAGER      " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host " Current Version Name : $formattedCurrent" -ForegroundColor Yellow
Write-Host " Current Version Code : $currentCode" -ForegroundColor Yellow
Write-Host " History Entries      : $($history.Count)" -ForegroundColor Yellow
Write-Host "=========================================" -ForegroundColor Cyan

# 3. Determine bump type
$selectedType = $Type.ToLower()
if ([string]::IsNullOrWhiteSpace($selectedType)) {
    Write-Host ""
    Write-Host "Select version bump type:" -ForegroundColor Green
    Write-Host " [1] Major  (xx+1 . 00 . 00) - Breaking / Major milestone"
    Write-Host " [2] Minor  (xx . yy+1 . 00) - New features"
    Write-Host " [3] Patch  (xx . yy . zz+1) - Bug fixes and tweaks"
    Write-Host " [4] Custom (manual xx.yy.zz input)"
    Write-Host " [Q] Cancel"
    Write-Host ""
    
    $choice = Read-Host "Enter option [1-4 or Q]"
    switch ($choice.Trim()) {
        "1" { $selectedType = "major" }
        "2" { $selectedType = "minor" }
        "3" { $selectedType = "patch" }
        "4" { $selectedType = "custom" }
        "Q" { Write-Host "Aborted."; exit 0 }
        "q" { Write-Host "Aborted."; exit 0 }
        default {
            Write-Error "Invalid selection: '$choice'"
            exit 1
        }
    }
}

$newMajor = $currentMajor
$newX = $currentX
$newSub = $currentSub

switch ($selectedType) {
    "major" {
        $newMajor = $currentMajor + 1
        $newX = 1
        $newSub = 1
    }
    "minor" {
        $newX = $currentX + 1
        $newSub = 1
    }
    "patch" {
        $newSub = $currentSub + 1
        if ($newSub > 14) {
            $newSub = 1
            $newX = $currentX + 1
        }
    }
    "custom" {
        $inputVer = $CustomVersion
        while ([string]::IsNullOrWhiteSpace($inputVer)) {
            $inputVer = Read-Host "Enter custom version (format: major.minor.patch)"
        }
        
        if ($inputVer -notmatch "^\s*(\d{1,2})\.(\d{1,2})\.(\d{1,2})\s*$") {
            Write-Error "Invalid version format '$inputVer'. Expected format like '4.28.14'."
            exit 1
        }
        $newMajor = [int]$Matches[1]
        $newX = [int]$Matches[2]
        $newSub = [int]$Matches[3]
    }
}

$newVersionName = "{0}.{1:D2}.{2}" -f $newMajor, $newX, $newSub
$newVersionCode = 26031000 + ($newMajor * 10000) + ($newX * 100) + $newSub

Write-Host ""
Write-Host "Target Version Name : $newVersionName" -ForegroundColor Green
Write-Host "Target Version Code : $newVersionCode" -ForegroundColor Green
Write-Host ""

# 4. Update version.properties
$newPropsContent = @"
VERSION_MAJOR=$newMajor
VERSION_X=$newX
VERSION_SUB=$newSub
"@.Trim()

[System.IO.File]::WriteAllText($PropsPath, $newPropsContent + [Environment]::NewLine, (New-Object System.Text.UTF8Encoding($false)))
Write-Host "[OK] Updated version.properties" -ForegroundColor Green

# 5. Update version_history.json
$historyEntry = [PSCustomObject]@{
    versionName = $newVersionName
    versionCode = $newVersionCode
    bumpType    = $selectedType
    previous    = $formattedCurrent
    timestamp   = (Get-Date).ToString("yyyy-MM-ddTHH:mm:sszzz")
}

$historyList = [System.Collections.Generic.List[object]]::new()
foreach ($item in $history) {
    $historyList.Add($item)
}
$historyList.Add($historyEntry)

$historyJsonOutput = $historyList | ConvertTo-Json -Depth 5
[System.IO.File]::WriteAllText($HistoryPath, $historyJsonOutput + [Environment]::NewLine, (New-Object System.Text.UTF8Encoding($false)))
Write-Host "[OK] Updated version_history.json" -ForegroundColor Green

# 6. Optionally update app-updates/choose-browser/update.json
if ($UpdateManifest -and (Test-Path $ManifestPath)) {
    try {
        $manifestJson = Get-Content -Path $ManifestPath -Raw | ConvertFrom-Json
        if ($manifestJson.stable) {
            $manifestJson.stable.versionCode = $newVersionCode
            $manifestJson.stable.versionName = $newVersionName
            $manifestJson.stable.version = $newVersionName
            $manifestJson.stable.releaseDate = (Get-Date).ToString("yyyy-MM-dd")
        }
        $updatedManifest = $manifestJson | ConvertTo-Json -Depth 10
        [System.IO.File]::WriteAllText($ManifestPath, $updatedManifest + [Environment]::NewLine, (New-Object System.Text.UTF8Encoding($false)))
        Write-Host "[OK] Synced app-updates/choose-browser/update.json manifest" -ForegroundColor Green
    } catch {
        Write-Warning "Could not update manifest: $_"
    }
}

# 7. Git commit (if enabled)
if (-not $NoCommit) {
    $defaultMsg = "chore: bump version to $newVersionName"
    if ([string]::IsNullOrWhiteSpace($CommitMessage)) {
        if ([string]::IsNullOrWhiteSpace($Type)) {
            $userMsg = Read-Host "Enter commit message (press Enter for default: '$defaultMsg')"
            $CommitMessage = if ([string]::IsNullOrWhiteSpace($userMsg)) { $defaultMsg } else { $userMsg }
        } else {
            $CommitMessage = $defaultMsg
        }
    }

    if ($CommitMessage.Length -gt 70) {
        $CommitMessage = $CommitMessage.Substring(0, 70)
    }

    Write-Host ""
    Write-Host "Suggested commit message: $CommitMessage" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Green
Write-Host "       VERSION BUMP COMPLETED            " -ForegroundColor Green
Write-Host " New Version: $newVersionName (Code: $newVersionCode)" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green
