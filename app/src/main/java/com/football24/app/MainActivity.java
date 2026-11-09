package com.football24.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

// Football 24 — كل ملفات التطبيق داخل الـ APK (مجلد assets)، بدون استضافة
public class MainActivity extends Activity {
    private WebView web;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        web.setBackgroundColor(0xFF0A0D12);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setTextZoom(100);
        web.addJavascriptInterface(new Bridge(), "F24");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return openOutside(r.getUrl());
            }
            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, String url) {
                return openOutside(Uri.parse(url));
            }
        });

        if (state != null) web.restoreState(state);
        else web.loadUrl("file:///android_asset/index.html");
    }

    // أي رابط خارجي يفتح بالمتصفح، مو داخل التطبيق
    private boolean openOutside(Uri uri) {
        if ("file".equals(uri.getScheme())) return false;
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) {}
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    // زر الرجوع: يسكّر نافذة تفاصيل المباراة أول، وبعدها يطلع من التطبيق
    @Override
    public void onBackPressed() {
        web.evaluateJavascript(
            "(function(){var s=document.getElementById('sheet');if(s&&s.classList.contains('show')){closeSheet();return 1}return 0})()",
            r -> { if (!"1".equals(r)) MainActivity.super.onBackPressed(); });
    }

    // جسر صغير: الصفحة تبلّغ التطبيق بالوضع (نهاري/ليلي) حتى يتلوّن شريط الحالة
    private class Bridge {
        @JavascriptInterface
        public void theme(final String color, final boolean light) {
            runOnUiThread(() -> {
                try {
                    int c = Color.parseColor(color);
                    getWindow().setStatusBarColor(c);
                    getWindow().setNavigationBarColor(c);
                    web.setBackgroundColor(c);
                    View d = getWindow().getDecorView();
                    int f = d.getSystemUiVisibility();
                    if (Build.VERSION.SDK_INT >= 23) f = light ? (f | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) : (f & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                    if (Build.VERSION.SDK_INT >= 26) f = light ? (f | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR) : (f & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
                    d.setSystemUiVisibility(f);
                } catch (Exception ignored) {}
            });
        }
    }
}
