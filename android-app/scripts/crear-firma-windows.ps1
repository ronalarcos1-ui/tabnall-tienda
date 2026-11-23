# TABNALL CONTROL - Firma permanente Android (Windows)
# Ejecutar localmente en PowerShell; nunca subir el archivo .jks ni su copia Base64 a GitHub.
$ErrorActionPreference = 'Stop'
Write-Host 'TABNALL CONTROL - Generador de firma permanente' -ForegroundColor Yellow
$keytool = Get-Command keytool -ErrorAction SilentlyContinue
if (-not $keytool) {
  Write-Host 'No se encontró keytool. Instala un JDK 17 (por ejemplo Temurin) y vuelve a ejecutar.' -ForegroundColor Red
  exit 1
}
$folder = Join-Path ([Environment]::GetFolderPath('MyDocuments')) 'TABNALL-FIRMA-PRIVADA'
New-Item -ItemType Directory -Path $folder -Force | Out-Null
$keystore = Join-Path $folder 'tabnall-control-release.jks'
if (Test-Path $keystore) {
  Write-Host 'Ya existe una firma. No se generará otra para evitar perder la identidad de la app.' -ForegroundColor Red
  exit 1
}
Write-Host 'Se generará la clave en: ' $keystore
Write-Host 'Elige una contraseña larga y guárdala en un gestor de contraseñas. No la compartas.'
& $keytool.Source -genkeypair -v -keystore $keystore -alias tabnallcontrol -keyalg RSA -keysize 3072 -validity 10000 -storetype PKCS12 -dname 'CN=TABNALL CONTROL, OU=Aplicacion, O=TABNALL, C=CO'
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $keystore)) { throw 'No se pudo generar la firma.' }
Write-Host 'Firma creada correctamente.' -ForegroundColor Green
Write-Host 'Haz una copia de seguridad cifrada de este archivo y conserva la contraseña.'
Write-Host 'Para GitHub Actions, codifica este archivo en Base64 y guárdalo SOLO como secreto TABNALL_KEYSTORE_BASE64.'
Write-Host 'Configura además TABNALL_KEYSTORE_PASSWORD, TABNALL_KEY_ALIAS (tabnallcontrol) y TABNALL_KEY_PASSWORD.'
Write-Host 'No subas archivos .jks, .p12 ni texto Base64 al repositorio.'
