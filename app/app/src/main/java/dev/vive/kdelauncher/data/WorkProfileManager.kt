package dev.vive.kdelauncher.data

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import androidx.core.graphics.drawable.toBitmap

/**
 * Detects and manages Android's real Work Profile (managed profile via MDM/Enterprise).
 *
 * Android Work Profile creates a separate user (managed profile) alongside the
 * personal user. Apps in the work profile appear with a briefcase badge.
 *
 * This manager:
 * 1. Detects if the device has a real managed work profile via UserManager.
 * 2. Queries apps from the work profile using the LauncherApps API.
 * 3. Falls back gracefully if no work profile exists (device is personal-only).
 */
class WorkProfileManager(private val context: Context) {

    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager

    /**
     * Returns true if the device has a real Android Work Profile (managed profile).
     * This requires Android 5.0+ and an MDM/EMM setup or Google Workspace account.
     */
    fun hasRealWorkProfile(): Boolean {
        return try {
            getWorkProfileHandle() != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Returns the UserHandle for the managed work profile, or null if none exists.
     */
    fun getWorkProfileHandle(): UserHandle? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val myHandle = android.os.Process.myUserHandle()
                val launcherApps =
                    context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                val launcherProfiles = launcherApps.profiles
                val fromLauncher = launcherProfiles.firstOrNull { it != myHandle }
                fromLauncher ?: userManager.userProfiles.firstOrNull { it != myHandle }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Returns the list of launchable apps from the real work profile.
     * Uses LauncherApps API introduced in Android 5.0.
     *
     * Returns empty list if no work profile exists or permission is denied.
     */
    fun getWorkProfileApps(loadIcons: Boolean = true): List<WorkProfileApp> {
        if (!hasRealWorkProfile()) return emptyList()

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                val workHandle = getWorkProfileHandle() ?: return emptyList()

                launcherApps
                    .getActivityList(null, workHandle)
                    .map { activity ->
                        val iconBitmap = if (loadIcons) {
                            try {
                                activity.getIcon(0)?.toBitmap(56, 56)
                            } catch (_: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                        WorkProfileApp(
                            packageName = activity.applicationInfo.packageName,
                            activityName = activity.name,
                            label = activity.label.toString(),
                            userHandle = workHandle,
                            iconBitmap = iconBitmap,
                            androidCategory = activity.applicationInfo.category
                        )
                    }
            } else {
                emptyList()
            }
        } catch (e: SecurityException) {
            // Permission denied — work profile exists but we can't access it yet
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Launch an app from the work profile using LauncherApps + its UserHandle.
     * Regular startActivity() won't work for cross-profile launches.
     */
    fun launchWorkApp(
        packageName: String,
        activityName: String,
        userHandle: UserHandle
    ): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                val component = android.content.ComponentName(packageName, activityName)
                launcherApps.startMainActivity(component, userHandle, null, null)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun loadWorkAppIcon(
        packageName: String,
        activityName: String,
        userHandle: UserHandle
    ): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                val activity = launcherApps
                    .getActivityList(packageName, userHandle)
                    .firstOrNull { it.name == activityName }
                activity?.getIcon(0)?.toBitmap(56, 56)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Check if the work profile is currently paused/locked (quiet mode).
     * When in quiet mode, work apps are not launchable until the user unlocks.
     *
     * Uses UserManager.isQuietModeEnabled() — available since API 24 (our minSdk is 26).
     * Quiet mode == the work profile is paused/suspended by the user or MDM.
     */
    fun isWorkProfileLocked(): Boolean {
        return try {
            val workHandle = getWorkProfileHandle() ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                userManager.isQuietModeEnabled(workHandle)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Represents an app from the real Android Work Profile.
 */
data class WorkProfileApp(
    val packageName: String,
    val activityName: String,
    val label: String,
    val userHandle: UserHandle,
    val iconBitmap: Bitmap?,
    val androidCategory: Int
)
