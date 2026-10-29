package com.example.alzabox

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import com.google.android.apps.auto.sdk.CarActivity
import java.io.ByteArrayInputStream

class CarWebViewActivity : CarActivity() {

    private lateinit var webView: WebView
    private lateinit var prefs: SharedPreferences
    private var isDesktopMode = false
    private var isAdBlockEnabled = true

    private val mobileUA = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
    private val desktopUA = "Mozilla/5.0 (X11; CrOS x86_64 14541.0.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

    private val adDomains = listOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com"
    )

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == "trigger_time" || key == "home_url") {
            val newUrl = sharedPreferences.getString("home_url", "https://m.youtube.com") ?: "https://m.youtube.com"
            webView.post { webView.loadUrl(newUrl) }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(bb: Bundle?) {
        super.onCreate(bb)
        getCarUiController().getStatusBarController().hideAppHeader()
        setContentView(R.layout.activity_car_webview)

        prefs = getSharedPreferences("alza_prefs", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(prefListener)

        val homeUrl = prefs.getString("home_url", "https://m.youtube.com") ?: "https://m.youtube.com"

        webView = findViewById(R.id.carWebView) as WebView
        val btnBack = findViewById(R.id.btnBack) as Button
        val btnReload = findViewById(R.id.btnReload) as Button
        val btnYT = findViewById(R.id.btnYT) as Button
        val btnViu = findViewById(R.id.btnViu) as Button
        val btnIqiyi = findViewById(R.id.btnIqiyi) as Button
        val btnNetflix = findViewById(R.id.btnNetflix) as Button
        val btnMode = findViewById(R.id.btnMode) as Button
        val btnAdBlock = findViewById(R.id.btnAdBlock) as Button

        setupWebView()

        btnBack.setOnClickListener { if (webView.canGoBack()) webView.goBack() }
        btnReload.setOnClickListener { webView.reload() }

        btnYT.setOnClickListener {
            setDesktopMode(false, btnMode)
            webView.loadUrl("https://m.youtube.com")
        }

        // Viu, iQiyi, and Netflix work best in Desktop mode so they don't redirect to the Play Store
        btnViu.setOnClickListener {
            setDesktopMode(true, btnMode)
            webView.loadUrl("https://www.viu.com")
        }

        btnIqiyi.setOnClickListener {
            setDesktopMode(true, btnMode)
            webView.loadUrl("https://www.iq.com")
        }

        btnNetflix.setOnClickListener {
            setDesktopMode(true, btnMode)
            webView.loadUrl("https://www.netflix.com")
        }

        btnMode.setOnClickListener {
            setDesktopMode(!isDesktopMode, btnMode)
            webView.reload()
        }

        btnAdBlock.setOnClickListener {
            isAdBlockEnabled = !isAdBlockEnabled
            btnAdBlock.text = if (isAdBlockEnabled) "AdBlk:ON" else "AdBlk:OFF"
            webView.reload()
        }

        webView.loadUrl(homeUrl)
    }

    private fun setDesktopMode(enableDesktop: Boolean, btnMode: Button) {
        isDesktopMode = enableDesktop
        webView.settings.userAgentString = if (isDesktopMode) desktopUA else mobileUA
        btnMode.text = if (isDesktopMode) "Desktop" else "Mobile"
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.userAgentString = mobileUA

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                // Block intent:// links from kicking the user out to the Play Store
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    return true
                }
                return false
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                if (isAdBlockEnabled && request != null) {
                    val host = request.url.host ?: ""
                    if (adDomains.any { host.contains(it) }) {
                        return WebResourceResponse(
                            "text/plain",
                            "utf-8",
                            ByteArrayInputStream(ByteArray(0))
                        )
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            // Grant Widevine DRM permission (RESOURCE_PROTECTED_MEDIA_ID) for streaming sites
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.let {
                    it.grant(it.resources)
                }
            }
        }
    }

    override fun onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        webView.destroy()
        super.onDestroy()
    }
}
