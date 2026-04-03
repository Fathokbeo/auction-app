[CmdletBinding()]
param(
    [string]$PngPath = "resources\\icons\\app-icon.png",
    [string]$IcoPath = "resources\\icons\\app-icon.ico",
    [int]$Size = 256
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

function Resolve-ProjectPath {
    param([string]$PathText)
    if ([System.IO.Path]::IsPathRooted($PathText)) {
        return $PathText
    }
    return Join-Path (Split-Path $PSScriptRoot -Parent) $PathText
}

$pngFullPath = Resolve-ProjectPath $PngPath
$icoFullPath = Resolve-ProjectPath $IcoPath
$pngDir = Split-Path $pngFullPath -Parent
$icoDir = Split-Path $icoFullPath -Parent

New-Item -ItemType Directory -Path $pngDir -Force | Out-Null
New-Item -ItemType Directory -Path $icoDir -Force | Out-Null

$bitmap = New-Object System.Drawing.Bitmap $Size, $Size
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$graphics.Clear([System.Drawing.Color]::Transparent)

$blackBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 18, 18, 18))
$warmBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 242, 214, 120))
$highlightBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 255, 255, 255))
$noseBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 215, 118, 136))

$earLeft = [System.Drawing.Point[]]@(
    (New-Object System.Drawing.Point ([int]($Size * 0.23)), ([int]($Size * 0.26))),
    (New-Object System.Drawing.Point ([int]($Size * 0.39)), ([int]($Size * 0.06))),
    (New-Object System.Drawing.Point ([int]($Size * 0.47)), ([int]($Size * 0.31)))
)
$earRight = [System.Drawing.Point[]]@(
    (New-Object System.Drawing.Point ([int]($Size * 0.77)), ([int]($Size * 0.26))),
    (New-Object System.Drawing.Point ([int]($Size * 0.61)), ([int]($Size * 0.06))),
    (New-Object System.Drawing.Point ([int]($Size * 0.53)), ([int]($Size * 0.31)))
)
$graphics.FillPolygon($blackBrush, $earLeft)
$graphics.FillPolygon($blackBrush, $earRight)
$graphics.FillEllipse($blackBrush, [int]($Size * 0.16), [int]($Size * 0.18), [int]($Size * 0.68), [int]($Size * 0.60))
$graphics.FillEllipse($blackBrush, [int]($Size * 0.20), [int]($Size * 0.62), [int]($Size * 0.60), [int]($Size * 0.20))

$graphics.FillEllipse($warmBrush, [int]($Size * 0.30), [int]($Size * 0.42), [int]($Size * 0.12), [int]($Size * 0.09))
$graphics.FillEllipse($warmBrush, [int]($Size * 0.58), [int]($Size * 0.42), [int]($Size * 0.12), [int]($Size * 0.09))
$graphics.FillEllipse($highlightBrush, [int]($Size * 0.335), [int]($Size * 0.445), [int]($Size * 0.028), [int]($Size * 0.028))
$graphics.FillEllipse($highlightBrush, [int]($Size * 0.615), [int]($Size * 0.445), [int]($Size * 0.028), [int]($Size * 0.028))

$nose = [System.Drawing.Point[]]@(
    (New-Object System.Drawing.Point ([int]($Size * 0.50)), ([int]($Size * 0.53))),
    (New-Object System.Drawing.Point ([int]($Size * 0.46)), ([int]($Size * 0.58))),
    (New-Object System.Drawing.Point ([int]($Size * 0.54)), ([int]($Size * 0.58)))
)
$graphics.FillPolygon($noseBrush, $nose)

$bitmap.Save($pngFullPath, [System.Drawing.Imaging.ImageFormat]::Png)

$pngBytes = [System.IO.File]::ReadAllBytes($pngFullPath)
$iconStream = [System.IO.File]::Open($icoFullPath, [System.IO.FileMode]::Create)
$writer = New-Object System.IO.BinaryWriter($iconStream)

$writer.Write([UInt16]0)
$writer.Write([UInt16]1)
$writer.Write([UInt16]1)
$writer.Write([byte]0)
$writer.Write([byte]0)
$writer.Write([byte]0)
$writer.Write([byte]0)
$writer.Write([UInt16]1)
$writer.Write([UInt16]32)
$writer.Write([UInt32]$pngBytes.Length)
$writer.Write([UInt32]22)
$writer.Write($pngBytes)
$writer.Flush()
$writer.Close()
$iconStream.Close()

$graphics.Dispose()
$bitmap.Dispose()
$blackBrush.Dispose()
$warmBrush.Dispose()
$highlightBrush.Dispose()
$noseBrush.Dispose()

Write-Host "Generated PNG: $pngFullPath"
Write-Host "Generated ICO: $icoFullPath"
