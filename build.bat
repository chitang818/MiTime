@echo off
setlocal

cd /d "%~dp0"

set "BUILD_TASK=assembleDebug"
if not "%~1"=="" set "BUILD_TASK=%~1"

echo.
echo MiTime one-click build
echo Task: %BUILD_TASK%
echo.
echo Enter a new version when prompted, or press Enter to keep the current value.
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File ".\build.ps1" -Task "%BUILD_TASK%" -InteractiveVersion
set "BUILD_EXIT_CODE=%ERRORLEVEL%"

echo.
if "%BUILD_EXIT_CODE%"=="0" (
    echo Build finished successfully.
    echo APK: app\build\outputs\apk\debug\app-debug.apk
) else (
    echo Build failed. Exit code: %BUILD_EXIT_CODE%
)
echo.
pause
exit /b %BUILD_EXIT_CODE%
