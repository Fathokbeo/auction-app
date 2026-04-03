[CmdletBinding()]
param(
    [string]$VersionTag = "wix3141rtm",
    [string]$DownloadFile = "wix314-binaries.zip",
    [string]$DestinationDir = "tools\\wix"
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

$destinationPath = Resolve-ProjectPath $DestinationDir
$tempDir = Resolve-ProjectPath "tools\\downloads"
$zipPath = Join-Path $tempDir $DownloadFile
$url = "https://github.com/wixtoolset/wix3/releases/download/$VersionTag/$DownloadFile"

New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
if (Test-Path $destinationPath) {
    Remove-Item -Recurse -Force $destinationPath
}
New-Item -ItemType Directory -Path $destinationPath -Force | Out-Null

Write-Host "Downloading WiX binaries from $url"
& curl.exe -L $url -o $zipPath
if ($LASTEXITCODE -ne 0) {
    throw "Tai WiX that bai."
}

Write-Host "Extracting WiX to $destinationPath"
Expand-Archive -Path $zipPath -DestinationPath $destinationPath -Force

if (-not (Test-Path (Join-Path $destinationPath "candle.exe"))) {
    throw "Khong tim thay candle.exe sau khi giai nen WiX."
}

Write-Host ""
Write-Host "Hoan tat."
Write-Host "WiX local: $destinationPath"
Write-Host "Bay gio ban co the chay: powershell -ExecutionPolicy Bypass -File .\\build-exe.ps1"
