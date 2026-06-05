param(
    [string]$ApkPath = ".\app\build\outputs\apk\debug\app-debug.apk"
)

$ErrorActionPreference = "Stop"

$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path -LiteralPath $adb)) {
    throw "adb.exe was not found under $adb"
}

if (-not (Test-Path -LiteralPath $ApkPath)) {
    throw "APK was not found: $ApkPath"
}

$packageName = "com.chitang.mitime"

& $adb install -r $ApkPath
if ($LASTEXITCODE -eq 0) {
    & $adb shell appops set $packageName WRITE_SETTINGS allow
    exit 0
}

Write-Host "Normal install failed. Retrying with --bypass-low-target-sdk-block..."
& $adb install -r --bypass-low-target-sdk-block $ApkPath
if ($LASTEXITCODE -eq 0) {
    & $adb shell appops set $packageName WRITE_SETTINGS allow
}
exit $LASTEXITCODE
