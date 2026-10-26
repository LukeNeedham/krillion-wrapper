package com.lukeneedham.krillionwrapper.game

/**
 * What the game page shows.
 *
 * @property errorShown the game page failed to load, so the retry screen covers the game.
 * @property canGoBack the game's own history has a page to go back to, so Back goes there instead of leaving the app.
 * @property inspectorOpen the debug-only DOM inspector (Eruda) is showing over the game.
 * @property settingsOpen the debug-only settings sheet is showing.
 */
internal data class GameUiState(
    val errorShown: Boolean = false,
    val canGoBack: Boolean = false,
    val inspectorOpen: Boolean = false,
    val settingsOpen: Boolean = false,
)
