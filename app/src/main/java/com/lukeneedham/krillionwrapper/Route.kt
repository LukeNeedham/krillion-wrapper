package com.lukeneedham.krillionwrapper

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Every page of the app. The wrapper has one page of its own, the game; the other three are the shared library's. */
@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Game : Route

    @Serializable
    data object Debug : Route

    @Serializable
    data object Logs : Route

    @Serializable
    data object Crashes : Route
}
