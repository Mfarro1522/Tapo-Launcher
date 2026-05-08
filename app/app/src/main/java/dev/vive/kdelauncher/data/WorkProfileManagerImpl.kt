package dev.vive.kdelauncher.data

import android.graphics.Bitmap
import android.os.UserHandle
import dev.vive.kdelauncher.data.model.AppIconBitmap
import dev.vive.kdelauncher.data.platform.WorkProfilePlatformGateway
import dev.vive.kdelauncher.domain.repository.WorkProfileManager

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
class WorkProfileManagerImpl(
    private val workProfilePlatformGateway: WorkProfilePlatformGateway
) : WorkProfileManager {

    private var cachedWorkProfileHandle: UserHandle? = null

    /**
     * Returns true if the device has a real Android Work Profile (managed profile).
     * This requires Android 5.0+ and an MDM/EMM setup or Google Workspace account.
     */
    override fun hasRealWorkProfile(): Boolean {
        return try {
            getWorkProfileHandle() != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Returns the UserHandle for the managed work profile, or null if none exists.
     */
    override fun getWorkProfileHandle(): UserHandle? {
        cachedWorkProfileHandle?.let { return it }
        return workProfilePlatformGateway.getWorkProfileHandle()?.also {
            cachedWorkProfileHandle = it
        }
    }

    /**
     * Returns the list of launchable apps from the real work profile.
     * Uses LauncherApps API introduced in Android 5.0.
     *
     * Returns empty list if no work profile exists or permission is denied.
     */
    override fun getWorkProfileApps(loadIcons: Boolean): List<WorkProfileApp> {
        if (!hasRealWorkProfile()) return emptyList()

        return try {
            val workHandle = getWorkProfileHandle() ?: return emptyList()
            workProfilePlatformGateway.getWorkProfileApps(workHandle, loadIcons)
        } catch (_: SecurityException) {
            // Permission denied — work profile exists but we can't access it yet
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Launch an app from the work profile using LauncherApps + its UserHandle.
     * Regular startActivity() won't work for cross-profile launches.
     */
    override fun launchWorkApp(
        packageName: String,
        activityName: String,
        userHandle: UserHandle
    ): Boolean {
        return workProfilePlatformGateway.launchWorkApp(packageName, activityName, userHandle)
    }

    override fun loadWorkAppIcon(
        packageName: String,
        activityName: String,
        userHandle: UserHandle
    ): Bitmap? {
        return workProfilePlatformGateway.loadWorkAppIcon(packageName, activityName, userHandle)
    }

    /**
     * Check if the work profile is currently paused/locked (quiet mode).
     * When in quiet mode, work apps are not launchable until the user unlocks.
     *
     * Uses UserManager.isQuietModeEnabled() — available since API 24 (our minSdk is 26).
     * Quiet mode == the work profile is paused/suspended by the user or MDM.
     */
    override fun isWorkProfileLocked(): Boolean {
        return try {
            val workHandle = getWorkProfileHandle() ?: return false
            workProfilePlatformGateway.isQuietModeEnabled(workHandle)
        } catch (_: Exception) {
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
    val icon: AppIconBitmap?,
    val androidCategory: Int,
    val versionCode: Long = 0L
)
