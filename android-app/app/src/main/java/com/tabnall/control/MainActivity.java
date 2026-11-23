package com.tabnall.control;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.database.Cursor;
import androidx.core.content.FileProvider;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.SslErrorHandler;
import android.net.http.SslError;
import android.webkit.ValueCallback;
import android.view.View;
import android.widget.Toast;
import android.webkit.WebResourceError;

public class MainActivity extends Activity {
    private static final String HOME = "https://ronalarcos1-ui.github.io/tabnall-tienda/control/";
    private WebView web;
    private long updateDownloadId = -1;
    private File updateFile;
    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) return;
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id != updateDownloadId) return;
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            try (Cursor c = dm.query(new DownloadManager.Query().setFilterById(id))) {
                if (c != null && c.moveToFirst() && c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) installUpdate();
                else Toast.makeText(MainActivity.this, "No se pudo descargar la actualización", Toast.LENGTH_LONG).show();
            } catch (Exception e) { Toast.makeText(MainActivity.this, "Error al verificar la descarga", Toast.LENGTH_LONG).show(); }
        }
    };
    private ValueCallback<Uri[]> fileCallback;
    private static final int FILE_REQUEST = 1234;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        setContentView(web);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost();
                if ("https".equalsIgnoreCase(uri.getScheme()) && "ronalarcos1-ui.github.io".equalsIgnoreCase(host) && uri.getPath() != null && uri.getPath().startsWith("/tabnall-tienda/control/")) return false;
                if ("https".equalsIgnoreCase(uri.getScheme()) && "gzunmqxvalyiciedcxdp.supabase.co".equalsIgnoreCase(host)) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) {}
                return true;
            }
            @Override public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) { handler.cancel(); }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) Toast.makeText(MainActivity.this, "No se pudo conectar. Revisa internet e intenta de nuevo.", Toast.LENGTH_LONG).show();
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try { startActivityForResult(params.createIntent(), FILE_REQUEST); return true; }
                catch (Exception ex) { fileCallback.onReceiveValue(null); fileCallback = null; return true; }
            }
        });
        if (state == null) web.loadUrl(HOME); else web.restoreState(state);
        registerReceiver(updateReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        checkForUpdates();
    }
    private void checkForUpdates() {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL("https://api.github.com/repos/ronalarcos1-ui/tabnall-tienda/releases/latest").openConnection();
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                if (connection.getResponseCode() != 200) return;
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (InputStream in = connection.getInputStream()) {
                    byte[] buf = new byte[4096]; int n;
                    while ((n = in.read(buf)) != -1 && bytes.size() < 200000) bytes.write(buf, 0, n);
                }
                JSONObject release = new JSONObject(bytes.toString("UTF-8"));
                String tag = release.optString("tag_name", "");
                if (!tag.startsWith("tabnall-control-v")) return;
                String version = tag.substring("tabnall-control-v".length());
                if (!isNewer(version, getPackageManager().getPackageInfo(getPackageName(), 0).versionName)) return;
                JSONArray assets = release.optJSONArray("assets");
                if (assets == null) return;
                String url = null;
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.getJSONObject(i);
                    if ("tabnall-control.apk".equals(asset.optString("name"))) { url = asset.optString("browser_download_url"); break; }
                }
                if (url == null || !url.startsWith("https://github.com/ronalarcos1-ui/tabnall-tienda/releases/download/")) return;
                final String downloadUrl = url;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    new AlertDialog.Builder(this).setTitle("Actualización disponible")
                        .setMessage("TABNALL CONTROL " + version + " está disponible. ¿Descargar e instalar ahora? Tus datos no se eliminarán.")
                        .setNegativeButton("Después", null)
                        .setPositiveButton("Actualizar", (d, w) -> downloadUpdate(downloadUrl)).show();
                });
            } catch (Exception ignored) { /* Sin conexión: la aplicación sigue funcionando. */ }
            finally { if (connection != null) connection.disconnect(); }
        }).start();
    }

    private boolean isNewer(String remote, String installed) {
        try {
            String[] a = remote.split("\\.");
            String[] b = installed.split("\\.");
            for (int i = 0; i < Math.max(a.length, b.length); i++) {
                int x = i < a.length ? Integer.parseInt(a[i]) : 0;
                int y = i < b.length ? Integer.parseInt(b[i]) : 0;
                if (x != y) return x > y;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void downloadUpdate(String url) {
        try {
            updateFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "tabnall-control-update.apk");
            if (updateFile.exists()) updateFile.delete();
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle("Actualización TABNALL CONTROL");
            request.setDescription("Descargando versión oficial");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationUri(Uri.fromFile(updateFile));
            updateDownloadId = ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(request);
            Toast.makeText(this, "Descargando actualización…", Toast.LENGTH_LONG).show();
        } catch (Exception e) { Toast.makeText(this, "No se pudo iniciar la descarga", Toast.LENGTH_LONG).show(); }
    }

    private void installUpdate() {
        try {
            if (updateFile == null || !updateFile.isFile() || updateFile.length() < 10000) {
                Toast.makeText(this, "Archivo de actualización inválido", Toast.LENGTH_LONG).show(); return;
            }
            if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
                new AlertDialog.Builder(this).setTitle("Permitir actualización")
                    .setMessage("Android necesita autorización para instalar la actualización desde TABNALL CONTROL. Autorízala y vuelve a abrir la aplicación para instalar la versión nueva.")
                    .setPositiveButton("Abrir ajustes", (d,w) -> startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()))))
                    .setNegativeButton("Cancelar", null).show();
                return;
            }
            Uri apk = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", updateFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apk, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) { Toast.makeText(this, "No se pudo abrir el instalador", Toast.LENGTH_LONG).show(); }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_REQUEST && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileCallback = null;
        }
    }
    @Override protected void onResume() { super.onResume(); if (updateDownloadId != -1 && updateFile != null && updateFile.isFile() && updateFile.length() > 10000 && (Build.VERSION.SDK_INT < 26 || getPackageManager().canRequestPackageInstalls())) { updateDownloadId = -1; installUpdate(); } }
    @Override protected void onSaveInstanceState(Bundle out) { web.saveState(out); super.onSaveInstanceState(out); }
    @Override public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }
    @Override protected void onDestroy() {
        if (fileCallback != null) { fileCallback.onReceiveValue(null); fileCallback = null; }
        try { unregisterReceiver(updateReceiver); } catch (Exception ignored) {}
        web.destroy();
        super.onDestroy();
    }
}
