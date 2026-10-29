package com.example.alzabox

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import com.google.android.apps.auto.sdk.CarActivity

class CarWebViewActivity : CarActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(bb: Bundle?) {
        super.onCreate(bb)
        carUiController.statusBarController.hideAppHeader()
        setContentView(R.layout.activity_car_webview)

        val prefs = getSharedPreferences("alza_prefs", Context.MODE_PRIVATE)
        val homeUrl = prefs.getString("home_url", "https://m.youtube.com") ?: "https://m.youtube.com"

        webView = findViewById(R.id.carWebView) as WebView
        val btnBack = findViewById(R.id.btnBack) as Button
        val btnHome = findViewById(R.id.btnHome) as Button
        val btnReload = findViewById(R.id.btnReload) as Button

        setupWebView()

        btnBack.setOnClickListener {
            if (webView.canGoBack()) webView.goBack()
        }
        btnHome.setOnClickListener {
            webView.loadUrl(homeUrl)
        }
        btnReload.setOnClickListener {
            webView.reload()
        }

        webView.loadUrl(homeUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return false
            }
        }
        webView.webChromeClient = WebChromeClient()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
