package dev.vive.kdelauncher.domain.usecase

import android.app.Application
import android.content.Intent
import dev.vive.kdelauncher.data.model.AppIconBitmap
import dev.vive.kdelauncher.data.model.AppModel
import dev.vive.kdelauncher.data.repository.IconDiskCache
import dev.vive.kdelauncher.domain.repository.AppRepository
import dev.vive.kdelauncher.domain.repository.WorkProfileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class LoadAppsUseCase(
    private val appRepository: AppRepository,
    private val workProfileManager: WorkProfileManager,
    private val iconDiskCache: IconDiskCache
) {

    /**
     * Limits concurrent PackageManager icon decodings to prevent HWUI saturation.
     * Only acquired for cache MISSES — disk reads are I/O-bound and don't pressure HWUI,
     * so they run fully in parallel without the semaphore.
     * 4 permits ≈ 4 × ~8 MB per decode = ~32 MB peak HWUI pressure.
     */
    private val iconSemaphore = Semaphore(4)

    /**
     * Two-phase loading:
     * 1. Returns metadata-only apps instantly
     * 2. Returns fully-loaded apps with icons (from disk cache or PackageManager)
     *
     * ## Icon loading strategy (fast-path first)
     * 1. Check [IconDiskCache] — O(1) file read, ~5 ms, no HWUI pressure → run in parallel
     * 2. On miss: query PackageManager via [Semaphore] → store result in disk cache
     *
     * ## Pre-warming strategy
     * ImageBitmap lazy properties are pre-warmed in batches with yield() calls between
     * chunks. This prevents monopolizing the CPU and allows Compose frames to render
     * between batches, eliminating the startup scroll jank caused by the old sequential
     * approach that blocked the thread for the full forEach.
     */
    suspend operator fun invoke(selectedIconPack: String?): Pair<List<AppModel>, List<AppModel>> {
        return withContext(Dispatchers.IO) {
            val personalMeta = appRepository.getInstalledAppsMetadata()
            val workMeta = if (workProfileManager.hasRealWorkProfile()) {
                workProfileManager.getWorkProfileApps(loadIcons = false).map { app ->
                    AppModel(
                        packageName = app.packageName,
                        activityName = app.activityName,
                        label = app.label,
                        icon = app.icon,
                        category = dev.vive.kdelauncher.data.model.AppCategorizer.categorize(
                            app.packageName, app.androidCategory, isSystemApp = false
                        ),
                        userHandle = app.userHandle,
                        versionCode = app.versionCode,
                        isSystemApp = false
                    )
                }
            } else emptyList()

            val mergedApps = mergeApps(personalMeta, workMeta)
            val metadataApps = applySmartGrouping(applyMultimediaGrouping(mergedApps))

            val iconsByKey = withContext(Dispatchers.IO) {
                val personalDeferreds = personalMeta.map { app ->
                    async {
                        val key = iconKey(app)
                        val bitmap = loadIconWithCache(
                            packageName = app.packageName,
                            activityName = app.activityName,
                            versionCode = app.versionCode,
                            selectedIconPack = selectedIconPack,
                            fromPackageManager = {
                                appRepository.getAppIcon(
                                    packageName = app.packageName,
                                    activityName = app.activityName,
                                    selectedIconPack = selectedIconPack
                                )
                            }
                        )
                        key to bitmap
                    }
                }
                val workDeferreds = if (workMeta.isNotEmpty()) {
                    workMeta.map { app ->
                        async {
                            val key = iconKey(app)
                            // Work profile icons cannot be cached by versionCode alone
                            // (same package, different profile). Load via semaphore.
                            val bitmap = if (app.userHandle != null) {
                                iconSemaphore.withPermit {
                                    workProfileManager.loadWorkAppIcon(
                                        packageName = app.packageName,
                                        activityName = app.activityName,
                                        userHandle = app.userHandle
                                    )
                                }
                            } else null
                            key to bitmap
                        }
                    }
                } else emptyList()

                (personalDeferreds + workDeferreds).awaitAll().toMap()
            }

            val fullApps = metadataApps.map { app ->
                val key = iconKey(app)
                val bitmap = iconsByKey[key]
                if (bitmap != null) app.copy(icon = AppIconBitmap(bitmap)) else app
            }

            // Pre-warm ImageBitmap lazy properties on the IO thread.
            // Process in chunks of 20 with yield() between batches to avoid
            // monopolizing the CPU. This lets Compose render frames between
            // batches so scroll is fluid even if pre-warming is still running.
            val chunkSize = 20
            for (chunk in fullApps.chunked(chunkSize)) {
                chunk.forEach { it.icon?.imageBitmap }
                yield() // release the thread for a frame
            }

            // Evict icons for uninstalled packages (best-effort, non-blocking).
            val knownPackages = personalMeta.map { it.packageName }.toSet()
            iconDiskCache.evictUnknown(knownPackages)

            metadataApps to fullApps
        }
    }

    /**
     * Loads an icon using a two-level cache strategy:
     * 1. [IconDiskCache] (fast, parallel-safe, survives process death)
     * 2. PackageManager via [iconSemaphore] (slow, HWUI-throttled)
     */
    private suspend fun loadIconWithCache(
        packageName: String,
        activityName: String?,
        versionCode: Long,
        selectedIconPack: String?,
        fromPackageManager: suspend () -> android.graphics.Bitmap?
    ): android.graphics.Bitmap? {
        // Icon packs change all icons for a package — the disk cache key must include
        // the pack so switching packs doesn't show stale icons.
        val cacheKey = if (selectedIconPack.isNullOrEmpty()) versionCode
                       else "${selectedIconPack.hashCode()}_$versionCode".hashCode().toLong()

        // Fast path: disk cache hit — no semaphore, no HWUI pressure
        iconDiskCache.get(packageName, cacheKey)?.let { return it }

        // Slow path: decode from PackageManager (throttled to avoid HWUI saturation)
        return iconSemaphore.withPermit {
            // Double-check: another coroutine may have written the cache while we waited
            iconDiskCache.get(packageName, cacheKey)?.let { return@withPermit it }

            fromPackageManager()?.also { bitmap ->
                iconDiskCache.put(packageName, cacheKey, bitmap)
            }
        }
    }

    private fun mergeApps(
        personalApps: List<AppModel>,
        workApps: List<AppModel>
    ): List<AppModel> {
        // distinctBy eliminates any package that appears twice in the same profile scope
        // (can happen on MIUI when an app has multiple launcher activities or when the
        // APK overlay system creates phantom entries). Key = packageName + userHandle
        // so genuine work-profile duplicates (different UserHandle) are kept.
        return (personalApps + workApps)
            .distinctBy { "${it.packageName}|${it.userHandle?.hashCode() ?: 0}" }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * Apply dynamic multimedia grouping rules:
     * - If music >= 5 → keep "music" category
     * - If streaming >= 5 → keep "streaming" category
     * - If neither >= 5 but combined >= 5 → merge into "multimedia"
     * - Otherwise → merge into "media"
     */
    private fun applyMultimediaGrouping(apps: List<AppModel>): List<AppModel> {
        val musicCount = apps.count { it.category == dev.vive.kdelauncher.data.model.AppCategory.MUSIC }
        val streamingCount = apps.count { it.category == dev.vive.kdelauncher.data.model.AppCategory.STREAMING }
        val mediaCount = apps.count { it.category == "media" }

        val totalMedia = musicCount + streamingCount + mediaCount

        return if (musicCount >= 5 || streamingCount >= 5) {
            // Keep individual categories; merge any legacy "media" into "multimedia"
            apps.map { app ->
                if (app.category == "media") app.copy(category = dev.vive.kdelauncher.data.model.AppCategory.MULTIMEDIA)
                else app
            }
        } else if (totalMedia >= 5) {
            // Merge all media-related into "multimedia"
            apps.map { app ->
                if (app.category in setOf(
                        dev.vive.kdelauncher.data.model.AppCategory.MUSIC,
                        dev.vive.kdelauncher.data.model.AppCategory.STREAMING,
                        "media"
                    )
                ) {
                    app.copy(category = dev.vive.kdelauncher.data.model.AppCategory.MULTIMEDIA)
                } else app
            }
        } else {
            // Few media apps: collapse into generic "media"
            apps.map { app ->
                if (app.category in setOf(
                        dev.vive.kdelauncher.data.model.AppCategory.MUSIC,
                        dev.vive.kdelauncher.data.model.AppCategory.STREAMING
                    )
                ) {
                    app.copy(category = "media")
                } else app
            }
        }
    }

    /**
     * Apply smart grouping for wallets, shopping and dev categories:
     * - Wallets >= 4 → keep "wallets"
     * - Compras >= 4 → keep "compras"
     * - Neither >= 4 but combined >= 5 → merge into "finanzas"
     * - Dev >= 4 → keep "dev"
     * - Dev < 4 → revert to "development"
     */
    private fun applySmartGrouping(apps: List<AppModel>): List<AppModel> {
        val walletsCount = apps.count { it.category == dev.vive.kdelauncher.data.model.AppCategory.WALLETS }
        val comprasCount = apps.count { it.category == dev.vive.kdelauncher.data.model.AppCategory.COMPRAS }
        val devCount = apps.count { it.category == dev.vive.kdelauncher.data.model.AppCategory.DEV }

        val hasEnoughWallets = walletsCount >= 4
        val hasEnoughCompras = comprasCount >= 4
        val hasEnoughDev = devCount >= 4
        val shouldMergeFinanzas = !hasEnoughWallets && !hasEnoughCompras && (walletsCount + comprasCount) >= 5

        return apps.map { app ->
            when (app.category) {
                dev.vive.kdelauncher.data.model.AppCategory.DEV -> {
                    if (hasEnoughDev) app else app.copy(category = "development")
                }
                dev.vive.kdelauncher.data.model.AppCategory.WALLETS -> {
                    if (hasEnoughWallets) app
                    else if (shouldMergeFinanzas) app.copy(category = dev.vive.kdelauncher.data.model.AppCategory.FINANZAS)
                    else app.copy(category = "finance")
                }
                dev.vive.kdelauncher.data.model.AppCategory.COMPRAS -> {
                    if (hasEnoughCompras) app
                    else if (shouldMergeFinanzas) app.copy(category = dev.vive.kdelauncher.data.model.AppCategory.FINANZAS)
                    else app.copy(category = "shopping")
                }
                else -> app
            }
        }
    }

    private fun iconKey(app: AppModel): String {
        val handleId = app.userHandle?.hashCode() ?: 0
        return "${app.packageName}|${app.activityName}|$handleId"
    }
}
