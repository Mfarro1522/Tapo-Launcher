package dev.vive.kdelauncher

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.vive.kdelauncher.ui.LauncherViewModel
import dev.vive.kdelauncher.ui.screens.LauncherScreen
import dev.vive.kdelauncher.ui.theme.KDELauncherTheme

class MainActivity : ComponentActivity() {

    private var launcherViewModel: LauncherViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Lock to portrait programmatically rather than via manifest to avoid
        // interfering with system gesture detection (long-press Home → assistant).
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()

        val container = (application as TAPOLauncherApp).container

        // A HOME-category activity is the root of the task stack.
        // When the user presses "back" there is NOTHING behind this activity,
        // so delegating to the system causes Android to re-launch/re-create it
        // (the exact "reloads on back" bug).
        // The correct behavior: let the ViewModel handle UI state resets
        // (close settings, clear search), and if there's nothing to reset,
        // simply consume the event — do NOT delegate to the framework.
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // handleBackPress returns true if it consumed the event
                    // (e.g., closed settings). If false, we still consume it
                    // because a launcher has nowhere to "go back" to.
                    launcherViewModel?.handleBackPress()
                }
            }
        )

        setContent {
            val viewModel: LauncherViewModel = viewModel(
                factory = LauncherViewModel.Factory(container, application)
            )
            launcherViewModel = viewModel
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            KDELauncherTheme(
                profile = uiState.currentProfile,
                isDarkTheme = uiState.isDarkTheme,
                colorTheme = uiState.colorTheme
            ) {
                LauncherScreen(
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // refreshStatus() was here before but it mutates 3 StateFlows
        // (isDefaultLauncher, hasRealWorkProfile, isWorkProfileLocked) on
        // EVERY resume, triggering a cascade through systemInput → uiInput →
        // uiState that recomposes the entire screen. These values rarely
        // change, so we only check them during ViewModel init and when
        // explicitly requested (e.g., after returning from the "set default
        // launcher" settings screen via onNewIntent).
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            launcherViewModel?.resetToHome()
        }
    }
}
