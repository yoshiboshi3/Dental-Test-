package dev.tilm.uhcandroid;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final int REQUEST_ASSET_TREE = 413;
    private static final String PREFS = "uhc_android";
    private static final String KEY_TREE = "asset_tree";

    private SharedPreferences prefs;
    private WebView webView;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        String saved = prefs.getString(KEY_TREE, null);
        if (saved == null) {
            showPickerScreen(null);
        } else {
            Uri uri = Uri.parse(saved);
            AssetPackResolver resolver = new AssetPackResolver(this, uri);
            if (!resolver.canReadRoot()) {
                prefs.edit().remove(KEY_TREE).apply();
                showPickerScreen("Android can no longer read the previously selected folder. Choose the Asset Pack again.");
            } else {
                launchCollection(uri);
            }
        }
    }

    private void showPickerScreen(String warning) {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        int pad = dp(28);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.rgb(235, 235, 235));

        TextView title = new TextView(this);
        title.setText("The Unofficial Homestuck Collection\nAndroid Prototype");
        title.setTextSize(24);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView body = new TextView(this);
        body.setText(
                "\nThis APK does not contain Homestuck.\n\n" +
                "Choose the root folder of your existing, untouched Unofficial Homestuck Collection Asset Pack. " +
                "Android will remember read-only access to that folder. The app frontend, settings and progress are stored locally and the reader is designed to work in airplane mode.\n\n" +
                "UHC created by Bambosh and maintained by GiovanH. This Android wrapper is an experimental port."
        );
        body.setTextSize(16);
        body.setTextColor(Color.DKGRAY);
        body.setGravity(Gravity.CENTER);
        root.addView(body, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (warning != null) {
            TextView warningView = new TextView(this);
            warningView.setText("\n" + warning);
            warningView.setTextColor(Color.rgb(170, 40, 40));
            warningView.setGravity(Gravity.CENTER);
            root.addView(warningView);
        }

        Button pick = new Button(this);
        pick.setText("Choose Asset Pack Folder");
        pick.setOnClickListener(v -> chooseAssetFolder());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = dp(24);
        root.addView(pick, buttonParams);

        setContentView(root);
    }

    private void chooseAssetFolder() {
        Intent i = new Intent("android.intent.action.OPEN_DOCUMENT_TREE");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        i.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        i.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(i, REQUEST_ASSET_TREE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_ASSET_TREE || resultCode != RESULT_OK || data == null) return;

        Uri uri = data.getData();
        if (uri == null) return;

        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException e) {
            Toast.makeText(this, "Could not keep permanent access; trying this session anyway.", Toast.LENGTH_LONG).show();
        }

        prefs.edit().putString(KEY_TREE, uri.toString()).apply();
        launchCollection(uri);
    }

    private void launchCollection(Uri treeUri) {
        AssetPackResolver resolver = new AssetPackResolver(this, treeUri);

        FrameLayout root = new FrameLayout(this);
        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        Button assets = new Button(this);
        assets.setText("Assets");
        assets.setTextSize(11);
        assets.setAlpha(0.78f);
        assets.setOnClickListener(v -> chooseAssetFolder());
        FrameLayout.LayoutParams assetsParams = new FrameLayout.LayoutParams(dp(84), dp(44), Gravity.TOP | Gravity.END);
        assetsParams.topMargin = dp(4);
        assetsParams.rightMargin = dp(4);
        root.addView(assets, assetsParams);

        setContentView(root);

        if (!resolver.looksLikeUhcPack()) {
            Toast.makeText(this,
                    "Folder selected. I could not immediately find an 'archive' folder, so if the Collection stays blank choose the Asset Pack root.",
                    Toast.LENGTH_LONG).show();
        }

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setLoadWithOverviewMode(false);
        s.setUseWideViewPort(true);
        s.setSupportMultipleWindows(false);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }

        WebView.setWebContentsDebuggingEnabled(true);
        webView.setWebViewClient(new LocalWebViewClient(this, resolver));
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                android.util.Log.d("UHCAndroid",
                        consoleMessage.message() + " @" + consoleMessage.sourceId() + ":" + consoleMessage.lineNumber());
                return true;
            }
        });

        webView.loadUrl("https://" + LocalWebViewClient.APP_HOST + "/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
