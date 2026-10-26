package com.lukeneedham.krillionwrapper.game

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.MutableContextWrapper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.CookieManager
import androidx.lifecycle.ViewModel
import com.lukeneedham.common.logger.Logger
import com.lukeneedham.krillionwrapper.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Owns the game's [WebView], so the game keeps running while the debug pages are drawn over it: the view model lives as
 * long as the game page is on the back stack, the page itself is not drawn while another page is on top. The view is
 * made with a [MutableContextWrapper] whose base is the activity while the page is drawn ([attach]) and the
 * application otherwise ([detach]), so it never keeps an activity alive.
 */
internal class GameViewModel(private val application: Application) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val webViewContext = MutableContextWrapper(application)
    private val inspector = PageInspector(application)

    /** Set when the main page fails to load, so the error screen is not hidden by a later partial load. */
    private var mainFrameFailed = false

    /** Called from the page's JavaScript when Eruda is shown or hidden, including by its own close button. */
    private inner class InspectorBridge {
        @JavascriptInterface
        fun onChanged(open: Boolean) {
            _uiState.update { it.copy(inspectorOpen = open) }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    val webView: WebView = WebView(webViewContext).apply {
        setBackgroundColor(application.getColor(R.color.background))
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
        webViewClient = GameWebViewClient(
            context = application,
            onPageStarted = { mainFrameFailed = false },
            onPageFinished = { if (!mainFrameFailed) _uiState.update { it.copy(errorShown = false) } },
            onMainFrameFailed = {
                mainFrameFailed = true
                _uiState.update { it.copy(errorShown = true) }
            },
            onHistoryChanged = { canGoBack -> _uiState.update { it.copy(canGoBack = canGoBack) } },
        )
        if (inspector.available) addJavascriptInterface(InspectorBridge(), INSPECTOR_BRIDGE)
    }

    init {
        CookieManager.getInstance().setAcceptCookie(true)
        Logger.i("Loading $GAME_URL")
        webView.loadUrl(GAME_URL)
    }

    /** The page is drawn in [context]: dialogs and the like the game opens belong to it. */
    fun attach(context: Context) {
        webViewContext.baseContext = context
    }

    /** The page is no longer drawn. */
    fun detach() {
        webViewContext.baseContext = application
    }

    fun goBack() {
        webView.goBack()
    }

    fun retry() {
        mainFrameFailed = false
        _uiState.update { it.copy(errorShown = false) }
        if (webView.url == null) webView.loadUrl(GAME_URL) else webView.reload()
    }

    fun openSettings() {
        _uiState.update { it.copy(settingsOpen = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(settingsOpen = false) }
    }

    /** The settings sheet closes and the inspector opens or closes; [GameUiState.inspectorOpen] follows the page's own events. */
    fun toggleInspector() {
        closeSettings()
        inspector.setOpen(webView, open = !_uiState.value.inspectorOpen)
    }

    fun closeInspector() {
        inspector.setOpen(webView, open = false)
    }

    override fun onCleared() {
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
    }
}
