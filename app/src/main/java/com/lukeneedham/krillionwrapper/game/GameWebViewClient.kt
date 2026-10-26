package com.lukeneedham.krillionwrapper.game

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.lukeneedham.common.logger.Logger
import com.lukeneedham.krillionwrapper.AdBlocker

internal const val GAME_URL = "https://krillion.io/"
private const val GAME_HOST = "krillion.io"

/** Keeps the game in the app, blocks its ads, and reports how loading went. */
internal class GameWebViewClient(
    private val context: Context,
    private val onPageStarted: () -> Unit,
    private val onPageFinished: () -> Unit,
    private val onMainFrameFailed: () -> Unit,
    private val onHistoryChanged: (canGoBack: Boolean) -> Unit,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val uri = request.url
        if (uri.isGameHost()) return false // stay in the app
        // Everything else (ads, social links, other sites) opens in the browser.
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { Logger.w("No app can open $uri", it) }
        return true
    }

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        // Answer ad requests with an empty page so nothing is downloaded or shown.
        val host = request.url.host
        return if (host != null && AdBlocker.isAdHost(host)) AdBlocker.emptyResponse() else null
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        onPageStarted()
        view.evaluateJavascript(AdBlocker.HIDE_ADS_SCRIPT, null)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        view.evaluateJavascript(AdBlocker.HIDE_ADS_SCRIPT, null)
        onPageFinished()
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
        onHistoryChanged(view.canGoBack())
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        // Sub-resource failures (an image, a tracker) must not replace the game with an error.
        if (request.isForMainFrame) {
            Logger.e("The game page failed to load: ${error.description} (code ${error.errorCode})")
            onMainFrameFailed()
        }
    }

    private fun Uri.isGameHost(): Boolean {
        val host = host ?: return false
        return scheme == "https" && (host == GAME_HOST || host.endsWith(".$GAME_HOST"))
    }
}
