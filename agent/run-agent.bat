@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

if "%CENTAURUS_AGENT_HOME%"=="" (
  set "APP_DIR=%SCRIPT_DIR%"
) else (
  set "APP_DIR=%CENTAURUS_AGENT_HOME%"
)

if exist "%APP_DIR%\.env" (
  for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%APP_DIR%\.env") do (
    if not "%%A"=="" (
      set "%%A=%%~B"
    )
  )
)

if "%CENTAURUS_AGENT_JAR%"=="" (
  set "JAR_PATH="
  for /f "delims=" %%F in ('dir /b /s "%APP_DIR%\centaurus-agent-*.jar" 2^>nul ^| findstr /v /i "\-plain\.jar"') do (
    set "JAR_PATH=%%F"
  )
) else (
  set "JAR_PATH=%CENTAURUS_AGENT_JAR%"
)

if "%JAR_PATH%"=="" (
  echo Centaurus Agent jar not found. Set CENTAURUS_AGENT_JAR or place centaurus-agent-*.jar in %APP_DIR%.
  exit /b 1
)

if not exist "%JAR_PATH%" (
  echo Centaurus Agent jar not found: %JAR_PATH%
  exit /b 1
)

if "%CENTAURUS_AGENT_CONFIG_PATH%"=="" set "CENTAURUS_AGENT_CONFIG_PATH=%APP_DIR%\agent-data\config.yml"
if "%CENTAURUS_AGENT_UI_BIND_ADDRESS%"=="" set "CENTAURUS_AGENT_UI_BIND_ADDRESS=127.0.0.1"
if "%CENTAURUS_AGENT_UI_PORT%"=="" set "CENTAURUS_AGENT_UI_PORT=8787"
if "%CENTAURUS_AGENT_UI_REMOTE_ACCESS_ENABLED%"=="" set "CENTAURUS_AGENT_UI_REMOTE_ACCESS_ENABLED=false"
if "%CENTAURUS_AGENT_AUTO_CONNECT%"=="" set "CENTAURUS_AGENT_AUTO_CONNECT=true"
if "%CENTAURUS_AGENT_RECONNECT_DELAY_SECONDS%"=="" set "CENTAURUS_AGENT_RECONNECT_DELAY_SECONDS=10"
if "%CENTAURUS_AGENT_LOG_PATH%"=="" set "CENTAURUS_AGENT_LOG_PATH=%APP_DIR%\logs\centaurus-agent.log"

for %%D in ("%CENTAURUS_AGENT_CONFIG_PATH%") do (
  if not exist "%%~dpD" mkdir "%%~dpD"
)

for %%D in ("%CENTAURUS_AGENT_LOG_PATH%") do (
  if not exist "%%~dpD" mkdir "%%~dpD"
)

if "%JAVA_BIN%"=="" (
  if not "%JAVA_HOME%"=="" (
    set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
  ) else (
    set "JAVA_BIN=java"
  )
)

"%JAVA_BIN%" %JAVA_OPTS% -jar "%JAR_PATH%"
