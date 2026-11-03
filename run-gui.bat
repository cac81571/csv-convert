@echo off
setlocal
cd /d "%~dp0"
chcp 65001 >nul

set "JAR=target\csv-convert-1.0.0.jar"
if not exist "%JAR%" (
  echo JAR not found. Run "mvn package" first.
  echo   %JAR%
  exit /b 1
)

java -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -cp "%JAR%" com.csvconvert.gui.App
exit /b %ERRORLEVEL%