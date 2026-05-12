package dev.vive.kdelauncher.data.platform

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.core.graphics.drawable.toBitmap

data class InstalledLauncherApp(
    val packageName: String,
    val activityName: String,
    val label: String,
    val androidCategory: Int,
    val versionCode: Long,
    val isSystemApp: Boolean = false
)

interface AppPlatformGateway {
    fun queryLaunchableApps(excludedPackageName: String): List<InstalledLauncherApp>
    fun getLaunchIntent(packageName: String): Intent?
    fun loadAppIcon(packageName: String, activityName: String): Bitmap?
}

class AndroidAppPlatformGateway(
    private val context: Context
) : AppPlatformGateway {

    override fun queryLaunchableApps(excludedPackageName: String): List<InstalledLauncherApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }

        val seen = mutableSetOf<String>()
        return resolveInfos
            .filter { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                if (packageName == excludedPackageName) return@filter false
                seen.add(packageName)
            }
            .map { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo
                val versionCode = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        pm.getPackageInfo(activityInfo.packageName, 0).longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getPackageInfo(activityInfo.packageName, 0).versionCode.toLong()
                    }
                } catch (e: Exception) {
                    0L
                }
                
                val appInfo = activityInfo.applicationInfo
                val isSystemApp = (appInfo?.flags?.and(android.content.pm.ApplicationInfo.FLAG_SYSTEM) ?: 0) != 0

                InstalledLauncherApp(
                    packageName = activityInfo.packageName,
                    activityName = activityInfo.name,
                    label = resolveInfo.loadLabel(pm).toString(),
                    androidCategory = appInfo?.category ?: -1,
                    versionCode = versionCode,
                    isSystemApp = isSystemApp
                )
            }
    }

    override fun getLaunchIntent(packageName: String): Intent? {
        return context.packageManager.getLaunchIntentForPackage(packageName)
    }

    override fun loadAppIcon(packageName: String, activityName: String): Bitmap? {
        return try {
            val activityInfo = context.packageManager.getActivityInfo(
                ComponentName(packageName, activityName),
                0
            )
            activityInfo.loadIcon(context.packageManager)?.toBitmap(ICON_SIZE_PX, ICON_SIZE_PX)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        /**
         * Icon decode size in pixels.
         * 192×192 covers up to 4× density at LARGE icon size (40dp × 4 = 160px)
         * with headroom for quality. ARGB_8888 = 144 KB per icon.
         * Most commercial launchers use 192 or 256.
         */
        const val ICON_SIZE_PX = 192
    }
}
