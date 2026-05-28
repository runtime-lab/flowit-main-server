@echo off
setlocal

set SCRIPT_DIR=%~dp0
call "%SCRIPT_DIR%gradlew.bat" --write-verification-metadata sha256 resolveDependencySources
exit /b %ERRORLEVEL%
