<#
.SYNOPSIS
    Build ToFlowAI installer with embedded GraalVM runtime.

.DESCRIPTION
    This script downloads GraalVM JDK, creates a custom runtime image,
    and builds platform-specific installers (MSI for Windows).
    
    The installer includes everything needed to run ToFlowAI without
    requiring any Java installation on the target machine.

.PARAMETER Version
    Application version (default: from environment or 0.1.0)

.PARAMETER GraalVMVersion
    GraalVM version to download (default: 25)

.PARAMETER SkipDownload
    Skip GraalVM download if already present

.PARAMETER Clean
    Clean build directories before building

.EXAMPLE
    .\build-installer.ps1
    
.EXAMPLE
    .\build-installer.ps1 -Version "1.0.0" -Clean

.NOTES
    Requires: Windows 10+, WiX Toolset (for MSI creation)
    Downloads: ~200MB GraalVM JDK
#>

param(
    [string]$Version = $env:APP_VERSION,
    [string]$GraalVMVersion = "25",
    [switch]$SkipDownload,
    [switch]$Clean
)

$ErrorActionPreference = "Stop"

# Configuration
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$ToolsDir = $PSScriptRoot
$BuildDir = Join-Path $ProjectRoot "build"
$RuntimeDir = Join-Path $BuildDir "runtime"
$DistDir = Join-Path $ProjectRoot "app\build\distributions"

# GraalVM download configuration
$GraalVMBaseUrl = "https://download.oracle.com/graalvm"
$GraalVMArchive = "graalvm-jdk-${GraalVMVersion}_windows-x64_bin.zip"
$GraalVMUrl = "${GraalVMBaseUrl}/${GraalVMVersion}/latest/${GraalVMArchive}"
$GraalVMDir = Join-Path $BuildDir "graalvm-jdk-${GraalVMVersion}"

# Default version
if (-not $Version) {
    $Version = "0.1.0"
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " ToFlowAI Installer Builder" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Version: $Version"
Write-Host "GraalVM: $GraalVMVersion"
Write-Host "Project: $ProjectRoot"
Write-Host ""

# Clean if requested
if ($Clean) {
    Write-Host "[1/6] Cleaning build directories..." -ForegroundColor Yellow
    if (Test-Path $BuildDir) {
        Remove-Item -Recurse -Force $BuildDir
    }
    if (Test-Path $DistDir) {
        Remove-Item -Recurse -Force $DistDir
    }
}
else {
    Write-Host "[1/6] Skipping clean (use -Clean to force)" -ForegroundColor Gray
}

# Create directories
New-Item -ItemType Directory -Force -Path $BuildDir | Out-Null
New-Item -ItemType Directory -Force -Path $RuntimeDir | Out-Null

# Download GraalVM
if (-not $SkipDownload -or -not (Test-Path $GraalVMDir)) {
    Write-Host "[2/6] Downloading GraalVM $GraalVMVersion..." -ForegroundColor Yellow
    
    $ArchivePath = Join-Path $BuildDir $GraalVMArchive
    
    if (-not (Test-Path $ArchivePath)) {
        Write-Host "  URL: $GraalVMUrl"
        try {
            $ProgressPreference = 'SilentlyContinue'
            Invoke-WebRequest -Uri $GraalVMUrl -OutFile $ArchivePath -UseBasicParsing
            $ProgressPreference = 'Continue'
        }
        catch {
            Write-Host "  Failed to download from Oracle. Trying alternative source..." -ForegroundColor Yellow
            # Alternative: Microsoft OpenJDK
            $MsJdkUrl = "https://aka.ms/download-jdk/microsoft-jdk-25-windows-x64.zip"
            Invoke-WebRequest -Uri $MsJdkUrl -OutFile $ArchivePath -UseBasicParsing
        }
        Write-Host "  Downloaded: $ArchivePath" -ForegroundColor Green
    }
    else {
        Write-Host "  Archive already exists: $ArchivePath" -ForegroundColor Gray
    }
    
    # Extract
    Write-Host "  Extracting..."
    if (Test-Path $GraalVMDir) {
        Remove-Item -Recurse -Force $GraalVMDir
    }
    Expand-Archive -Path $ArchivePath -DestinationPath $BuildDir -Force
    
    # Find extracted folder (name may vary)
    $ExtractedDir = Get-ChildItem -Path $BuildDir -Directory | Where-Object { $_.Name -like "graalvm*" -or $_.Name -like "jdk*" } | Select-Object -First 1
    if ($ExtractedDir -and $ExtractedDir.FullName -ne $GraalVMDir) {
        Rename-Item -Path $ExtractedDir.FullName -NewName (Split-Path $GraalVMDir -Leaf)
    }
    
    Write-Host "  GraalVM ready: $GraalVMDir" -ForegroundColor Green
}
else {
    Write-Host "[2/6] Using existing GraalVM at $GraalVMDir" -ForegroundColor Gray
}

# Build the application
Write-Host "[3/6] Building application..." -ForegroundColor Yellow
Push-Location $ProjectRoot
try {
    $env:JAVA_HOME = $GraalVMDir
    & .\gradlew.bat clean bootJar -x test --no-daemon
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed"
    }
    Write-Host "  Application built successfully" -ForegroundColor Green
}
finally {
    Pop-Location
}

