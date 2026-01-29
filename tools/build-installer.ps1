<#
.SYNOPSIS
    Build ToFlowAI installer with embedded GraalVM runtime.

.DESCRIPTION
    This script downloads GraalVM JDK and WiX Toolset if needed, creates a custom 
    runtime image, and builds platform-specific installers (MSI for Windows).
    
    The installer includes everything needed to run ToFlowAI without
    requiring any Java installation on the target machine.

.PARAMETER Version
    Application version (default: from environment or 0.1.0)

.PARAMETER GraalVMVersion
    GraalVM version to download (default: 25)

.PARAMETER Portable
    Build portable ZIP instead of MSI installer (no WiX required)

.PARAMETER SkipDownload
    Skip GraalVM download if already present

.PARAMETER SkipClean
    Skip the clean step (useful when files are locked)

.EXAMPLE
    .\build-installer.ps1
    # Builds MSI installer with embedded GraalVM

.EXAMPLE
    .\build-installer.ps1 -Portable
    # Builds portable ZIP (no WiX required)

.EXAMPLE
    .\build-installer.ps1 -Version "1.0.0" -SkipClean
    # Builds MSI without cleaning first

.NOTES
    Requires: Windows 10+
    Downloads: ~200MB GraalVM JDK, ~40MB WiX Toolset (auto-downloaded)
#>

param(
    [string]$Version = $env:APP_VERSION,
    [string]$GraalVMVersion = "25",
    [switch]$Portable,
    [switch]$SkipDownload,
    [switch]$SkipClean
)

$ErrorActionPreference = "Stop"

# Configuration
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$ToolsDir = $PSScriptRoot
$BuildDir = Join-Path $ProjectRoot "build"
$RuntimeDir = Join-Path $BuildDir "runtime"
$DistDir = Join-Path $ProjectRoot "app\build\distributions"

# WiX configuration (local download - no admin required)
$WixPath = Join-Path $ToolsDir "wix314"
$WixUrl = "https://github.com/wixtoolset/wix3/releases/download/wix3141rtm/wix314-binaries.zip"

# GraalVM download configuration
$GraalVMBaseUrl = "https://download.oracle.com/graalvm"
$GraalVMArchive = "graalvm-jdk-${GraalVMVersion}_windows-x64_bin.zip"
$GraalVMUrl = "${GraalVMBaseUrl}/${GraalVMVersion}/latest/${GraalVMArchive}"
$GraalVMDir = Join-Path $BuildDir "graalvm-jdk-${GraalVMVersion}"

# Default version
if (-not $Version) {
    $Version = "0.1.0"
}

Write-Host ""
Write-Host "╔═══════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║           ToFlowAI Windows Installer Builder              ║" -ForegroundColor Cyan
Write-Host "╚═══════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

$buildType = if ($Portable) { "Portable ZIP" } else { "MSI Installer" }
Write-Host "Build Type: $buildType" -ForegroundColor White
Write-Host "Version:    $Version" -ForegroundColor White
Write-Host "GraalVM:    $GraalVMVersion" -ForegroundColor White
Write-Host "Project:    $ProjectRoot" -ForegroundColor Gray
Write-Host ""

