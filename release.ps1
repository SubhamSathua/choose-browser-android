# Choose Browser Release Manager
# Automates validation, release note verification, tagging, and publishing
param (
    [ValidateSet("stable", "alpha", "")]
    [string]$Channel = "",
    [string]$Version = "",
    [switch]$Build,
    [switch]$Push,
    [switch]$NonInteractive
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

$PropsPath = Join-Path $ScriptDir "version.properties"
if (-not (Test-Path $PropsPath)) {
    Write-Error "version.properties not found at $PropsPath"
    exit 1
}

# 1. Read current version from version.properties
$propsContent = Get-Content -Path $PropsPath -Raw
$vMajor = 4
$vX = 30
$vSub = 1

if ($propsContent -match "VERSION_MAJOR=(\d+)") { $vMajor = [int]$Matches[1] }
if ($propsContent -match "VERSION_X=(\d+)") { $vX = [int]$Matches[1] }
if ($propsContent -match "VERSION_SUB=(\d+)") { $vSub = [int]$Matches[1] }

$defaultVersion = "{0}.{1:D2}.{2}" -f $vMajor, $vX, $vSub

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "     CHOOSE BROWSER RELEASE MANAGER      " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host " Configured Version: $defaultVersion" -ForegroundColor Yellow
Write-Host "=========================================" -ForegroundColor Cyan

# 2. Select Release Channel (Alpha vs Stable)
$selectedChannel = $Channel.ToLower()
if ([string]::IsNullOrWhiteSpace($selectedChannel) -and -not $NonInteractive) {
    Write-Host ""
    Write-Host "Select release channel:" -ForegroundColor Green
    Write-Host " [1] Stable (Production Release - e.g. v4.30.1)"
    Write-Host " [2] Alpha  (Pre-release Testing - e.g. v4.30.1-alpha.1)"
    Write-Host " [Q] Cancel"
    Write-Host ""
    
    $choice = Read-Host "Enter option [1-2 or Q]"
    switch ($choice.Trim()) {
        "1" { $selectedChannel = "stable" }
        "2" { $selectedChannel = "alpha" }
        "Q" { Write-Host "Aborted."; exit 0 }
        "q" { Write-Host "Aborted."; exit 0 }
        default {
            Write-Error "Invalid selection: '$choice'"
            exit 1
        }
    }
} elseif ([string]::IsNullOrWhiteSpace($selectedChannel)) {
    $selectedChannel = "stable"
}

# 3. Determine and Validate Version String
$targetVersion = if ([string]::IsNullOrWhiteSpace($Version)) { $defaultVersion } else { $Version.Trim() }

if ($targetVersion -notmatch "^\d{1,2}\.\d{1,2}\.\d{1,2}(-alpha(\.\d+)?)?$") {
    Write-Error "Invalid version format '$targetVersion'. Expected format like '4.30.1' or '4.30.1-alpha.1'."
    exit 1
}

# 4. Construct Git Tag Name
$tagName = if ($selectedChannel -eq "alpha") {
    if ($targetVersion -match "-alpha") { "v$targetVersion" } else { "v$targetVersion-alpha.1" }
} else {
    "v$targetVersion"
}

$tagClean = $tagName.TrimStart("v")

Write-Host ""
Write-Host "Target Tag     : $tagName" -ForegroundColor Green
Write-Host "Release Channel: $($selectedChannel.ToUpper())" -ForegroundColor Green
Write-Host ""

# 5. Check Release Notes Existence
$notesCandidates = @(
    (Join-Path $ScriptDir ".github\releases\$tagName.md"),
    (Join-Path $ScriptDir ".github\releases\$tagClean.md"),
    (Join-Path $ScriptDir ".github\release\$tagName.md"),
    (Join-Path $ScriptDir ".github\release\$tagClean.md")
)

$foundNote = $null
foreach ($path in $notesCandidates) {
    if (Test-Path $path) {
        $foundNote = $path
        break
    }
}

if (-not $foundNote) {
    Write-Warning "No release note found for $tagName in .github/releases/ or .github/release/."
    Write-Host "Checked paths:" -ForegroundColor Yellow
    foreach ($p in $notesCandidates) { Write-Host "  - $p" -ForegroundColor DarkGray }
    if (-not $NonInteractive) {
        $proceed = Read-Host "Proceed without custom release notes? [y/N]"
        if ($proceed.Trim().ToLower() -ne "y") {
            Write-Host "Aborted. Please create .github/releases/$tagClean.md first."
            exit 0
        }
    }
} else {
    Write-Host "[OK] Found release note at: $foundNote" -ForegroundColor Green
}

# 6. Optional Local Build & Verification
if ($Build) {
    Write-Host ""
    Write-Host "Building release artifacts locally..." -ForegroundColor Cyan
    $gradleTask = if ($selectedChannel -eq "alpha") { "assembleAlpha" } else { "assembleRelease" }
    & .\gradlew $gradleTask
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Build failed with exit code $LASTEXITCODE"
        exit $LASTEXITCODE
    }
    Write-Host "[OK] Local build successful." -ForegroundColor Green
}

# 7. Check if Tag already exists
$existingTag = git tag -l $tagName
if ($existingTag) {
    Write-Error "Git tag '$tagName' already exists locally! Cannot overwrite an existing release."
    exit 1
}

# 8. Confirmation and Tag Creation
if (-not $NonInteractive) {
    Write-Host ""
    Write-Host "Ready to create and push release tag '$tagName' to remote repository." -ForegroundColor Yellow
    $confirm = Read-Host "Create tag and trigger CI/CD release? [y/N]"
    if ($confirm.Trim().ToLower() -ne "y") {
        Write-Host "Aborted by user."
        exit 0
    }
}

# Create Git tag
$tagMessage = if ($foundNote) {
    "Release $tagName"
} else {
    "Choose Browser $tagName"
}

git tag -a $tagName -m "$tagMessage"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to create git tag '$tagName'"
    exit $LASTEXITCODE
}
Write-Host "[OK] Created git tag '$tagName'" -ForegroundColor Green

# 9. Push Tag to Remote (triggers GitHub Actions release workflow)
if ($Push -or (-not $NonInteractive)) {
    Write-Host "Pushing tag '$tagName' to origin..." -ForegroundColor Cyan
    git push origin $tagName
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to push git tag '$tagName' to origin"
        exit $LASTEXITCODE
    }
    Write-Host "[OK] Successfully pushed tag '$tagName' to GitHub!" -ForegroundColor Green
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Green
Write-Host "         RELEASE COMPLETED!              " -ForegroundColor Green
Write-Host " GitHub Actions is now building & publishing:" -ForegroundColor Cyan
Write-Host " https://github.com/SubhamSathua/choose-browser-android/actions" -ForegroundColor Yellow
Write-Host "=========================================" -ForegroundColor Green
