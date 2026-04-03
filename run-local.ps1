[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
if (Get-Variable -Name PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue) {
    $PSNativeCommandUseErrorActionPreference = $false
}

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

$javaInfo = cmd /c "java -XshowSettings:properties -version 2>&1"
$javaHomeLine = $javaInfo | Select-String "java.home ="
if (-not $javaHomeLine) {
    throw "Khong tim thay JDK. Hay cai JDK 25 truoc khi chay."
}

$javaHome = ($javaHomeLine.ToString() -split "=")[1].Trim()
$javac = Join-Path $javaHome "bin\javac.exe"
if (-not (Test-Path $javac)) {
    throw "Khong tim thay javac.exe trong $javaHome"
}

$sourceFiles = Get-ChildItem -Recurse -Filter *.java src | Select-Object -ExpandProperty FullName
if (-not $sourceFiles) {
    throw "Khong tim thay source Java trong src"
}

New-Item -ItemType Directory -Path out -Force | Out-Null

Write-Host "Compiling..."
& $javac --module-path javafx-lib --add-modules javafx.controls -d out $sourceFiles
if ($LASTEXITCODE -ne 0) {
    throw "Compile that bai."
}

if (Test-Path resources) {
    Write-Host "Copying resources..."
    Copy-Item resources\* out -Recurse -Force
}

Write-Host "Launching app..."
& java --module-path javafx-lib --add-modules javafx.controls --enable-native-access=javafx.graphics -cp out com.auctionstudio.Main
