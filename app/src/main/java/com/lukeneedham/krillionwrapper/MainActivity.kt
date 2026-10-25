package com.lukeneedham.krillionwrapper

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.lukeneedham.common.logger.Logger

private const val GAME_URL = "https://krillion.io/"
private const val GAME_HOST = "krillion.io"

/** Hosts the Krillion web game full screen in a [WebView]. */
class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var errorView: View

    /** Eruda DevTools source, for inspecting the game's DOM. Only loaded in debuggable builds. */
    private val erudaScript by lazy {
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) {
            null
        } else {
            runCatching { assets.open("eruda.js").bufferedReader().use { it.readText() } }
                .onFailure { Logger.w("Could not load eruda.js", it) }
                .getOrNull()
        }
    }

    /** Set when the main page fails to load, so the error screen is not hidden by a later partial load. */
    private var mainFrameFailed = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(getColor(R.color.background))
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true // localStorage / sessionStorage, used for game saves
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false // let game audio start without a tap
                cacheMode = WebSettings.LOAD_DEFAULT
                allowFileAccess = false
                allowContentAccess = false
                setSupportZoom(false)
                builtInZoomControls = false
            }
            webViewClient = GameWebViewClient()
        }
        CookieManager.getInstance().setAcceptCookie(true)

        errorView = buildErrorView()

        setContentView(
            FrameLayout(this).apply {
                setBackgroundColor(getColor(R.color.background))
                addView(webView)
                addView(errorView)
                if (erudaScript != null) addView(buildDevToolsButton())
                // Android 15+ draws edge to edge; keep the game clear of the (visible) system bars.
                ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                    val safe = insets.getInsets(
                        WindowInsetsCompat.Type.systemBars() or
                            WindowInsetsCompat.Type.displayCutout() or
                            WindowInsetsCompat.Type.ime(),
                    )
                    view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
                    WindowInsetsCompat.CONSUMED
                }
            },
        )

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            },
        )

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        }
        if (webView.url == null) {
            Logger.i("Loading $GAME_URL")
            webView.loadUrl(GAME_URL)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        webView.onPause()
        CookieManager.getInstance().flush() // persist cookies in case the process is killed
        super.onPause()
    }

    override fun onDestroy() {
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
        super.onDestroy()
    }

    /** Debug-only native button that opens or closes Eruda DevTools over the game. */
    private fun buildDevToolsButton(): View {
        val dp = resources.displayMetrics.density
        return Button(this).apply {
            text = "DOM"
            textSize = 11f
            setPadding(0, 0, 0, 0)
            alpha = 0.7f
            setOnClickListener { toggleDevTools() }
            layoutParams = FrameLayout.LayoutParams((56 * dp).toInt(), (40 * dp).toInt()).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                leftMargin = (8 * dp).toInt()
                bottomMargin = (8 * dp).toInt()
            }
        }
    }

    private fun toggleDevTools() {
        val eruda = erudaScript ?: return
        val script = "(function () {\n" +
            "if (!window.eruda) {\n$eruda\n}\n" +
            "if (!window.__erudaInit) {\n" +
            "  window.__erudaInit = true;\n" +
            "  eruda.init();\n" +
            "  var btn = document.querySelector('.eruda-entry-btn'); if (btn) btn.style.display = 'none';\n" +
            "}\n" +
            "window.__erudaOpen = !window.__erudaOpen;\n" +
            "if (window.__erudaOpen) eruda.show(); else eruda.hide();\n" +
            "return window.__erudaOpen ? 'opened' : 'closed';\n" +
            "})()"
        webView.evaluateJavascript(script) { Logger.i("DevTools toggle: $it") }
    }

    private fun buildErrorView(): View {
        val dp = resources.displayMetrics.density
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(getColor(R.color.background))
            visibility = View.GONE
            isClickable = true // swallow touches meant for the hidden WebView underneath
            addView(
                TextView(context).apply {
                    setText(R.string.error_title)
                    setTextColor(Color.WHITE)
                    textSize = 18f
                    gravity = Gravity.CENTER
                },
            )
            addView(
                Button(context).apply {
                    setText(R.string.error_retry)
                    setOnClickListener { reload() }
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = (16 * dp).toInt() },
            )
        }
    }

    private fun reload() {
        errorView.visibility = View.GONE
        mainFrameFailed = false
        if (webView.url == null) webView.loadUrl(GAME_URL) else webView.reload()
    }

    private inner class GameWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            if (uri.isGameHost()) return false // stay in the app
            // Everything else (ads, social links, other sites) opens in the browser.
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                .onFailure { Logger.w("No app can open $uri", it) }
            return true
        }

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest,
        ): WebResourceResponse? {
            // Answer ad requests with an empty page so nothing is downloaded or shown.
            val host = request.url.host
            return if (host != null && AdBlocker.isAdHost(host)) AdBlocker.emptyResponse() else null
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
            mainFrameFailed = false
            view.evaluateJavascript(AdBlocker.HIDE_ADS_SCRIPT, null)
        }

        override fun onPageFinished(view: WebView, url: String?) {
            view.evaluateJavascript(AdBlocker.HIDE_ADS_SCRIPT, null)
            if (!mainFrameFailed) errorView.visibility = View.GONE
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError,
        ) {
            // Sub-resource failures (an image, a tracker) must not replace the game with an error.
            if (request.isForMainFrame) {
                Logger.e("The game page failed to load: ${error.description} (code ${error.errorCode})")
                mainFrameFailed = true
                errorView.visibility = View.VISIBLE
            }
        }
    }

    private fun Uri.isGameHost(): Boolean {
        val host = host ?: return false
        return scheme == "https" && (host == GAME_HOST || host.endsWith(".$GAME_HOST"))
    }
}