# Create custom runtime image with jlink
Write-Host "[4/6] Creating custom runtime image..." -ForegroundColor Yellow
$JlinkPath = Join-Path $GraalVMDir "bin\jlink.exe"
$CustomRuntimeDir = Join-Path $RuntimeDir "toflowai-runtime"

if (Test-Path $CustomRuntimeDir) {
    Remove-Item -Recurse -Force $CustomRuntimeDir
}

$Modules = @(
    "java.base",
    "java.desktop",
    "java.logging",
    "java.management",
    "java.naming",
    "java.net.http",
    "java.prefs",
    "java.scripting",
    "java.security.jgss",
    "java.sql",
    "java.xml",
    "jdk.crypto.ec",
    "jdk.unsupported",
    "jdk.zipfs"
) -join ","

& $JlinkPath `
    --module-path (Join-Path $GraalVMDir "jmods") `
    --add-modules $Modules `
    --output $CustomRuntimeDir `
    --strip-debug `
    --no-man-pages `
    --no-header-files `
    --compress=zip-6

if ($LASTEXITCODE -ne 0) {
    throw "jlink failed to create runtime image"
}

Write-Host "  Custom runtime created: $CustomRuntimeDir" -ForegroundColor Green
$RuntimeSize = (Get-ChildItem -Path $CustomRuntimeDir -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB
Write-Host "  Runtime size: $([math]::Round($RuntimeSize, 1)) MB" -ForegroundColor Gray

# Build installer with jpackage
Write-Host "[5/6] Creating installer with jpackage..." -ForegroundColor Yellow
Push-Location $ProjectRoot
try {
    $env:JAVA_HOME = $GraalVMDir
    & .\gradlew.bat :app:jpackage "-Pjpackage.runtime=$CustomRuntimeDir" --no-daemon
    if ($LASTEXITCODE -ne 0) {
        throw "jpackage failed"
    }
    Write-Host "  Installer created successfully" -ForegroundColor Green
}
finally {
    Pop-Location
}

# Summary
Write-Host "[6/6] Build complete!" -ForegroundColor Yellow
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Build Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$InstallerPath = Get-ChildItem -Path $DistDir -Filter "*.msi" | Select-Object -First 1
if ($InstallerPath) {
    $InstallerSize = [math]::Round($InstallerPath.Length / 1MB, 1)
    Write-Host ""
    Write-Host "Installer: $($InstallerPath.FullName)" -ForegroundColor Green
    Write-Host "Size:      $InstallerSize MB" -ForegroundColor Green
    Write-Host ""
    Write-Host "The installer includes:" -ForegroundColor White
    Write-Host "  - ToFlowAI application" -ForegroundColor Gray
    Write-Host "  - Embedded GraalVM runtime (no Java required)" -ForegroundColor Gray
    Write-Host "  - All dependencies bundled" -ForegroundColor Gray
    Write-Host ""
    Write-Host "To install: Double-click the MSI file" -ForegroundColor Yellow
}
else {
    Write-Host "Warning: No MSI file found in $DistDir" -ForegroundColor Yellow
    Write-Host "Check the build output for errors." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Done!" -ForegroundColor Green
