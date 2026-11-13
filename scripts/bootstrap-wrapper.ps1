# Obtém o JAR padrão do Gradle Wrapper a partir do MDK oficial FIXADO.
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$destination = Join-Path $root 'gradle\wrapper\gradle-wrapper.jar'
$expected = '1b33c55baabb587c669f562ae36f953de2481846'
$url = 'https://raw.githubusercontent.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle/e93d7f09d4eec12c76d1d7be9bf4eaad0246e9e0/gradle/wrapper/gradle-wrapper.jar'

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    throw 'Git deve estar instalado para verificar a integridade do Gradle Wrapper JAR.'
}
if (-not (Test-Path $destination)) {
    Write-Host 'Baixando Gradle Wrapper JAR do MDK oficial fixado...'
    Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $destination
}
$actual = (& git hash-object $destination).Trim()
if ($LASTEXITCODE -ne 0 -or $actual -ne $expected) {
    Remove-Item -Path $destination -Force -ErrorAction SilentlyContinue
    throw "Integridade do Gradle Wrapper JAR invalida: $actual"
}
Write-Host 'Gradle Wrapper JAR validado.'
