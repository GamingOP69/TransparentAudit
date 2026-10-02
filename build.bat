@echo off
setlocal
cd /d "%~dp0"

set "SERVER_JAR="
for %%F in (spigot-1.8.8.jar spigot.jar paper-1.8.8.jar paper.jar craftbukkit-1.8.8.jar server.jar) do (
  if not defined SERVER_JAR if exist "%%F" set "SERVER_JAR=%%F"
)
if not defined SERVER_JAR if defined SPIGOT_JAR if exist "%SPIGOT_JAR%" set "SERVER_JAR=%SPIGOT_JAR%"

if not defined SERVER_JAR (
  echo [ERROR] Put your 1.8.8 Spigot/Paper server jar in this folder, or set SPIGOT_JAR to its path.
  echo         Example: set SPIGOT_JAR=C:\Minecraft\server\spigot-1.8.8.jar
  exit /b 1
)

if not exist build\classes mkdir build\classes
if not exist build mkdir build

echo [1/3] Compiling against %SERVER_JAR% ...
javac -source 1.8 -target 1.8 -encoding UTF-8 -cp "%SERVER_JAR%" -d build\classes src\main\java\com\notgamingop\transparentaudit\TransparentAudit.java
if errorlevel 1 exit /b 1

echo [2/3] Copying plugin resources ...
copy /y src\main\resources\plugin.yml build\classes\plugin.yml >nul
copy /y src\main\resources\config.yml build\classes\config.yml >nul

if exist build\TransparentAudit-1.2.0.jar del /q build\TransparentAudit-1.2.0.jar

echo [3/3] Building jar ...
jar cf build\TransparentAudit-1.2.0.jar -C build\classes .
if errorlevel 1 exit /b 1

echo.
echo BUILD OK: build\TransparentAudit-1.2.0.jar
echo Copy that jar into your server's plugins folder and restart the server.
endlocal
