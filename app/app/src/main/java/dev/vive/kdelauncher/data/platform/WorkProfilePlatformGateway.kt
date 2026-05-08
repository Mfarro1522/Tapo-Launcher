package dev.vive.kdelauncher.data.platform

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import androidx.core.graphics.drawable.toBitmap
import dev.vive.kdelauncher.data.WorkProfileApp
import dev.vive.kdelauncher.data.model.AppIconBitmap

interface WorkProfilePlatformGateway {
    fun getWorkProfileHandle(): UserHandle?
    fun getWorkProfileApps(userHandle: UserHandle, loadIcons: Boolean): List<WorkProfileApp>
    fun launchWorkApp(packageName: String, activityName: String, userHandle: UserHandle): Boolean
    fun loadWorkAppIcon(packageName: String, activityName: String, userHandle: UserHandle): Bitmap?
    fun isQuietModeEnabled(userHandle: UserHandle): Boolean
}

class AndroidWorkProfilePlatformGateway(
    private val context: Context
) : WorkProfilePlatformGateway {

    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    private val launcherAppsService =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    override fun getWorkProfileHandle(): UserHandle? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val myHandle = android.os.Process.myUserHandle()
                val launcherProfiles = launcherAppsService.profiles
                val fromLauncher = launcherProfiles.firstOrNull { it != myHandle }
                fromLauncher ?: userManager.userProfiles.firstOrNull { it != myHandle }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun getWorkProfileApps(
        userHandle: UserHandle,
        loadIcons: Boolean
    ): List<WorkProfileApp> {
        val pm = context.packageManager
        return launcherAppsService
            .getActivityList(null, userHandle)
            .map { activity ->
                val iconBitmap = if (loadIcons) {
                    try {
                        activity.getIcon(0)?.toBitmap(128, 128)
                    } catch (_: Exception) {
                        null
                    }
                } else {
                    null
                }
                val versionCode = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        pm.getPackageInfo(activity.applicationInfo.packageName, 0).longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getPackageInfo(activity.applicationInfo.packageName, 0).versionCode.toLong()
                    }
                } catch (_: Exception) {
                    0L
                }
                WorkProfileApp(
                    packageName = activity.applicationInfo.packageName,
                    activityName = activity.name,
                    label = activity.label.toString(),
                    userHandle = userHandle,
                    icon = iconBitmap?.let { AppIconBitmap(it) },
                    androidCategory = activity.applicationInfo.category,
                    versionCode = versionCode
                )
            }
    }

    override fun launchWorkApp(
        packageName: String,
        activityName: String,
        userHandle: UserHandle
    ): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                launcherAppsService.startMainActivity(
                    ComponentName(packageName, activityName),
                    userHandle,
                    null,
                    null
                )
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun loadWorkAppIcon(
        packageName: String,
        activityName: String,
        userHandle: UserHandle
    ): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                launcherAppsService
                    .getActivityList(packageName, userHandle)
                    .firstOrNull { it.name == activityName }
                    ?.getIcon(0)
                    ?.toBitmap(128, 128)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun isQuietModeEnabled(userHandle: UserHandle): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                userManager.isQuietModeEnabled(userHandle)
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

}
