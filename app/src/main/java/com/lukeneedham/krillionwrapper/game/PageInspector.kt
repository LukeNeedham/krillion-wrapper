package com.lukeneedham.krillionwrapper.game

import android.content.Context
import android.webkit.WebView
import com.lukeneedham.common.logger.Logger
import com.lukeneedham.krillionwrapper.BuildConfig

/** The name the debug-only inspector bridge has in the page. */
internal const val INSPECTOR_BRIDGE = "KrillionDevTools"

/** The debug-only Eruda DevTools, for inspecting the game's DOM. Not available in release builds. */
internal class PageInspector(private val context: Context) {

    /** Eruda DevTools source. Only loaded in debug builds. */
    private val script by lazy {
        if (!BuildConfig.DEBUG) {
            null
        } else {
            runCatching { context.assets.open("eruda.js").bufferedReader().use { it.readText() } }
                .onFailure { Logger.w("Could not load eruda.js", it) }
                .getOrNull()
        }
    }

    val available: Boolean get() = script != null

    /** Opens or closes the inspector over the game, loading it into the page on first use. */
    fun setOpen(webView: WebView, open: Boolean) {
        val eruda = script ?: return
        val js = "(function () {\n" +
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
        webView.evaluateJavascript(js, null)
    }
}
