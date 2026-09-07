package com.shalltear.shellylauncher

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.shalltear.shellylauncher.ui.screen.LauncherScreen
import kotlinx.coroutines.flow.MutableSharedFlow


class MainActivity : ComponentActivity() {

    companion object {
        /**
         * Registered by LauncherScreen via DisposableEffect.
         * Called by [onWindowFocusChanged] when the window regains focus (e.g. lock screen
         * unlock) so the correct status-bar visibility is restored without waiting for a
         * Compose recomposition to occur.
         */
        @Volatile var reapplyImmersive: (() -> Unit)? = null

        /**
         * Emits a Unit whenever the user presses the Home button while this launcher
         * is already in the foreground. LauncherScreen collects this to collapse all
         * open overlays back to the clean homescreen state.
         */
        val homeEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allow the launcher to draw behind system bars so the status bar
        // can be fully hidden on the homescreen without a black gap.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            LauncherScreen()
        }

        // Keep nav bar black; status bar visibility is driven from LauncherScreen.
        window.navigationBarColor = android.graphics.Color.BLACK
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(0, android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Re-apply the correct status-bar state whenever the window regains focus.
        // This handles the lock-screen-unlock case where Android restores the status bar
        // without triggering a Compose recomposition (so SideEffect wouldn't re-run).
        if (hasFocus) reapplyImmersive?.invoke()
    }

    /**
     * Called when the user presses the Home button while this activity is already
     * in the foreground (i.e. this launcher IS the current home screen).
     * We signal LauncherScreen to close all open overlays.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN) {
            homeEvents.tryEmit(Unit)
        }
    }
}