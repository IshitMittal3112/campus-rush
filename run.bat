@echo off
title Campus Rush Launcher
cd /d "%~dp0"
set "MVN=mvn"
where mvn >nul 2>nul
if errorlevel 1 (
  set "MVN="
  for /d %%A in ("%USERPROFILE%\Downloads\apache-maven-*-bin") do for /d %%D in ("%%~fA\apache-maven-*") do if exist "%%~fD\bin\mvn.cmd" set "MVN=%%~fD\bin\mvn.cmd"
)
if not defined MVN goto no_maven
if /i "%1"=="console" goto console
if /i "%1"=="test" goto test
if /i "%1"=="sim" goto sim
call "%MVN%" -q clean javafx:run
if errorlevel 1 goto gui_failed
exit /b 0

:gui_failed
echo Campus Rush failed to start. Review the Maven error above.
pause
exit /b 1

:console
call "%MVN%" -q exec:java -Dexec.mainClass=com.campusrush.console.ConsoleMain
exit /b %errorlevel%

:test
call "%MVN%" -q clean compile exec:java -Dexec.mainClass=com.campusrush.sim.SelfTest
exit /b %errorlevel%

:sim
call "%MVN%" -q clean compile exec:java -Dexec.mainClass=com.campusrush.sim.BotSimulator
exit /b %errorlevel%

:no_maven
echo Maven was not found. Install it or add Maven\bin to PATH.
echo Download: https://maven.apache.org/download.cgi
pause
exit /b 1
