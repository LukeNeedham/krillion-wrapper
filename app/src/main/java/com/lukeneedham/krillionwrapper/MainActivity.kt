package com.lukeneedham.krillionwrapper

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat

/** The one activity. It only sets up the window and calls [Root]; the game is the page `game/GamePage.kt`. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Android 15+ draws edge to edge; the game page keeps the game clear of the system bars.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent { Root() }
    }
}