# =============================================================================
# Step 1: Download/Verify WiX Toolset (only for MSI builds)
# =============================================================================
if (-not $Portable) {
    if (-not (Test-Path "$WixPath\light.exe")) {
        Write-Host "[1/6] Downloading WiX Toolset 3.14..." -ForegroundColor Yellow
        
        if (-not (Test-Path $WixPath)) {
            New-Item -ItemType Directory -Path $WixPath -Force | Out-Null
        }
        
        $wixZip = Join-Path $env:TEMP "wix314.zip"
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        
        try {
            Write-Host "      Downloading from GitHub..." -ForegroundColor Gray
            $ProgressPreference = 'SilentlyContinue'
            Invoke-WebRequest -Uri $WixUrl -OutFile $wixZip -UseBasicParsing
            $ProgressPreference = 'Continue'
            
            Write-Host "      Extracting WiX..." -ForegroundColor Gray
            Expand-Archive -Path $wixZip -DestinationPath $WixPath -Force
            Remove-Item $wixZip -Force
            
            Write-Host "      ✓ WiX installed to: $WixPath" -ForegroundColor Green
        }
        catch {
            Write-Host "      ✗ Failed to download WiX: $_" -ForegroundColor Red
            Write-Host ""
            Write-Host "      Please download WiX manually:" -ForegroundColor Yellow
            Write-Host "      1. Go to: https://github.com/wixtoolset/wix3/releases" -ForegroundColor Gray
            Write-Host "      2. Download: wix314-binaries.zip" -ForegroundColor Gray
            Write-Host "      3. Extract to: $WixPath" -ForegroundColor Gray
            exit 1
        }
    }
    else {
        Write-Host "[1/6] WiX Toolset already installed ✓" -ForegroundColor Green
    }
    
    # Add WiX to PATH for this session
    $env:PATH = "$WixPath;$env:PATH"
    Write-Host "      WiX added to PATH for this session" -ForegroundColor Gray
}
else {
    Write-Host "[1/6] WiX not required for portable build ✓" -ForegroundColor Green
}

# =============================================================================
# Step 2: Download/Verify GraalVM JDK
# =============================================================================
if (-not $SkipDownload -or -not (Test-Path $GraalVMDir)) {
    Write-Host "[2/6] Downloading GraalVM JDK $GraalVMVersion..." -ForegroundColor Yellow
    
    # Create directories
    New-Item -ItemType Directory -Force -Path $BuildDir | Out-Null
    New-Item -ItemType Directory -Force -Path $RuntimeDir | Out-Null
    
    $ArchivePath = Join-Path $BuildDir $GraalVMArchive
    
    if (-not (Test-Path $ArchivePath)) {
        Write-Host "      URL: $GraalVMUrl" -ForegroundColor Gray
        try {
            $ProgressPreference = 'SilentlyContinue'
            Invoke-WebRequest -Uri $GraalVMUrl -OutFile $ArchivePath -UseBasicParsing
            $ProgressPreference = 'Continue'
            Write-Host "      ✓ Downloaded: $ArchivePath" -ForegroundColor Green
        }
        catch {
            Write-Host "      Failed to download from Oracle. Trying Microsoft OpenJDK..." -ForegroundColor Yellow
            $MsJdkUrl = "https://aka.ms/download-jdk/microsoft-jdk-${GraalVMVersion}-windows-x64.zip"
            try {
                Invoke-WebRequest -Uri $MsJdkUrl -OutFile $ArchivePath -UseBasicParsing
                Write-Host "      ✓ Downloaded from Microsoft" -ForegroundColor Green
            }
            catch {
                Write-Host "      ✗ Failed to download JDK: $_" -ForegroundColor Red
                exit 1
            }
        }
    }
    else {
        Write-Host "      Archive already exists: $ArchivePath" -ForegroundColor Gray
    }
    
    # Extract
    Write-Host "      Extracting JDK..." -ForegroundColor Gray
    if (Test-Path $GraalVMDir) {
        Remove-Item -Recurse -Force $GraalVMDir
    }
    Expand-Archive -Path $ArchivePath -DestinationPath $BuildDir -Force
    
    # Find extracted folder (name may vary)
    $ExtractedDir = Get-ChildItem -Path $BuildDir -Directory | Where-Object { $_.Name -like "graalvm*" -or $_.Name -like "jdk*" } | Select-Object -First 1
    if ($ExtractedDir -and $ExtractedDir.FullName -ne $GraalVMDir) {
        Rename-Item -Path $ExtractedDir.FullName -NewName (Split-Path $GraalVMDir -Leaf)
    }
    
    Write-Host "      ✓ GraalVM ready: $GraalVMDir" -ForegroundColor Green
}
else {
    Write-Host "[2/6] Using existing GraalVM at $GraalVMDir ✓" -ForegroundColor Green
}

