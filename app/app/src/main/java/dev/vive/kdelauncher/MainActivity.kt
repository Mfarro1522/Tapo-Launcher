package dev.vive.kdelauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.collectAsState
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
        enableEdgeToEdge()

        setContent {
            val viewModel: LauncherViewModel = viewModel()
            launcherViewModel = viewModel
            val uiState by viewModel.uiState.collectAsState()

            KDELauncherTheme(
                profile = uiState.currentProfile,
                isDarkTheme = uiState.isDarkTheme
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

    /**
     * Re-check default launcher status and work profile every time the user
     * returns to the launcher. This handles the case where they went to
     * Settings → Default Apps to change the home app.
     */
    override fun onResume() {
        super.onResume()
        launcherViewModel?.refreshStatus()
    }

    /**
     * Override back press to prevent exiting the launcher.
     * As a home screen, pressing back should do nothing.
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Do nothing — launcher should not exit on back press
    }
}
