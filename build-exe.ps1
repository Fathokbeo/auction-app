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
    [string]$ChecksumFile = "SHA256.txt",
    [string]$AppImageRoot = ".jpackage\\app-image",
    [switch]$KeepBuildFolders,
    [switch]$Sign,
    [string]$PfxPath = "",
    [string]$PfxPassword = "",
    [string]$PfxPasswordEnvVar = "SIGN_PFX_PASSWORD",
    [string]$SignToolPath = "",
    [string]$TimestampUrl = "http://timestamp.digicert.com",
    [string]$FileDigest = "SHA256",
    [string]$TimestampDigest = "SHA256"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
if (Get-Variable -Name PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue) {
    $PSNativeCommandUseErrorActionPreference = $false
}

function Resolve-ProjectPath {
    param([string]$PathText)
    if ([string]::IsNullOrWhiteSpace($PathText)) {
        return ""
    }
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

function Get-SignTool {
    param([string]$RequestedPath)

    if ($RequestedPath) {
        $resolved = Resolve-ProjectPath $RequestedPath
        if (-not (Test-Path $resolved)) {
            throw "Khong tim thay signtool.exe tai $resolved"
        }
        return $resolved
    }

    $command = Get-Command signtool.exe -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $kitRoots = @(
        "C:\Program Files (x86)\Windows Kits\10\bin",
        "C:\Program Files (x86)\Windows Kits\11\bin"
    )

    foreach ($root in $kitRoots) {
        if (-not (Test-Path $root)) {
            continue
        }

        $match = Get-ChildItem -Path $root -Recurse -Filter signtool.exe -ErrorAction SilentlyContinue |
                Sort-Object FullName -Descending |
                Select-Object -First 1
        if ($match) {
            return $match.FullName
        }
    }

    return $null
}

function Get-SigningPassword {
    param([string]$DirectPassword, [string]$EnvName)

    if ($DirectPassword) {
        return $DirectPassword
    }

    if ($EnvName -and (Test-Path "Env:$EnvName")) {
        return (Get-Item "Env:$EnvName").Value
    }

    return ""
}

function Ensure-CleanDirectory {
    param([string]$PathText)
    if (Test-Path $PathText) {
        Remove-Item -Recurse -Force $PathText
    }
    New-Item -ItemType Directory -Path $PathText | Out-Null
}

function Invoke-SignTool {
    param(
        [string]$ToolPath,
        [string]$FilePath,
        [string]$CertificatePath,
        [string]$CertificatePassword,
        [string]$Description,
        [string]$TimestampServer,
        [string]$Digest,
        [string]$TimestampHash
    )

    Write-Host ("Signing {0}: {1}" -f $Description, $FilePath)
    & $ToolPath sign `
        /fd $Digest `
        /td $TimestampHash `
        /tr $TimestampServer `
        /f $CertificatePath `
        /p $CertificatePassword `
        /d $Description `
        $FilePath

    if ($LASTEXITCODE -ne 0) {
        throw "Ky so that bai cho file: $FilePath"
    }
}

function Write-Checksum {
    param([string]$TargetFile, [string]$OutputFile)
    $hash = (Get-FileHash $TargetFile -Algorithm SHA256).Hash.ToLower()
    $line = "$hash  $([System.IO.Path]::GetFileName($TargetFile))"
    Set-Content -Path $OutputFile -Value $line
}

$shouldSign = $Sign.IsPresent -or -not [string]::IsNullOrWhiteSpace($PfxPath)

$projectRoot = $PSScriptRoot
$classesPath = Resolve-ProjectPath $ClassesDir
$distPath = Resolve-ProjectPath $DistDir
$outputPath = Resolve-ProjectPath $OutputDir
$sourcePath = Resolve-ProjectPath $SourceDir
$resourcePath = Resolve-ProjectPath $ResourceDir
$javaFxPath = Resolve-ProjectPath $JavaFxDir
$checksumPath = Resolve-ProjectPath $ChecksumFile
$appImageRootPath = Resolve-ProjectPath $AppImageRoot
$jarPath = Join-Path $distPath $JarName
$resolvedPfxPath = Resolve-ProjectPath $PfxPath
$resolvedIconPath = Resolve-ProjectPath $IconPath

if (-not (Test-Path $sourcePath)) {
    throw "Khong tim thay thu muc source: $sourcePath"
}

if (-not (Test-Path $javaFxPath)) {
    throw "Khong tim thay JavaFX libs tai $javaFxPath"
}

if ($IconPath -and -not (Test-Path $resolvedIconPath)) {
    throw "Khong tim thay icon tai $resolvedIconPath"
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

$signTool = $null
$certificatePassword = ""
if ($shouldSign) {
    if (-not $resolvedPfxPath -or -not (Test-Path $resolvedPfxPath)) {
        throw "Can file .pfx hop le de ky so. Truyen -PfxPath <duong_dan.pfx>."
    }

    $certificatePassword = Get-SigningPassword -DirectPassword $PfxPassword -EnvName $PfxPasswordEnvVar
    if (-not $certificatePassword) {
        throw "Can mat khau PFX. Truyen -PfxPassword hoac dat bien moi truong $PfxPasswordEnvVar."
    }

    $signTool = Get-SignTool -RequestedPath $SignToolPath
    if (-not $signTool) {
        throw "Khong tim thay signtool.exe. Cai Windows SDK/Visual Studio Build Tools hoac truyen -SignToolPath."
    }
}

Ensure-CleanDirectory $classesPath
Ensure-CleanDirectory $distPath
Ensure-CleanDirectory $outputPath
Ensure-CleanDirectory $appImageRootPath

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

$commonArgs = @(
    "--name", $AppName,
    "--input", $distPath,
    "--main-jar", $JarName,
    "--main-class", $MainClass,
    "--module-path", $javaFxPath,
    "--add-modules", "javafx.controls",
    "--java-options", "--enable-native-access=javafx.graphics",
    "--vendor", $Vendor
)
if ($resolvedIconPath) {
    $commonArgs += @("--icon", $resolvedIconPath)
}

Write-Host "Creating app-image..."
& $jpackage @commonArgs --type app-image --dest $appImageRootPath
if ($LASTEXITCODE -ne 0) {
    throw "Tao app-image that bai."
}

$appImageDir = Join-Path $appImageRootPath $AppName
if (-not (Test-Path $appImageDir)) {
    $appImageDir = Get-ChildItem -Path $appImageRootPath -Directory | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $appImageDir) {
    throw "Khong tim thay app-image sau khi jpackage chay xong."
}

if ($shouldSign) {
    $nativeFiles = Get-ChildItem -Path $appImageDir -Recurse -File |
            Where-Object { $_.Extension -in @(".exe", ".dll") } |
            Sort-Object FullName

    foreach ($file in $nativeFiles) {
        Invoke-SignTool `
            -ToolPath $signTool `
            -FilePath $file.FullName `
            -CertificatePath $resolvedPfxPath `
            -CertificatePassword $certificatePassword `
            -Description $AppName `
            -TimestampServer $TimestampUrl `
            -Digest $FileDigest `
            -TimestampHash $TimestampDigest
    }
}

Write-Host "Packaging EXE..."
& $jpackage `
    --type exe `
    --dest $outputPath `
    --name $AppName `
    --app-version $Version `
    --vendor $Vendor `
    --win-shortcut `
    --win-menu `
    --app-image $appImageDir

if ($LASTEXITCODE -ne 0) {
    throw "Dong goi EXE that bai."
}

$exeFile = Get-ChildItem -Path $outputPath -Filter *.exe | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $exeFile) {
    throw "Khong tim thay file EXE trong $outputPath"
}

if ($shouldSign) {
    Invoke-SignTool `
        -ToolPath $signTool `
        -FilePath $exeFile.FullName `
        -CertificatePath $resolvedPfxPath `
        -CertificatePassword $certificatePassword `
        -Description $AppName `
        -TimestampServer $TimestampUrl `
        -Digest $FileDigest `
        -TimestampHash $TimestampDigest
}

Write-Host "Writing SHA256..."
Write-Checksum -TargetFile $exeFile.FullName -OutputFile $checksumPath
Write-Checksum -TargetFile $exeFile.FullName -OutputFile (Join-Path $outputPath "SHA256.txt")

if (-not $KeepBuildFolders) {
    if (Test-Path $distPath) {
        Remove-Item -Recurse -Force $distPath
    }
}

Write-Host ""
Write-Host "Hoan tat."
Write-Host "EXE: $($exeFile.FullName)"
Write-Host "SHA256: $checksumPath"
if (-not $shouldSign) {
    Write-Warning "Ban vua build ban unsigned. Smart App Control tren Windows 11 van co the chan ban nay."
    Write-Warning "De khach hang chay binh thuong, ban can build lai voi PFX RSA hop le tu nha cung cap tin cay."
}