# Set JAVA_HOME for subsequent steps
$env:JAVA_HOME = $GraalVMDir

# =============================================================================
# Step 3: Build the application
# =============================================================================
Write-Host "[3/6] Building application..." -ForegroundColor Yellow
Push-Location $ProjectRoot
try {
    $gradleArgs = @()
    if (-not $SkipClean) {
        $gradleArgs += "clean"
    }
    $gradleArgs += "bootJar"
    $gradleArgs += "-x"
    $gradleArgs += "test"
    $gradleArgs += "--no-daemon"
    
    & .\gradlew.bat @gradleArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed"
    }
    Write-Host "      ✓ Application built successfully" -ForegroundColor Green
}
finally {
    Pop-Location
}

# =============================================================================
# Step 4: Create custom runtime image with jlink
# =============================================================================
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

$RuntimeSize = (Get-ChildItem -Path $CustomRuntimeDir -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB
Write-Host "      ✓ Custom runtime created ($([math]::Round($RuntimeSize, 1)) MB)" -ForegroundColor Green

# =============================================================================
# Step 5: Create installer with jpackage
# =============================================================================
Write-Host "[5/6] Creating $buildType with jpackage..." -ForegroundColor Yellow

$JpackagePath = Join-Path $GraalVMDir "bin\jpackage.exe"
$MainJar = Get-ChildItem -Path (Join-Path $ProjectRoot "app\build\libs") -Filter "app-*.jar" | Select-Object -First 1

if (-not $MainJar) {
    throw "Could not find application JAR in app\build\libs"
}

$AppImageDir = Join-Path $DistDir "ToFlowAI"

# Remove existing output if present
if (Test-Path $AppImageDir) {
    Remove-Item -Recurse -Force $AppImageDir
}

if ($Portable) {
    # Run jpackage directly for portable builds (app-image)
    Write-Host "      Creating app-image with jpackage..." -ForegroundColor Gray
    
    $jpackageArgs = @(
        "--name", "ToFlowAI",
        "--type", "app-image",
        "--runtime-image", $CustomRuntimeDir,
        "--input", (Join-Path $ProjectRoot "app\build\libs"),
        "--main-jar", $MainJar.Name,
        "--dest", $DistDir,
        "--vendor", "ToFlowAI",
        "--app-version", $Version,
        "--description", "AI-Native Workflow Automation Platform",
        "--java-options", "-Djava.net.preferIPv4Stack=true",
        "--java-options", "-Dfile.encoding=UTF-8",
        "--java-options", "--enable-preview",
        "--java-options", "--enable-native-access=ALL-UNNAMED",
        "--java-options", "-Xms256m",
        "--java-options", "-Xmx1024m",
        "--verbose"
    )
    
    & $JpackagePath @jpackageArgs
    if ($LASTEXITCODE -ne 0) {
        throw "jpackage failed"
    }
    Write-Host "      ✓ App-image created successfully" -ForegroundColor Green
}
else {
    # Run jpackage directly for MSI builds (more reliable than Gradle plugin)
    Write-Host "      Creating MSI installer with jpackage..." -ForegroundColor Gray
    
    # Remove existing MSI if present
    $existingMsi = Get-ChildItem -Path $DistDir -Filter "*.msi" -ErrorAction SilentlyContinue
    if ($existingMsi) {
        Remove-Item $existingMsi.FullName -Force
    }
    
    $jpackageArgs = @(
        "--name", "ToFlowAI",
        "--type", "msi",
        "--runtime-image", $CustomRuntimeDir,
        "--input", (Join-Path $ProjectRoot "app\build\libs"),
        "--main-jar", $MainJar.Name,
        "--dest", $DistDir,
        "--vendor", "ToFlowAI",
        "--app-version", $Version,
        "--description", "AI-Native Workflow Automation Platform",
        "--java-options", "-Djava.net.preferIPv4Stack=true",
        "--java-options", "-Dfile.encoding=UTF-8",
        "--java-options", "--enable-preview",
        "--java-options", "--enable-native-access=ALL-UNNAMED",
        "--java-options", "-Xms256m",
        "--java-options", "-Xmx1024m",
        "--win-menu",
        "--win-per-user-install",
        "--win-dir-chooser",
        "--win-shortcut",
        "--win-shortcut-prompt",
        "--win-menu-group", "ToFlowAI",
        "--win-upgrade-uuid", "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "--verbose"
    )
    
    & $JpackagePath @jpackageArgs
    if ($LASTEXITCODE -ne 0) {
        throw "jpackage failed"
    }
    Write-Host "      ✓ MSI installer created successfully" -ForegroundColor Green
}

# =============================================================================
# Step 6: Summary
# =============================================================================
Write-Host "[6/6] Build complete!" -ForegroundColor Yellow
Write-Host ""

# Check for output files
$msiFile = Get-ChildItem -Path $DistDir -Filter "*.msi" -ErrorAction SilentlyContinue | Select-Object -First 1
$zipFile = Get-ChildItem -Path $DistDir -Filter "*-portable.zip" -ErrorAction SilentlyContinue | Select-Object -First 1
$appImage = Join-Path $DistDir "ToFlowAI"

if ($msiFile -or $zipFile -or (Test-Path $appImage)) {
    Write-Host "╔═══════════════════════════════════════════════════════════╗" -ForegroundColor Green
    Write-Host "║                    BUILD SUCCESSFUL                       ║" -ForegroundColor Green
    Write-Host "╚═══════════════════════════════════════════════════════════╝" -ForegroundColor Green
    Write-Host ""
    
    if ($msiFile) {
        $msiSize = [math]::Round($msiFile.Length / 1MB, 1)
        Write-Host "MSI Installer:" -ForegroundColor Cyan
        Write-Host "  Path: $($msiFile.FullName)" -ForegroundColor White
        Write-Host "  Size: $msiSize MB" -ForegroundColor Gray
        Write-Host ""
    }
    
    if ($zipFile) {
        $zipSize = [math]::Round($zipFile.Length / 1MB, 1)
        Write-Host "Portable ZIP:" -ForegroundColor Cyan
        Write-Host "  Path: $($zipFile.FullName)" -ForegroundColor White
        Write-Host "  Size: $zipSize MB" -ForegroundColor Gray
        Write-Host ""
    }
    
    if (Test-Path $appImage) {
        $appSize = (Get-ChildItem -Path $appImage -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB
        Write-Host "App Image (Portable Directory):" -ForegroundColor Cyan
        Write-Host "  Path: $appImage" -ForegroundColor White
        Write-Host "  Size: $([math]::Round($appSize, 1)) MB" -ForegroundColor Gray
        Write-Host "  Run:  $appImage\ToFlowAI.exe" -ForegroundColor Yellow
        Write-Host ""
    }
    
    Write-Host "The installer includes:" -ForegroundColor White
    Write-Host "  ✓ ToFlowAI application" -ForegroundColor Gray
    Write-Host "  ✓ Embedded GraalVM runtime (no Java required)" -ForegroundColor Gray
    Write-Host "  ✓ All dependencies bundled" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Output directory: $DistDir" -ForegroundColor Gray
}
else {
    Write-Host "╔═══════════════════════════════════════════════════════════╗" -ForegroundColor Red
    Write-Host "║                     BUILD FAILED                          ║" -ForegroundColor Red
    Write-Host "╚═══════════════════════════════════════════════════════════╝" -ForegroundColor Red
    Write-Host ""
    Write-Host "Tips:" -ForegroundColor Yellow
    Write-Host "  - If files are locked, try: .\build-installer.ps1 -SkipClean" -ForegroundColor Gray
    Write-Host "  - Check if ToFlowAI is running and close it" -ForegroundColor Gray
    Write-Host "  - Try portable build: .\build-installer.ps1 -Portable" -ForegroundColor Gray
    Write-Host "  - Check Gradle output above for errors" -ForegroundColor Gray
    exit 1
}
