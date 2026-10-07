$ErrorActionPreference = "Stop"
if (-not (Test-Path ".\gradlew.bat")) { throw "Gradle wrapper is missing. Open the project once in Android Studio and generate the wrapper, or install Gradle 9.6+ and place it on PATH." }
if (-not (Test-Path ".\bumo.jks")) {
  keytool -genkeypair -v -keystore bumo.jks -keyalg RSA -keysize 2048 -validity 10000 -alias bumo -storepass bumo123 -keypass bumo123 -dname "CN=Bumo" -noprompt
}
.\gradlew.bat assembleRelease
New-Item -ItemType Directory -Force -Path .\output | Out-Null
Copy-Item .\app\build\outputs\apk\release\app-release.apk .\output\bumo.apk -Force
Write-Host "Built: bumo-android/output/bumo.apk"
