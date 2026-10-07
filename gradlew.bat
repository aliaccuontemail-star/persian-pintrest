@echo off
setlocal
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)
echo Gradle was not found on PATH.
echo Open this folder in Android Studio, then run "gradle wrapper --gradle-version 9.6.0" once, or install Gradle 9.6+.
exit /b 1
