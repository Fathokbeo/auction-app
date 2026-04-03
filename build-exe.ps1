[CmdletBinding()]
param(
    [string]$AppName = "Auction Studio",
    [string]$MainClass = "com.auctionstudio.Main",
    [string]$JarName = "auction-studio.jar",
    [string]$Version = "1.0.0",
    [string]$Vendor = "Your Company",
    [string]$JavaFxDir = "javafx-lib",
    [string]$ResourceDir = "resources",
    [string]$OutputDir = "release",
    [string]$DistDir = "dist",
    [string]$ClassesDir = "out",
    [string]$SourceDir = "src",
    [string]$IconPath = "resources\\icons\\app-icon.ico",
    [switch]$KeepBuildFolders
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
if (Get-Variable -Name PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue) {
    $PSNativeCommandUseErrorActionPreference = $false
}

function Resolve-ProjectPath {
    param([string]$PathText)
    if ([System.IO.Path]::IsPathRooted($PathText)) {
        return $PathText
    }
    return Join-Path $PSScriptRoot $PathText
}

function Get-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\\javac.exe"))) {
        return $env:JAVA_HOME
    }

    $javaInfo = cmd /c "java -XshowSettings:properties -version 2>&1"
    $javaHomeLine = $javaInfo | Select-String "java.home ="
    if (-not $javaHomeLine) {
        throw "Khong tim thay JAVA_HOME. Hay cai JDK day du va thu lai."
    }

    $javaHome = ($javaHomeLine.ToString() -split "=")[1].Trim()
    if (-not (Test-Path (Join-Path $javaHome "bin\\javac.exe"))) {
        throw "Duong dan JDK khong hop le: $javaHome"
    }

    return $javaHome
}

function Get-WixBin {
    $localCandidates = @(
        (Join-Path $PSScriptRoot "tools\wix\bin"),
        (Join-Path $PSScriptRoot "tools\wix")
    )
    foreach ($candidate in $localCandidates) {
        if ((Test-Path (Join-Path $candidate "candle.exe")) -and (Test-Path (Join-Path $candidate "light.exe"))) {
            return $candidate
        }
    }

    $candle = Get-Command candle.exe -ErrorAction SilentlyContinue
    $light = Get-Command light.exe -ErrorAction SilentlyContinue
    if ($candle -and $light) {
        return Split-Path $candle.Source
    }

    $candidates = @(
        "C:\Program Files (x86)\WiX Toolset v3.11\bin",
        "C:\Program Files (x86)\WiX Toolset v3.14\bin",
        "C:\Program Files\WiX Toolset v3.11\bin",
        "C:\Program Files\WiX Toolset v3.14\bin"
    )

    foreach ($candidate in $candidates) {
        if ((Test-Path (Join-Path $candidate "candle.exe")) -and (Test-Path (Join-Path $candidate "light.exe"))) {
            return $candidate
        }
    }

    return $null
}

function Ensure-CleanDirectory {
    param([string]$PathText)
    if (Test-Path $PathText) {
        Remove-Item -Recurse -Force $PathText
    }
    New-Item -ItemType Directory -Path $PathText | Out-Null
}

$projectRoot = $PSScriptRoot
$classesPath = Resolve-ProjectPath $ClassesDir
$distPath = Resolve-ProjectPath $DistDir
$outputPath = Resolve-ProjectPath $OutputDir
$sourcePath = Resolve-ProjectPath $SourceDir
$resourcePath = Resolve-ProjectPath $ResourceDir
$javaFxPath = Resolve-ProjectPath $JavaFxDir
$jarPath = Join-Path $distPath $JarName

if (-not (Test-Path $sourcePath)) {
    throw "Khong tim thay thu muc source: $sourcePath"
}

if (-not (Test-Path $javaFxPath)) {
    throw "Khong tim thay JavaFX libs tai $javaFxPath"
}

$javaHome = Get-JavaHome
$javac = Join-Path $javaHome "bin\javac.exe"
$jar = Join-Path $javaHome "bin\jar.exe"
$jpackage = Join-Path $javaHome "bin\jpackage.exe"

if (-not (Test-Path $jpackage)) {
    throw "Khong tim thay jpackage.exe trong $javaHome. Hay cai JDK day du."
}

$wixBin = Get-WixBin
if (-not $wixBin) {
    throw "Khong tim thay WiX Toolset (candle.exe/light.exe). Cai WiX 3.x roi chay lai script nay."
}

$env:Path = "$wixBin;$env:Path"

Ensure-CleanDirectory $classesPath
Ensure-CleanDirectory $distPath
Ensure-CleanDirectory $outputPath

$javaFiles = Get-ChildItem -Path $sourcePath -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
if (-not $javaFiles) {
    throw "Khong co file .java nao trong $sourcePath"
}

Write-Host "Compiling sources..."
& $javac --module-path $javaFxPath --add-modules javafx.controls -d $classesPath $javaFiles
if ($LASTEXITCODE -ne 0) {
    throw "Compile that bai."
}

if (Test-Path $resourcePath) {
    Write-Host "Copying resources..."
    Copy-Item -Path (Join-Path $resourcePath "*") -Destination $classesPath -Recurse -Force
}

Write-Host "Creating JAR..."
& $jar --create --file $jarPath --main-class $MainClass -C $classesPath .
if ($LASTEXITCODE -ne 0) {
    throw "Tao JAR that bai."
}

$jpackageArgs = @(
    "--type", "exe",
    "--name", $AppName,
    "--app-version", $Version,
    "--dest", $outputPath,
    "--input", $distPath,
    "--main-jar", $JarName,
    "--main-class", $MainClass,
    "--module-path", $javaFxPath,
    "--add-modules", "javafx.controls",
    "--java-options", "--enable-native-access=javafx.graphics",
    "--win-shortcut",
    "--win-menu",
    "--vendor", $Vendor
)

if ($IconPath) {
    $resolvedIcon = Resolve-ProjectPath $IconPath
    if (-not (Test-Path $resolvedIcon)) {
        throw "Khong tim thay icon tai $resolvedIcon"
    }
    $jpackageArgs += @("--icon", $resolvedIcon)
}

Write-Host "Packaging EXE..."
& $jpackage @jpackageArgs
if ($LASTEXITCODE -ne 0) {
    throw "Dong goi EXE that bai."
}

$exeFile = Get-ChildItem -Path $outputPath -Filter *.exe | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $exeFile) {
    throw "Khong tim thay file EXE trong $outputPath"
}

if (-not $KeepBuildFolders) {
    if (Test-Path $distPath) {
        Remove-Item -Recurse -Force $distPath
    }
}

Write-Host ""
Write-Host "Hoan tat."
Write-Host "EXE: $($exeFile.FullName)"
Write-Host "Ban co the upload file nay cho khach hang tai ve."
