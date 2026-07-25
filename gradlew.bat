@rem
@rem Monochrome Beat — minimal Gradle wrapper (Windows).
@rem Delegates to a system-installed gradle if available; otherwise downloads
@rem Gradle 8.10.2 to %GRADLE_USER_HOME%\wrapper\dists.

@echo off
setlocal

set SCRIPT_DIR=%~dp0
set GRADLE_VERSION=8.10.2
set DIST_URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip

if "%GRADLE_USER_HOME%"=="" set GRADLE_USER_HOME=%USERPROFILE%\.gradle
set INSTALL_DIR=%GRADLE_USER_HOME%\wrapper\dists\gradle-%GRADLE_VERSION%-bin
set GRADLE_BIN=%INSTALL_DIR%\gradle-%GRADLE_VERSION%\bin\gradle.bat

where gradle >nul 2>nul
if %ERRORLEVEL%==0 (
    gradle %* -p "%SCRIPT_DIR%"
    exit /b %ERRORLEVEL%
)

if not exist "%GRADLE_BIN%" (
    echo Downloading Gradle %GRADLE_VERSION% ...
    if not exist "%GRADLE_USER_HOME%\wrapper\dists" mkdir "%GRADLE_USER_HOME%\wrapper\dists"
    powershell -Command "$ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri '%DIST_URL%' -OutFile 'gradle.zip'"
    powershell -Command "Expand-Archive -Force 'gradle.zip' -DestinationPath '%GRADLE_USER_HOME%\wrapper\dists'"
    ren "%GRADLE_USER_HOME%\wrapper\dists%\gradle-%GRADLE_VERSION%" "gradle-%GRADLE_VERSION%-bin"
    del gradle.zip
)

call "%GRADLE_BIN%" %* -p "%SCRIPT_DIR%"
endlocal
