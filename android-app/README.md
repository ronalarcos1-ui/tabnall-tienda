# TABNALL CONTROL para Android

Proyecto Android WebView de TABNALL CONTROL. La aplicación abre exclusivamente la versión HTTPS publicada en GitHub Pages y conserva el acceso a Supabase. No incorpora datos de ventas, contraseñas ni claves privadas.

## Descargar APK
En GitHub, abre **Actions → Compilar APK TABNALL CONTROL → última ejecución exitosa → Artifacts → TABNALL-CONTROL-Android-APK**. Descomprime el ZIP y transfiere `app-debug.apk` al teléfono Android para instalarlo. Android puede pedir autorización para instalar aplicaciones de esa fuente. Este APK es de **pruebas**, firmado con la clave de depuración automática de GitHub Actions; no es una versión de distribución estable ni de Play Store. Para una versión de producción es necesario configurar firma privada persistente y probar la app en un dispositivo real.

## Seguridad
La aplicación solo carga el sitio web HTTPS oficial. La autenticación y los datos continúan gestionados por Supabase. La sesión de la APK es independiente de la sesión de Chrome. No reemplaza la app PWA de iPhone.
