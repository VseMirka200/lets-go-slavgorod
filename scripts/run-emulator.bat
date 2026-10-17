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

where adb >nul 2>nul
if errorlevel 1 (
    echo adb not found. Start an Android emulator from Android Studio and add platform-tools to PATH.
    exit /b 1
)

echo Installing the debug build on the emulator...
call "%~dp0..\gradlew.bat" installDebug
if errorlevel 1 exit /b %errorlevel%

adb -e wait-for-device
adb -e shell monkey -p ru.slavgorod.transport.debug 1
if errorlevel 1 (
    echo Could not open the app. Check that an Android emulator is running.
    exit /b 1
)

echo The app has started on the emulator.
