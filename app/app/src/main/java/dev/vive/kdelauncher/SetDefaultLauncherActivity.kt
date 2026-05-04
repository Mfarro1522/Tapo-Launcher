package dev.vive.kdelauncher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings

/**
 * Transparent trampoline activity that opens the system "Choose Default Home App"
 * settings screen and immediately finishes itself.
 *
 * On Android 10+ (API 29+) we can deep-link directly to the Home App chooser.
 * On older Android versions we open the general Default Apps settings.
 *
 * Usage: start this activity from anywhere inside the launcher to let the user
 * set KDE Launcher as their default home screen.
 */
class SetDefaultLauncherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openDefaultLauncherSettings()
        finish()
    }

    private fun openDefaultLauncherSettings() {
        try {
            // Android 10+ has a direct deep-link to the Home App chooser
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                return
            }
        } catch (_: Exception) {}

        // Fallback: open general Default Apps settings
        try {
            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (_: Exception) {
            // Last resort: open general settings
            try {
                val intent = Intent(Settings.ACTION_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (_: Exception) {}
        }
    }
}
