@echo off
setlocal EnableExtensions

if not defined JAVA_HOME (
    if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" (
        set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
    )
)

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Java not found. Set JAVA_HOME to JDK 17 or install Android Studio.
    exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"

if not defined KEYSTORE_PATH set /p "KEYSTORE_PATH=Path to keystore (.jks): "
if not exist "%KEYSTORE_PATH%" (
    echo Keystore file not found.
    exit /b 1
)

if not defined KEY_ALIAS set /p "KEY_ALIAS=Key alias: "
if not defined KEYSTORE_PASSWORD set /p "KEYSTORE_PASSWORD=Keystore password: "
if not defined KEY_PASSWORD set /p "KEY_PASSWORD=Key password: "

if not defined KEY_ALIAS goto missingCredentials
if not defined KEYSTORE_PASSWORD goto missingCredentials
if not defined KEY_PASSWORD goto missingCredentials

call "%~dp0..\gradlew.bat" assembleRelease
if errorlevel 1 exit /b %errorlevel%

echo.
echo Signed release APK created:
echo app\build\outputs\apk\release\app-release.apk
exit /b 0

:missingCredentials
echo Signing credentials are required.
exit /b 1
