package com.ruhi.hud;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_VOICE = 42;
    private WebView web;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        web.setBackgroundColor(0xFF050507);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(), "Android");
        web.loadUrl("file:///android_asset/index.html");
        setContentView(web);
        immersive();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) immersive();
    }

    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onBackPressed() {
        // A home screen should not close on Back.
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_VOICE && res == RESULT_OK && data != null) {
            ArrayList<String> r = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (r != null && !r.isEmpty()) {
                String t = r.get(0).replace("\\", "\\\\").replace("'", "\\'");
                web.evaluateJavascript("window.onVoice('" + t + "')", null);
                return;
            }
        }
        if (req == REQ_VOICE) web.evaluateJavascript("window.onVoice('')", null);
    }

    private void open(Intent i) {
        try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception ignored) { }
    }

    private class Bridge {
        @JavascriptInterface
        public void launch(final String name) {
            runOnUiThread(() -> {
                switch (name) {
                    case "PHONE":    open(new Intent(Intent.ACTION_DIAL)); break;
                    case "MSG":
                    case "CHAT":     open(new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))); break;
                    case "CAMERA":   open(new Intent(MediaStore.ACTION_IMAGE_CAPTURE)); break;
                    case "WEB":      open(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))); break;
                    case "MAPS":     open(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))); break;
                    case "MAIL":     open(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))); break;
                    case "MUSIC":    open(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC)); break;
                    case "CAL":      open(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)); break;
                    case "NOTES":    open(new Intent(AlarmClock.ACTION_SHOW_ALARMS)); break;
                    case "SETTINGS": open(new Intent(Settings.ACTION_SETTINGS)); break;
                    case "APPS":     open(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CONTACTS)); break;
                    default: break;
                }
            });
        }

        @JavascriptInterface
        public void listen() {
            runOnUiThread(() -> {
                Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                try {
                    startActivityForResult(i, REQ_VOICE);
                } catch (Exception e) {
                    web.evaluateJavascript("window.onVoice('')", null);
                }
            });
        }
    }
}
