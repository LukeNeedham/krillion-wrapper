package com.lukeneedham.krillionwrapper

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.lukeneedham.common.logger.Logger
import com.lukeneedham.common.ui.ProvideDebugSettings
import com.lukeneedham.krillionwrapper.ui.AppOverlay
import com.lukeneedham.krillionwrapper.ui.AppThemeValues
import com.lukeneedham.krillionwrapper.ui.AppTheme
import com.lukeneedham.krillionwrapper.ui.toDebugTheme
import org.json.JSONTokener

private const val GAME_URL = "https://krillion.io/"
private const val GAME_HOST = "krillion.io"

/** The name the debug-only inspector bridge has in the page. */
private const val INSPECTOR_BRIDGE = "KrillionDevTools"

/** Describes the elements at the bottom of the page and every fixed/sticky element, for debugging. */
private const val LAYOUT_REPORT_SCRIPT = """
    (function () {
      function d(el) {
        var cs = getComputedStyle(el), r = el.getBoundingClientRect();
        var cls = typeof el.className === 'string' && el.className.trim()
          ? '.' + el.className.trim().split(/\s+/).join('.') : '';
        return el.tagName.toLowerCase() + (el.id ? '#' + el.id : '') + cls +
          ' pos=' + cs.position + ' rect=' + [r.left, r.top, r.width, r.height].map(Math.round) +
          ' bottom=' + cs.bottom + ' pb=' + cs.paddingBottom + ' mb=' + cs.marginBottom +
          ' bg=' + cs.backgroundColor + ' z=' + cs.zIndex + ' pe=' + cs.pointerEvents;
      }
      var layout = document.querySelector('.kr-ad-layout');
      var out = [
        'viewport ' + innerWidth + 'x' + innerHeight + ' dpr=' + devicePixelRatio,
        'rail-bottom=' + (layout ? getComputedStyle(layout).getPropertyValue('--kr-rail-bottom') : 'no layout')
      ];
      [8, 40, 80, 120].forEach(function (off) {
        out.push('--- topmost at y=innerHeight-' + off);
        var el = document.elementFromPoint(innerWidth / 2, innerHeight - off), n = 0;
        while (el && n++ < 6) { out.push(d(el)); el = el.parentElement; }
      });
      out.push('--- selectors');
      ['.kr-ad-layout', '.ds-root', '.ds-done', '.ds-done-inner', '.ds-stage'].forEach(function (s) {
        var e = document.querySelector(s);
        out.push(s + ': ' + (e ? d(e) : 'none'));
      });
      out.push('--- fixed/sticky elements');
      document.querySelectorAll('body *').forEach(function (e) {
        var p = getComputedStyle(e).position;
        if (p === 'fixed' || p === 'sticky') out.push(d(e));
      });
      return out.join('\n');
    })()
"""

/** Hosts the Krillion web game full screen in a [WebView]. */
class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var errorView: View

    /** Eruda DevTools source, for inspecting the game's DOM. Only loaded in debug builds. */
    private val erudaScript by lazy {
        if (!BuildConfig.DEBUG) {
            null
        } else {
            runCatching { assets.open("eruda.js").bufferedReader().use { it.readText() } }
                .onFailure { Logger.w("Could not load eruda.js", it) }
                .getOrNull()
        }
    }

    /** Whether the Eruda inspector is showing. Kept in step with Eruda's own show/hide events. */
    private var inspectorOpen by mutableStateOf(false)

    /** Called from the page's JavaScript when Eruda is shown or hidden, including by its own close button. */
    private inner class InspectorBridge {
        @JavascriptInterface
        fun onChanged(open: Boolean) {
            runOnUiThread { inspectorOpen = open }
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
            if (erudaScript != null) addJavascriptInterface(InspectorBridge(), INSPECTOR_BRIDGE)
        }
        CookieManager.getInstance().setAcceptCookie(true)

        errorView = buildErrorView()

        setContentView(
            FrameLayout(this).apply {
                setBackgroundColor(getColor(R.color.background))
                addView(webView)
                addView(errorView)
                if (BuildConfig.DEBUG) addView(buildOverlay())
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
                    if (inspectorOpen) {
                        setPageInspectorOpen(false)
                    } else if (webView.canGoBack()) {
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

    /** Opens or closes the Eruda inspector over the game, loading it into the page on first use. */
    private fun setPageInspectorOpen(open: Boolean) {
        val eruda = erudaScript ?: return
        val script = "(function () {\n" +
            "if (!window.eruda) {\n$eruda\n}\n" +
            "if (!window.__erudaInit) {\n" +
            "  window.__erudaInit = true;\n" +
            "  eruda.init();\n" +
            // The settings sheet controls the inspector, so hide Eruda's own draggable entry button (it
            // lives in a shadow root, which a plain querySelector cannot reach).
            "  eruda.get('entryBtn').hide();\n" +
            "  var host = document.getElementById('eruda');\n" +
            "  if (host && host.shadowRoot) {\n" +
            "    var style = document.createElement('style');\n" +
            "    style.textContent = '.eruda-entry-btn { display: none !important; }';\n" +
            "    host.shadowRoot.appendChild(style);\n" +
            "  }\n" +
            // Tell the app whenever the panel opens or closes, including its own close button.
            "  eruda.get().on('show', function () { window.$INSPECTOR_BRIDGE.onChanged(true); });\n" +
            "  eruda.get().on('hide', function () { window.$INSPECTOR_BRIDGE.onChanged(false); });\n" +
            "}\n" +
            "if (${open}) eruda.show(); else eruda.hide();\n" +
            "})()"
        webView.evaluateJavascript(script, null)
    }

    /** Debug-only: shows what sits at the bottom of the page, in a dialog whose text can be copied. */
    private fun showLayoutReport() {
        webView.evaluateJavascript(LAYOUT_REPORT_SCRIPT) { raw ->
            val report = runCatching { JSONTokener(raw).nextValue() as? String }.getOrNull() ?: raw
            val text = TextView(this).apply {
                this.text = report
                textSize = 11f
                setTextIsSelectable(true)
                setPadding(32, 24, 32, 24)
            }
            AlertDialog.Builder(this)
                .setTitle("Layout report")
                .setView(android.widget.ScrollView(this).apply { addView(text) })
                .setPositiveButton("Copy") { _, _ ->
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("layout report", report))
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }

    /** The settings gear, its sheet and the debug pages, drawn over the game. Debug builds only. */
    private fun buildOverlay(): View = ComposeView(this).apply {
        setContent {
            val theme = AppThemeValues()
            AppTheme(theme) {
                ProvideDebugSettings(theme = theme.toDebugTheme()) {
                    AppOverlay(
                        inspectorOpen = inspectorOpen,
                        onToggleInspector = { setPageInspectorOpen(!inspectorOpen) },
                        onShowLayoutReport = ::showLayoutReport,
                    )
                }
            }
        }
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
