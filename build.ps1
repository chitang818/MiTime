param(
    [string]$Task = "assembleDebug",
    [int]$VersionCode,
    [string]$VersionName,
    [switch]$InteractiveVersion
)

$ErrorActionPreference = "Stop"

$gradlePropertiesPath = ".\gradle.properties"

function Set-GradleProperty {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [Parameter(Mandatory = $true)]
        [string]$Value
    )

    $lines = @()
    if (Test-Path -LiteralPath $Path) {
        $lines = Get-Content -LiteralPath $Path -Encoding UTF8
    }

    $escapedName = [regex]::Escape($Name)
    $updated = $false
    $newLines = foreach ($line in $lines) {
        if ($line -match "^\s*$escapedName\s*=") {
            $updated = $true
            "$Name=$Value"
        } else {
            $line
        }
    }

    if (-not $updated) {
        $newLines += "$Name=$Value"
    }

    Set-Content -LiteralPath $Path -Encoding UTF8 -Value $newLines
}

function Get-GradleProperty {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        return $null
    }

    $escapedName = [regex]::Escape($Name)
    $line = Get-Content -LiteralPath $Path -Encoding UTF8 |
        Where-Object { $_ -match "^\s*$escapedName\s*=" } |
        Select-Object -First 1
    if (-not $line) {
        return $null
    }

    return ($line -split "=", 2)[1].Trim()
}

function Read-VersionCode {
    param(
        [Parameter(Mandatory = $true)]
        [string]$CurrentValue
    )

    while ($true) {
        $inputValue = Read-Host "VersionCode [$CurrentValue]"
        if ([string]::IsNullOrWhiteSpace($inputValue)) {
            return $CurrentValue
        }

        $parsed = 0
        if ([int]::TryParse($inputValue.Trim(), [ref]$parsed) -and $parsed -gt 0) {
            return $parsed.ToString()
        }

        Write-Host "VersionCode must be a positive integer."
    }
}

if ($InteractiveVersion) {
    $currentVersionCode = Get-GradleProperty -Path $gradlePropertiesPath -Name "MITIME_VERSION_CODE"
    $currentVersionName = Get-GradleProperty -Path $gradlePropertiesPath -Name "MITIME_VERSION_NAME"
    if ([string]::IsNullOrWhiteSpace($currentVersionCode)) {
        $currentVersionCode = "1"
    }
    if ([string]::IsNullOrWhiteSpace($currentVersionName)) {
        $currentVersionName = "1.0"
    }

    Write-Host "Current MiTime versionName=$currentVersionName versionCode=$currentVersionCode"

    if (-not $PSBoundParameters.ContainsKey("VersionName")) {
        $inputVersionName = Read-Host "VersionName [$currentVersionName]"
        if (-not [string]::IsNullOrWhiteSpace($inputVersionName)) {
            $VersionName = $inputVersionName.Trim()
            $PSBoundParameters["VersionName"] = $VersionName
        }
    }

    if (-not $PSBoundParameters.ContainsKey("VersionCode")) {
        $VersionCode = [int](Read-VersionCode -CurrentValue $currentVersionCode)
        $PSBoundParameters["VersionCode"] = $VersionCode
    }
}

if ($PSBoundParameters.ContainsKey("VersionCode")) {
    if ($VersionCode -le 0) {
        throw "VersionCode must be a positive integer."
    }
    Set-GradleProperty -Path $gradlePropertiesPath -Name "MITIME_VERSION_CODE" -Value $VersionCode.ToString()
}

if ($PSBoundParameters.ContainsKey("VersionName")) {
    if ([string]::IsNullOrWhiteSpace($VersionName)) {
        throw "VersionName cannot be empty."
    }
    Set-GradleProperty -Path $gradlePropertiesPath -Name "MITIME_VERSION_NAME" -Value $VersionName.Trim()
}

$currentVersionCode = Get-GradleProperty -Path $gradlePropertiesPath -Name "MITIME_VERSION_CODE"
$currentVersionName = Get-GradleProperty -Path $gradlePropertiesPath -Name "MITIME_VERSION_NAME"
Write-Host "Building MiTime versionName=$currentVersionName versionCode=$currentVersionCode task=$Task"

if (Test-Path -LiteralPath ".\gradlew.bat") {
    & .\gradlew.bat $Task
    exit $LASTEXITCODE
}

if (Get-Command gradle -ErrorAction SilentlyContinue) {
    & gradle $Task
    exit $LASTEXITCODE
}

throw "Gradle was not found. Install Android Studio/Gradle or generate a Gradle Wrapper, then rerun .\build.ps1."
