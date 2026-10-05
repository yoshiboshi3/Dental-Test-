package dev.tilm.uhcandroid;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class LocalWebViewClient extends WebViewClient {
    static final String APP_HOST = "uhc-app.local";
    static final String ASSET_HOST = "uhc-assets.local";

    private final Context context;
    private final AssetPackResolver assetPack;

    LocalWebViewClient(Context context, AssetPackResolver assetPack) {
        this.context = context.getApplicationContext();
        this.assetPack = assetPack;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        return intercept(request.getUrl(), request.getRequestHeaders().get("Range"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        return intercept(Uri.parse(url), null);
    }

    private WebResourceResponse intercept(Uri uri, String range) {
        String host = uri.getHost();
        try {
            if (APP_HOST.equals(host)) {
                return serveBundledApp(uri.getPath());
            }
            if (ASSET_HOST.equals(host)) {
                String path = uri.getEncodedPath();
                AssetPackResolver.AssetResponse r = assetPack.open(path, range);
                return new WebResourceResponse(
                        r.mimeType,
                        null,
                        r.statusCode,
                        r.reason,
                        r.headers,
                        r.stream
                );
            }
            // The Android port is deliberately offline. Do not silently leak resource
            // requests to remote trackers/CDNs.
            if ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) {
                return textResponse(451, "Unavailable Offline",
                        "This resource is external and is disabled in the offline Android build:\n" + uri);
            }
        } catch (FileNotFoundException e) {
            return textResponse(404, "Asset Not Found",
                    "The selected UHC Asset Pack did not contain:\n" + Uri.decode(uri.getEncodedPath()) +
                            "\n\nTry choosing the Asset Pack root folder again.");
        } catch (Exception e) {
            return textResponse(500, "Asset Read Error",
                    "Could not read " + uri + "\n\n" + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return null;
    }

    private WebResourceResponse serveBundledApp(String path) throws IOException {
        if (path == null || path.isEmpty() || "/".equals(path)) path = "/index.html";
        while (path.startsWith("/")) path = path.substring(1);
        String assetName = "www/" + path;

        try {
            InputStream in = context.getAssets().open(assetName);
            return response(200, "OK", mimeFor(path), in);
        } catch (FileNotFoundException missing) {
            // Vue's history-style routes should still load the bundled SPA.
            if (!path.substring(path.lastIndexOf('/') + 1).contains(".")) {
                return response(200, "OK", "text/html", context.getAssets().open("www/index.html"));
            }
            throw missing;
        }
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return maybeExternal(request.getUrl());
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return maybeExternal(Uri.parse(url));
    }

    private boolean maybeExternal(Uri uri) {
        String host = uri.getHost();
        if (APP_HOST.equals(host) || ASSET_HOST.equals(host)) return false;
        String scheme = uri.getScheme();
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, uri);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            } catch (Exception e) {
                Toast.makeText(context, "External link unavailable while offline.", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return false;
    }

    private static WebResourceResponse response(int code, String reason, String mime, InputStream body) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Cache-Control", "no-cache");
        return new WebResourceResponse(mime, "UTF-8", code, reason, headers, body);
    }

    private static WebResourceResponse textResponse(int code, String reason, String text) {
        return response(code, reason, "text/plain",
                new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static String mimeFor(String path) {
        String p = path.toLowerCase(Locale.US);
        if (p.endsWith(".html")) return "text/html";
        if (p.endsWith(".css")) return "text/css";
        if (p.endsWith(".js")) return "application/javascript";
        if (p.endsWith(".json")) return "application/json";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
        if (p.endsWith(".gif")) return "image/gif";
        if (p.endsWith(".svg")) return "image/svg+xml";
        if (p.endsWith(".ico")) return "image/x-icon";
        if (p.endsWith(".woff2")) return "font/woff2";
        if (p.endsWith(".woff")) return "font/woff";
        if (p.endsWith(".wasm")) return "application/wasm";
        if (p.endsWith(".map")) return "application/json";
        return "application/octet-stream";
    }
}
