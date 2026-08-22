@echo off
setlocal
cd /d "%~dp0"

echo Baue und starte Sandbox-IDE (Gradle-Task "runIde") ...
echo Erster Start kann laenger dauern (IDE-Download/-Aufbau).
echo.

call gradlew.bat runIde

echo.
echo Beendet. Exit-Code: %ERRORLEVEL%
pause
endlocal
