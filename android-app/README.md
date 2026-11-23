# TABNALL CONTROL para Android

Proyecto Android WebView de TABNALL CONTROL. La aplicación abre exclusivamente la versión HTTPS publicada en GitHub Pages y conserva el acceso a Supabase. No incorpora datos de ventas, contraseñas ni claves privadas.

## Descargar APK
En GitHub, abre **Actions → Compilar APK TABNALL CONTROL → última ejecución exitosa → Artifacts → TABNALL-CONTROL-Android-APK**. Descomprime el ZIP y transfiere `app-debug.apk` al teléfono Android para instalarlo. Android puede pedir autorización para instalar aplicaciones de esa fuente. Este APK es de **pruebas**, firmado con la clave de depuración automática de GitHub Actions; no es una versión de distribución estable ni de Play Store. Para una versión de producción es necesario configurar firma privada persistente y probar la app en un dispositivo real.

## Seguridad
La aplicación solo carga el sitio web HTTPS oficial. La autenticación y los datos continúan gestionados por Supabase. La sesión de la APK es independiente de la sesión de Chrome. No reemplaza la app PWA de iPhone.

## Firma permanente para producción (Windows)
1. Instala JDK 17 y verifica que el comando `keytool` esté disponible.
2. Descarga `android-app/scripts/crear-firma-windows.ps1` desde el repositorio y ejecútalo localmente en PowerShell. Revisa el contenido antes de ejecutarlo.
3. La firma se crea en `Documentos/TABNALL-FIRMA-PRIVADA/tabnall-control-release.jks`. Haz una copia de seguridad cifrada, fuera del repositorio.
4. En GitHub → Settings → Secrets and variables → Actions, configura los cuatro **Repository secrets**: `TABNALL_KEYSTORE_BASE64` (contenido Base64 del archivo .jks), `TABNALL_KEYSTORE_PASSWORD`, `TABNALL_KEY_ALIAS` (valor `tabnallcontrol`) y `TABNALL_KEY_PASSWORD`. No pegues ningún secreto en issues, commits o conversaciones.
5. En Actions → Compilar APK TABNALL CONTROL → Run workflow, marca `release` para generar la APK firmada. La compilación fallará de forma segura si faltan secretos.
6. Para cada nueva APK nativa, incrementa `versionCode` y `versionName` en `android-app/app/build.gradle`. Conserva siempre `applicationId` y la misma firma.
7. La APK de pruebas `app-debug.apk` tiene una firma distinta: para pasar a la versión release podría requerir desinstalación. Verifica primero la sincronización y el respaldo de datos en Supabase.

**Importante:** GitHub Actions almacena el artefacto release durante 30 días. La firma debe permanecer bajo control del propietario. Si se pierde, las futuras APK no podrán actualizar las instalaciones firmadas con esa clave.
