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
import java.io.ByteArrayOutputStream;
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

    private static boolean isMusicHost(String host) {
        if (host == null) return false;
        String h = host.toLowerCase(Locale.US);
        return h.equals("bandcamp.com") ||
                h.endsWith(".bandcamp.com") ||
                h.equals("bcbits.com") ||
                h.endsWith(".bcbits.com");
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
                if (r.statusCode == 200 && r.mimeType != null &&
                        r.mimeType.toLowerCase(Locale.US).startsWith("text/html")) {
                    return serveResponsiveLegacyHtml(r, path);
                }
                if (r.statusCode == 200 && path != null &&
                        path.toLowerCase(Locale.US).endsWith(".xml")) {
                    return serveRewrittenTextAsset(r, "application/xml");
                }
                return new WebResourceResponse(
                        r.mimeType,
                        null,
                        r.statusCode,
                        r.reason,
                        r.headers,
                        r.stream
                );
            }
            // The story/app remains fully offline. The one deliberate exception is
            // Homestuck's music database, whose original player is a Bandcamp embed.
            // If the device is offline the player simply fails normally; no story
            // content depends on network access.
            if ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) {
                if (isMusicHost(host)) return null;
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


    private WebResourceResponse serveResponsiveLegacyHtml(AssetPackResolver.AssetResponse source, String path)
            throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (InputStream in = source.stream) {
            byte[] chunk = new byte[16384];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
        }

        // ISO-8859-1 is intentional here: converting arbitrary legacy HTML bytes
        // to String and back with this charset is byte-preserving for 0x00-0xFF.
        String html = new String(buffer.toByteArray(), StandardCharsets.ISO_8859_1);
        // Raw UHC HTML pages were authored for Electron's custom assets:// protocol.
        // Translate those references to the Android bridge origin before WebView parses them.
        html = html.replace("assets://", "https://" + ASSET_HOST + "/");
        boolean isSburb =
                html.contains("Sburb.initialize") ||
                html.contains("Sburb.min.js") ||
                html.contains("SBURBgameDiv");

        String openboundTouch = isSburb ? (
                "<script>(function(){" +
                "function report(msg){try{var id='uhcOpenboundError';var p=document.getElementById(id);" +
                "if(!p){p=document.createElement('pre');p.id=id;p.style.cssText='position:fixed;left:6px;right:6px;" +
                "top:6px;z-index:2147483647;max-height:42vh;overflow:auto;background:rgba(0,0,0,.86);color:#ffb;" +
                "padding:8px;font:11px monospace;white-space:pre-wrap;pointer-events:none';document.body.appendChild(p);}p.textContent+=msg+'\\n';}catch(_){}}" +
                "window.addEventListener('error',function(e){report('JS '+(e.message||'error')+' @ '+(e.filename||'')+':'+(e.lineno||''));});" +
                "window.addEventListener('unhandledrejection',function(e){report('PROMISE '+String(e.reason));});" +
                "function eventFor(type,code,key){var e=new Event(type,{bubbles:true,cancelable:true});" +
                "try{Object.defineProperty(e,'keyCode',{value:code});Object.defineProperty(e,'which',{value:code});" +
                "Object.defineProperty(e,'key',{value:key});}catch(_){ }return e;}" +
                "function target(){return document.getElementById('SBURBgameDiv')||document.getElementById('SBURBStage')||document.body;}" +
                "function send(type,code,key){var t=target();try{var st=document.getElementById('SBURBStage');if(st)st.focus();}catch(_){ }" +
                "var e=eventFor(type,code,key);var h=(type==='keydown'?t.onkeydown:t.onkeyup);" +
                "if(typeof h==='function'){try{h.call(t,e);return;}catch(err){report('INPUT '+String(err));}}" +
                "t.dispatchEvent(e);}" +
                "window.addEventListener('message',function(e){var d=e.data||{};" +
                "if(d.type==='uhcSburbInput'){send(d.down?'keydown':'keyup',Number(d.code),String(d.key||''));}});" +
                "function ready(){try{window.parent.postMessage({type:'uhcSburbReady'},'*');}catch(_){}}" +
                "if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',ready);else ready();" +
                "window.addEventListener('pagehide',function(){try{window.parent.postMessage({type:'uhcSburbGone'},'*');}catch(_){}});" +
                "})();</script>"
        ) : "";

        String injection =
                "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">" +
                "<style id=\"uhc-android-legacy-fit\">" +
                "html,body{max-width:100vw!important;min-width:0!important;overflow-x:hidden!important;}" +
                "body>*{max-width:100%!important;box-sizing:border-box!important;}" +
                "img,video,object,embed,iframe,canvas{max-width:100%!important;}" +
                "pre{white-space:pre-wrap!important;overflow-wrap:anywhere!important;}" +
                "</style>" +
                openboundTouch +
                "<script>(function(){" +
                "function fit(){try{" +
                "document.body.style.zoom='';document.body.style.width='';" +
                "var vw=document.documentElement.clientWidth||window.innerWidth;" +
                "var w=Math.max(document.documentElement.scrollWidth,document.body.scrollWidth);" +
                "if(w>vw+8){var z=Math.max(.55,Math.min(1,vw/w));" +
                "document.body.style.zoom=String(z);document.body.style.width=(100/z)+'%';}" +
                "}catch(e){}}" +
                "window.addEventListener('load',function(){fit();setTimeout(fit,250);setTimeout(fit,1000);});" +
                "window.addEventListener('resize',fit);" +
                "})();</script>";

        String lower = html.toLowerCase(Locale.US);
        int headEnd = lower.indexOf("</head>");
        if (headEnd >= 0) {
            html = html.substring(0, headEnd) + injection + html.substring(headEnd);
        } else {
            int bodyStart = lower.indexOf("<body");
            html = bodyStart >= 0
                    ? html.substring(0, bodyStart) + injection + html.substring(bodyStart)
                    : injection + html;
        }

        byte[] out = html.getBytes(StandardCharsets.ISO_8859_1);
        Map<String, String> headers = new HashMap<>(source.headers);
        headers.remove("Content-Length");
        headers.put("Content-Length", Integer.toString(out.length));
        headers.put("Cache-Control", "no-cache");
        headers.put("Access-Control-Allow-Origin", "*");
        return new WebResourceResponse(
                "text/html",
                null,
                200,
                "OK",
                headers,
                new ByteArrayInputStream(out)
        );
    }

    private WebResourceResponse serveRewrittenTextAsset(
            AssetPackResolver.AssetResponse source, String mime) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (InputStream in = source.stream) {
            byte[] chunk = new byte[16384];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
        }

        String text = new String(buffer.toByteArray(), StandardCharsets.UTF_8)
                .replace("assets://", "https://" + ASSET_HOST + "/");
        byte[] out = text.getBytes(StandardCharsets.UTF_8);

        Map<String, String> headers = new HashMap<>(source.headers);
        headers.remove("Content-Length");
        headers.put("Content-Length", Integer.toString(out.length));
        headers.put("Cache-Control", "no-cache");
        headers.put("Access-Control-Allow-Origin", "*");

        return new WebResourceResponse(
                mime,
                "UTF-8",
                200,
                "OK",
                headers,
                new ByteArrayInputStream(out)
        );
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
        if (APP_HOST.equals(host) || ASSET_HOST.equals(host) || isMusicHost(host)) return false;
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
        if (p.endsWith(".xml")) return "application/xml";
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
