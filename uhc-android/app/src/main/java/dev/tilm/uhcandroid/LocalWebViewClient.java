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
                "<style id=\"uhc-sburb-controller-style\">" +
                "#uhcOpenboundTouch{position:fixed;left:0;right:0;bottom:0;height:154px;z-index:2147483647;" +
                "pointer-events:none;font-family:monospace;-webkit-user-select:none;user-select:none}" +
                "#uhcOpenboundTouch *{box-sizing:border-box}" +
                "#uhcOpenboundTouch .plate{position:absolute;left:8px;right:8px;bottom:8px;height:138px;" +
                "border:2px solid rgba(88,255,244,.55);border-radius:20px;background:linear-gradient(180deg,rgba(11,19,30,.20),rgba(4,9,17,.58));" +
                "box-shadow:inset 0 0 0 2px rgba(255,0,170,.10),0 4px 18px rgba(0,0,0,.34);pointer-events:none}" +
                "#uhcOpenboundTouch .brand{position:absolute;left:50%;bottom:116px;transform:translateX(-50%);" +
                "color:rgba(210,255,250,.78);font:bold 10px monospace;letter-spacing:2px;text-shadow:0 1px #000}" +

                "#uhcOpenboundTouch button{pointer-events:auto;position:absolute;border:2px solid rgba(255,255,255,.78);" +
                "background:rgba(9,14,22,.70);color:#fff;font:bold 18px monospace;touch-action:none;" +
                "-webkit-tap-highlight-color:transparent;box-shadow:inset 0 -3px rgba(0,0,0,.35)}" +

                "#uhcOpenboundTouch .joy{pointer-events:auto;position:absolute;left:20px;bottom:17px;width:112px;height:112px;" +
                "border-radius:56px;border:3px solid rgba(77,235,255,.78);background:radial-gradient(circle,rgba(27,52,68,.62),rgba(4,10,18,.78));" +
                "touch-action:none;box-shadow:inset 0 0 16px rgba(0,220,255,.25)}" +
                "#uhcOpenboundTouch .joy:after{content:'';position:absolute;left:38px;top:38px;width:32px;height:32px;border-radius:50%;" +
                "background:rgba(230,255,255,.82);border:2px solid rgba(0,0,0,.55);box-shadow:0 2px 8px rgba(0,0,0,.5)}" +
                "#uhcOpenboundTouch .joyLabel{position:absolute;left:49px;bottom:61px;color:#061016;font:bold 8px monospace;z-index:1;pointer-events:none}" +

                "#uhcOpenboundTouch .u{left:171px;bottom:88px;width:44px;height:44px;border-radius:9px}" +
                "#uhcOpenboundTouch .d{left:171px;bottom:10px;width:44px;height:44px;border-radius:9px}" +
                "#uhcOpenboundTouch .l{left:132px;bottom:49px;width:44px;height:44px;border-radius:9px}" +
                "#uhcOpenboundTouch .r{left:210px;bottom:49px;width:44px;height:44px;border-radius:9px}" +

                "#uhcOpenboundTouch .abxy{position:absolute;right:14px;bottom:13px;width:150px;height:118px;pointer-events:none}" +
                "#uhcOpenboundTouch .a,#uhcOpenboundTouch .b,#uhcOpenboundTouch .x,#uhcOpenboundTouch .y{" +
                "width:48px;height:48px;border-radius:50%;font-size:18px}" +
                "#uhcOpenboundTouch .a{right:0;bottom:35px;border-color:#80ff9a;color:#9affaa}" +
                "#uhcOpenboundTouch .b{right:42px;bottom:0;border-color:#ff7272;color:#ff9d9d}" +
                "#uhcOpenboundTouch .x{right:42px;bottom:70px;border-color:#72b9ff;color:#a7d4ff}" +
                "#uhcOpenboundTouch .y{right:84px;bottom:35px;border-color:#ffe36b;color:#fff1a5}" +

                "#uhcOpenboundTouch .shoulder{width:58px;height:28px;border-radius:9px;font-size:10px;bottom:118px}" +
                "#uhcOpenboundTouch .shift{left:274px;border-color:#cf8cff;color:#e2b9ff}" +
                "#uhcOpenboundTouch .enter{right:182px;border-color:#70ffe8;color:#a5fff2}" +
                "</style>" +

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

                "function bind(btn,code,key){var down=false;" +
                "function press(ev){ev.preventDefault();if(down)return;down=true;send('keydown',code,key);}" +
                "function release(ev){if(ev)ev.preventDefault();if(!down)return;down=false;send('keyup',code,key);}" +
                "btn.addEventListener('pointerdown',press);btn.addEventListener('pointerup',release);" +
                "btn.addEventListener('pointercancel',release);btn.addEventListener('pointerleave',release);" +
                "btn.addEventListener('contextmenu',function(e){e.preventDefault();});}" +

                "function bindJoystick(el){var held={87:false,65:false,83:false,68:false};" +
                "function set(code,on,key){if(held[code]===on)return;held[code]=on;send(on?'keydown':'keyup',code,key);}" +
                "function update(ev){var r=el.getBoundingClientRect();var x=ev.clientX-(r.left+r.width/2);var y=ev.clientY-(r.top+r.height/2);" +
                "var dead=13;set(65,x<-dead,'a');set(68,x>dead,'d');set(87,y<-dead,'w');set(83,y>dead,'s');}" +
                "function clear(){set(65,false,'a');set(68,false,'d');set(87,false,'w');set(83,false,'s');}" +
                "el.addEventListener('pointerdown',function(e){e.preventDefault();try{el.setPointerCapture(e.pointerId);}catch(_){ }update(e);});" +
                "el.addEventListener('pointermove',function(e){if(e.buttons||e.pressure>0)update(e);});" +
                "el.addEventListener('pointerup',function(e){e.preventDefault();clear();});" +
                "el.addEventListener('pointercancel',clear);}" +

                "function mount(){if(document.getElementById('uhcOpenboundTouch'))return;" +
                "var c=document.createElement('div');c.id='uhcOpenboundTouch';" +
                "c.innerHTML='<div class=\"plate\"></div><div class=\"brand\">SBURB CONTROLLER</div>'+" +
                "'<div class=\"joy\"><span class=\"joyLabel\">MOVE</span></div>'+" +
                "'<button class=\"u\">▲</button><button class=\"d\">▼</button><button class=\"l\">◀</button><button class=\"r\">▶</button>'+" +
                "'<div class=\"abxy\"><button class=\"a\">A</button><button class=\"b\">B</button><button class=\"x\">X</button><button class=\"y\">Y</button></div>'+" +
                "'<button class=\"shoulder shift\">SHIFT</button><button class=\"shoulder enter\">ENTER</button>';" +
                "document.body.appendChild(c);" +

                "bindJoystick(c.querySelector('.joy'));" +
                "bind(c.querySelector('.u'),38,'ArrowUp');bind(c.querySelector('.d'),40,'ArrowDown');" +
                "bind(c.querySelector('.l'),37,'ArrowLeft');bind(c.querySelector('.r'),39,'ArrowRight');" +
                "bind(c.querySelector('.a'),32,' ');bind(c.querySelector('.b'),17,'Control');" +
                "bind(c.querySelector('.x'),88,'x');bind(c.querySelector('.y'),90,'z');" +
                "bind(c.querySelector('.shift'),16,'Shift');bind(c.querySelector('.enter'),13,'Enter');}" +
                "if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount);else mount();" +
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
