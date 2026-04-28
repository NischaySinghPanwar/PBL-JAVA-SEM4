@echo off
setlocal
cd /d "%~dp0"

javac -cp .;* Server.java Client.java
if errorlevel 1 (
    echo Compilation failed.
    exit /b 1
)

echo Starting Server...
java -cp .;* Server
