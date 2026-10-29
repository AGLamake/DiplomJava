# Сборка приложения в папку target\dist\SecurityAgency (exe + встроенная Java)
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

mvn -q -DskipTests package
if ($LASTEXITCODE -ne 0) { throw "Сборка Maven завершилась с ошибкой" }

$inputDir = "target\jpackage-input"
if (Test-Path $inputDir) { Remove-Item -Recurse -Force $inputDir }
New-Item -ItemType Directory -Force $inputDir | Out-Null
Copy-Item "target\security-agency-1.0.0.jar" $inputDir

if (Test-Path "target\dist") { Remove-Item -Recurse -Force "target\dist" }
jpackage --type app-image --name SecurityAgency --input $inputDir `
    --main-jar security-agency-1.0.0.jar --main-class ru.vlsu.security.Launcher --dest target\dist
if ($LASTEXITCODE -ne 0) { throw "jpackage завершился с ошибкой" }

Write-Host "Готово: target\dist\SecurityAgency\SecurityAgency.exe"
