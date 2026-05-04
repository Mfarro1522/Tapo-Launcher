package dev.vive.kdelauncher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.os.Build
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import dev.vive.kdelauncher.data.IconPackManager
import dev.vive.kdelauncher.data.model.AppCategorizer
import dev.vive.kdelauncher.data.model.AppModel
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
 * 3. **Icon size** — Bitmaps are decoded at 72×72 (was 96×96). Each icon goes from
 *    ~36 KB to ~20 KB; for 150 apps that's saving ~2.4 MB of memory and decode time.
 */
class AppRepository(private val context: Context) {

    private val iconPackManager = IconPackManager(context)

    companion object {
        /**
         * Max memory for icon cache.
         * Conservative cap (8 MB) keeps us safe on low-end devices with 1-2 GB RAM.
         * 56×56 ARGB_8888 ≈ 12 KB/icon → ~680 icons fit in 8 MB, well above any device.
         */
        private val CACHE_MAX_KB = minOf(
            (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt(),
            8 * 1024   // hard cap at 8 MB
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
    suspend fun getInstalledApps(
        selectedIconPack: String? = null
    ): List<AppModel> = withContext(Dispatchers.IO) {
        try {
            val resolveInfos = queryLaunchableApps()
            val pm = context.packageManager
            val seen = mutableSetOf<String>()

            resolveInfos
                .filter { ri ->
                    val pkg = ri.activityInfo.packageName
                    if (pkg == context.packageName) return@filter false
                    seen.add(pkg)
                }
                .map { ri ->
                    val activityInfo = ri.activityInfo
                    val appInfo = activityInfo.applicationInfo
                    val androidCategory = appInfo?.category ?: -1
                    val cacheKey = "${activityInfo.packageName}|${selectedIconPack ?: ""}"

                    val bitmap = iconCache.get(cacheKey) ?: run {
                        val decoded = loadIconForApp(ri, pm, selectedIconPack,
                            activityInfo.packageName, activityInfo.name)
                        if (decoded != null) iconCache.put(cacheKey, decoded)
                        decoded
                    }

                    AppModel(
                        packageName = activityInfo.packageName,
                        activityName = activityInfo.name,
                        label = ri.loadLabel(pm).toString(),
                        iconBitmap = bitmap,
                        category = AppCategorizer.categorize(activityInfo.packageName, androidCategory)
                    )
                }
                .sortedBy { it.label.lowercase() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Fetch app list **without icons** (metadata only) — nearly instant.
     * Use this to populate the UI immediately while icons load in the background.
     */
    suspend fun getInstalledAppsMetadata(): List<AppModel> = withContext(Dispatchers.IO) {
        try {
            val resolveInfos = queryLaunchableApps()
            val pm = context.packageManager
            val seen = mutableSetOf<String>()

            resolveInfos
                .filter { ri ->
                    val pkg = ri.activityInfo.packageName
                    if (pkg == context.packageName) return@filter false
                    seen.add(pkg)
                }
                .map { ri ->
                    val activityInfo = ri.activityInfo
                    val appInfo = activityInfo.applicationInfo
                    AppModel(
                        packageName = activityInfo.packageName,
                        activityName = activityInfo.name,
                        label = ri.loadLabel(pm).toString(),
                        iconBitmap = null,          // no icon yet
                        category = AppCategorizer.categorize(
                            activityInfo.packageName, appInfo?.category ?: -1
                        )
                    )
                }
                .sortedBy { it.label.lowercase() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getLaunchIntent(packageName: String): Intent? =
        context.packageManager.getLaunchIntentForPackage(packageName)

    /**
     * Clear the icon bitmap cache.
     * Call when the user switches icon packs so stale icons are evicted.
     */
    fun clearIconPackCache() {
        iconCache.evictAll()
        iconPackManager.clearCache()
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private fun queryLaunchableApps(): List<ResolveInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }
    }

    private suspend fun loadIconForApp(
        ri: ResolveInfo,
        pm: PackageManager,
        selectedIconPack: String?,
        componentPackage: String,
        activityName: String
    ): Bitmap? {
        return if (selectedIconPack != null) {
            iconPackManager.loadIcon(
                iconPackPackage = selectedIconPack,
                componentPackage = componentPackage,
                activityName = activityName
            ) ?: try {
                ri.loadIcon(pm)?.toBitmap(56, 56)
            } catch (_: Exception) { null }
        } else {
            try {
                ri.loadIcon(pm)?.toBitmap(56, 56)   // 56 px covers all launcher grid sizes
            } catch (_: Exception) { null }
        }
    }
}
