package dev.vive.kdelauncher.data.repository

import android.content.Intent
import android.graphics.Bitmap
import android.util.LruCache
import dev.vive.kdelauncher.data.model.AppCategorizer
import dev.vive.kdelauncher.data.model.AppIconBitmap
import dev.vive.kdelauncher.data.model.AppModel
import dev.vive.kdelauncher.data.platform.AppPlatformGateway
import dev.vive.kdelauncher.domain.repository.AppRepository
import dev.vive.kdelauncher.domain.repository.IconPackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository that queries the device's PackageManager for all launchable apps,
 * auto-categorizes them, de-duplicates by package name, and applies icon packs.
 *
 * ## Performance strategy
 *
 * Decoding all app icons on every launch was the main bottleneck (~3 mins on first
 * open). The fixes applied here:
 *
 * 1. **LruCache for bitmaps** — Decoded icons are cached in memory keyed by
 *    packageName + iconPackage. Any call after the first (e.g., package added/removed)
 *    returns cached bitmaps instantly without re-decoding.
 *
 * 2. **Two-phase loading** — [getInstalledAppsMetadata] returns the app list
 *    *without* icons instantly so the UI can display names/categories right away.
 *    [getInstalledApps] then loads/caches icons. Use both in sequence for a
 *    perceived-instant start.
 *
 * 3. **Icon size** — Bitmaps are decoded at 128×128 for crisp rendering at any
 *    launcher icon size (SMALL=28dp, MEDIUM=32dp, LARGE=40dp) without blurriness
 *    from upscaling. The memory cache uses 1/4 of available heap (capped at 32 MB)
 *    to comfortably hold 500+ 128×128 ARGB_8888 icons (~500 MB → ~31 MB).
 */
class AppRepositoryImpl(
    private val appPackageName: String,
    private val appPlatformGateway: AppPlatformGateway,
    private val iconPackManager: IconPackManager
) : AppRepository {

    companion object {
        /**
         * Max memory for icon cache.
         * 128×128 ARGB_8888 ≈ 64 KB/icon → 500 icons ~ 31 MB.
         * Uses 1/4 of available heap (capped at 32 MB) to cover any device.
         */
        private val CACHE_MAX_KB = minOf(
            (Runtime.getRuntime().maxMemory() / 1024 / 4).toInt(),
            32 * 1024  // hard cap at 32 MB
        )
    }

    /**
     * LruCache keyed by "packageName|iconPackPackage" (empty string = system icons).
     * Evicts oldest bitmaps when memory limit is reached.
     */
    private val iconCache = object : LruCache<String, Bitmap>(CACHE_MAX_KB) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            value.byteCount / 1024  // size in KB
    }

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Fetch all launchable applications **including** their decoded icons.
     * Icons are served from cache when available; otherwise decoded and cached.
     *
     * Optionally overlays icons from [selectedIconPack] (package name, or null for system).
     */
    override suspend fun getInstalledApps(
        selectedIconPack: String?
    ): List<AppModel> = buildAppList(selectedIconPack, loadIcons = true)

    /**
     * Fetch app list **without icons** (metadata only) — nearly instant.
     * Use this to populate the UI immediately while icons load in the background.
     */
    override suspend fun getInstalledAppsMetadata(): List<AppModel> =
        buildAppList(selectedIconPack = null, loadIcons = false)

    override fun getLaunchIntent(packageName: String): Intent? =
        appPlatformGateway.getLaunchIntent(packageName)

    override suspend fun getAppIcon(
        packageName: String,
        activityName: String,
        selectedIconPack: String?
    ): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "${packageName}|${selectedIconPack ?: ""}"
        iconCache.get(cacheKey) ?: run {
            val bitmap = if (selectedIconPack != null) {
                iconPackManager.loadIcon(
                    iconPackPackage = selectedIconPack,
                    componentPackage = packageName,
                    activityName = activityName
                )
            } else {
                null
            } ?: appPlatformGateway.loadAppIcon(packageName, activityName)

            if (bitmap != null) iconCache.put(cacheKey, bitmap)
            bitmap
        }
    }

    /**
     * Clear the icon bitmap cache.
     * Call when the user switches icon packs so stale icons are evicted.
     */
    override fun clearIconPackCache() {
        iconCache.evictAll()
        iconPackManager.clearCache()
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    /**
     * Unified app list builder. When [loadIcons] is true, icons are decoded
     * and cached; when false, apps are returned with null icons for fast loading.
     */
    private suspend fun buildAppList(
        selectedIconPack: String?,
        loadIcons: Boolean
    ): List<AppModel> = withContext(Dispatchers.IO) {
        try {
            val installedApps = appPlatformGateway.queryLaunchableApps(appPackageName)

            installedApps
                .map { installedApp ->
                    val icon = if (loadIcons) {
                        val cacheKey = "${installedApp.packageName}|${selectedIconPack ?: ""}"
                        val bitmap = iconCache.get(cacheKey) ?: run {
                            val decoded = loadIconForApp(
                                selectedIconPack = selectedIconPack,
                                componentPackage = installedApp.packageName,
                                activityName = installedApp.activityName
                            )
                            if (decoded != null) iconCache.put(cacheKey, decoded)
                            decoded
                        }
                        bitmap?.let { AppIconBitmap(it) }
                    } else null

                    AppModel(
                        packageName = installedApp.packageName,
                        activityName = installedApp.activityName,
                        label = installedApp.label,
                        icon = icon,
                        category = AppCategorizer.categorize(
                            installedApp.packageName,
                            installedApp.androidCategory
                        ),
                        versionCode = installedApp.versionCode
                    )
                }
                .sortedBy { it.label.lowercase() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun loadIconForApp(
        selectedIconPack: String?,
        componentPackage: String,
        activityName: String
    ): Bitmap? {
        return if (selectedIconPack != null) {
            iconPackManager.loadIcon(
                iconPackPackage = selectedIconPack,
                componentPackage = componentPackage,
                activityName = activityName
            ) ?: appPlatformGateway.loadAppIcon(componentPackage, activityName)
        } else {
            appPlatformGateway.loadAppIcon(componentPackage, activityName)
        }
    }
}
