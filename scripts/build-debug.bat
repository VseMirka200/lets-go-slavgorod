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

call "%~dp0..\gradlew.bat" assembleDebug
if errorlevel 1 exit /b %errorlevel%

echo.
echo Debug APK created:
echo app\build\outputs\apk\debug\app-debug.apk
