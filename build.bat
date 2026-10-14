@echo off
setlocal enabledelayedexpansion

REM ==============================================================================
REM Soulbound QoL - Windows Build Script
REM Compiles all sources with Java 21 and packages soulbound-qol-1.0.0.jar
REM ==============================================================================

echo === Building Soulbound QoL Addon (Windows) ===

REM 1. Locate Java compiler
set "JAVAC_BIN="
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" set "JAVAC_BIN=%JAVA_HOME%\bin\javac.exe"
)
if not defined JAVAC_BIN (
    for %%X in (javac.exe) do set "JAVAC_BIN=%%~$PATH:X"
)
if not defined JAVAC_BIN (
    echo Error: javac not found! Please set JAVA_HOME or ensure javac is in PATH.
    exit /b 1
)

echo Using javac: %JAVAC_BIN%

REM 2. Determine Profile & Metadata paths
set "MODRINTH_PROFILE=%APPDATA%\ModrinthApp\profiles\Soulbound"
if not exist "%MODRINTH_PROFILE%" (
    if exist "%APPDATA%\.minecraft" set "MODRINTH_PROFILE=%APPDATA%\.minecraft"
)
set "FABRIC_META=%APPDATA%\ModrinthApp\meta"

REM 3. Clean and prepare build directory
if exist build\classes rmdir /s /q build\classes
if not exist build\classes mkdir build\classes
if not exist build\libs mkdir build\libs

REM 4. Collect Java sources into a temp file
set "SOURCES_FILE=%TEMP%\soulbound_sources_%RANDOM%.txt"
if exist "%SOURCES_FILE%" del "%SOURCES_FILE%"
for /r src %%F in (*.java) do (
    echo %%F >> "%SOURCES_FILE%"
)

REM 5. Construct Classpath
set "CP="
for /r "%MODRINTH_PROFILE%" %%F in (client-intermediary.jar) do if not defined CP (set "CP=%%F")
for /r libs %%F in (turnbattle*.jar) do set "CP=!CP!;%%F"
if exist "%MODRINTH_PROFILE%\mods" (
    for /r "%MODRINTH_PROFILE%\mods" %%F in (turnbattle*.jar) do set "CP=!CP!;%%F"
)
if exist "%FABRIC_META%" (
    for /r "%FABRIC_META%" %%F in (*.jar) do set "CP=!CP!;%%F"
)
if exist "%MODRINTH_PROFILE%\.fabric\processedMods" (
    for /r "%MODRINTH_PROFILE%\.fabric\processedMods" %%F in (*.jar) do set "CP=!CP!;%%F"
)

REM 6. Compile sources
echo Compiling Java sources...
"%JAVAC_BIN%" --release 21 -proc:none -cp "!CP!" -d build\classes @"%SOURCES_FILE%"
set "BUILD_STATUS=%ERRORLEVEL%"
if exist "%SOURCES_FILE%" del "%SOURCES_FILE%"

if %BUILD_STATUS% neq 0 (
    echo Compilation failed!
    exit /b %BUILD_STATUS%
)

REM 7. Copy resources
echo Copying mod resources...
copy /y src\resources\fabric.mod.json build\classes\ >nul
copy /y src\resources\soulbound-qol.mixins.json build\classes\ >nul

REM 8. Package JAR
echo Packaging soulbound-qol-1.0.0.jar...
jar -cf build\libs\soulbound-qol-1.0.0.jar -C build\classes .
copy /y build\libs\soulbound-qol-1.0.0.jar soulbound-qol-1.0.0.jar >nul

echo Build successful! Created: soulbound-qol-1.0.0.jar

REM 9. Optional install
if "%~1"=="--install" (
    if exist "%MODRINTH_PROFILE%\mods" (
        copy /y soulbound-qol-1.0.0.jar "%MODRINTH_PROFILE%\mods\soulbound-qol-1.0.0.jar" >nul
        echo Installed to "%MODRINTH_PROFILE%\mods\soulbound-qol-1.0.0.jar"
    ) else (
        echo Warning: Target mods directory does not exist.
    )
)

exit /b 0
