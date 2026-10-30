package com.example.alzabox

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import com.google.android.apps.auto.sdk.CarActivity
import org.json.JSONObject
import java.io.ByteArrayInputStream

class CarWebViewActivity : CarActivity() {

    private lateinit var webView: WebView
    private lateinit var keyboardContainer: LinearLayout
    private lateinit var prefs: SharedPreferences
    private var isDesktopMode = false
    private var isAdBlockEnabled = true
    private var isShiftEnabled = false
    private val letterButtons = mutableListOf<Button>()

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

    inner class JsBridge {
        @JavascriptInterface
        fun showKeyboard() {
            webView.post {
                keyboardContainer.visibility = View.VISIBLE
            }
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
        keyboardContainer = findViewById(R.id.keyboardContainer) as LinearLayout

        val btnKeyboard = findViewById(R.id.btnKeyboard) as Button
        val btnBack = findViewById(R.id.btnBack) as Button
        val btnReload = findViewById(R.id.btnReload) as Button
        val btnYT = findViewById(R.id.btnYT) as Button
        val btnViu = findViewById(R.id.btnViu) as Button
        val btnIqiyi = findViewById(R.id.btnIqiyi) as Button
        val btnNetflix = findViewById(R.id.btnNetflix) as Button
        val btnMode = findViewById(R.id.btnMode) as Button
        val btnAdBlock = findViewById(R.id.btnAdBlock) as Button

        setupWebView()
        buildOnScreenKeyboard()

        btnKeyboard.setOnClickListener { toggleKeyboard() }
        btnBack.setOnClickListener { if (webView.canGoBack()) webView.goBack() }
        btnReload.setOnClickListener { webView.reload() }

        btnYT.setOnClickListener {
            setDesktopMode(false, btnMode)
            webView.loadUrl("https://m.youtube.com")
        }

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

    private fun toggleKeyboard() {
        keyboardContainer.visibility =
            if (keyboardContainer.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun buildOnScreenKeyboard() {
        val ctx = webView.context
        keyboardContainer.removeAllViews()
        letterButtons.clear()

        val rows = listOf(
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "@", "."),
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "-", "_"),
            listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", "!", "?", "/"),
            listOf("SHIFT", "z", "x", "c", "v", "b", "n", "m", "SPACE", "DEL", "GO", "HIDE")
        )

        for (rowKeys in rows) {
            val rowLayout = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(38)
                ).apply {
                    bottomMargin = dpToPx(2)
                }
            }

            for (key in rowKeys) {
                val weight = when (key) {
                    "SPACE" -> 2.4f
                    "SHIFT", "DEL", "GO", "HIDE" -> 1.4f
                    else -> 1.0f
                }

                val btn = Button(ctx).apply {
                    text = key
                    isAllCaps = false
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    setTextColor(Color.WHITE)
                    setPadding(0, 0, 0, 0)
                    gravity = Gravity.CENTER
                    setBackgroundColor(
                        when (key) {
                            "GO" -> Color.parseColor("#1A73E8")
                            "DEL", "SHIFT", "HIDE" -> Color.parseColor("#3C4043")
                            else -> Color.parseColor("#5F6368")
                        }
                    )
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight).apply {
                        setMargins(dpToPx(2), 0, dpToPx(2), 0)
                    }
                }

                if (key.length == 1 && key[0].isLetter()) {
                    letterButtons.add(btn)
                }

                btn.setOnClickListener { handleKeyPress(key) }
                rowLayout.addView(btn)
            }
            keyboardContainer.addView(rowLayout)
        }
    }

    private fun handleKeyPress(key: String) {
        when (key) {
            "SHIFT" -> {
                isShiftEnabled = !isShiftEnabled
                for (b in letterButtons) {
                    val current = b.text.toString()
                    b.text = if (isShiftEnabled) current.uppercase() else current.lowercase()
                }
            }
            "HIDE" -> keyboardContainer.visibility = View.GONE
            "SPACE" -> sendKeyToWebView(" ")
            "DEL" -> sendKeyToWebView("BACKSPACE")
            "GO" -> {
                sendKeyToWebView("ENTER")
                keyboardContainer.visibility = View.GONE
            }
            else -> {
                val outChar = if (key.length == 1 && key[0].isLetter() && isShiftEnabled) {
                    key.uppercase()
                } else {
                    key
                }
                sendKeyToWebView(outChar)
            }
        }
    }

    private fun sendKeyToWebView(actionOrChar: String) {
        val safeKey = JSONObject.quote(actionOrChar)
        val js = "(function(key){" +
            "var el=document.activeElement||window.__lastFocusedInput;" +
            "if(!el||(el.tagName!=='INPUT'&&el.tagName!=='TEXTAREA'&&!el.isContentEditable)){" +
            "el=document.querySelector('input:not([type=\"hidden\"]),textarea');if(el)el.focus();}" +
            "if(!el)return;" +
            "if(key==='ENTER'){" +
            "var ev=new KeyboardEvent('keydown',{key:'Enter',code:'Enter',keyCode:13,which:13,bubbles:true,cancelable:true});" +
            "el.dispatchEvent(ev);" +
            "if(el.form){if(el.form.requestSubmit)el.form.requestSubmit();else el.form.submit();}" +
            "return;}" +
            "if(el.isContentEditable){" +
            "if(key==='BACKSPACE')document.execCommand('delete',false,null);" +
            "else document.execCommand('insertText',false,key);return;}" +
            "var s=el.selectionStart||el.value.length;var e=el.selectionEnd||el.value.length;" +
            "var v=el.value||'';var nv=v;var np=s;" +
            "if(key==='BACKSPACE'){if(s!==e){nv=v.slice(0,s)+v.slice(e);np=s;}else if(s>0){nv=v.slice(0,s-1)+v.slice(e);np=s-1;}}" +
            "else{nv=v.slice(0,s)+key+v.slice(e);np=s+key.length;}" +
            "var pr=el.tagName==='TEXTAREA'?window.HTMLTextAreaElement.prototype:window.HTMLInputElement.prototype;" +
            "var st=Object.getOwnPropertyDescriptor(pr,'value').set;" +
            "if(st)st.call(el,nv);else el.value=nv;" +
            "try{el.setSelectionRange(np,np);}catch(err){}" +
            "el.dispatchEvent(new Event('input',{bubbles:true}));" +
            "el.dispatchEvent(new Event('change',{bubbles:true}));" +
            "})(" + safeKey + ");"

        webView.evaluateJavascript(js, null)
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            webView.resources.displayMetrics
        ).toInt()
    }

    private fun setDesktopMode(enableDesktop: Boolean, btnMode: Button) {
        isDesktopMode = enableDesktop
        webView.settings.userAgentString = if (isDesktopMode) desktopUA else mobileUA
        btnMode.text = if (isDesktopMode) "Desktop" else "Mobile"
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
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

        webView.addJavascriptInterface(JsBridge(), "AlzaKB")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    return true
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val focusHook = "(function(){" +
                    "function h(e){var t=e.target;if(t&&(t.tagName==='INPUT'||t.tagName==='TEXTAREA'||t.isContentEditable)){" +
                    "window.__lastFocusedInput=t;if(window.AlzaKB)window.AlzaKB.showKeyboard();}}" +
                    "document.addEventListener('focusin',h,true);document.addEventListener('click',h,true);" +
                    "})();"
                view?.evaluateJavascript(focusHook, null)
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
