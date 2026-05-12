package dev.vive.kdelauncher.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File

/**
 * Persistent icon cache backed by compressed WebP files on internal storage.
 *
 * ## Why this exists
 * The in-memory [android.util.LruCache] is cleared whenever Android kills the launcher
 * process (e.g., after the user spends 30 minutes in YouTube). On the next return to
 * the launcher the process restarts and all icons must be re-decoded from PackageManager.
 * With 200+ apps and a Semaphore(4) throttle that takes ~2 seconds.
 *
 * This cache stores each icon as a WebP file keyed by `packageName_versionCode`. Because
 * versionCode is part of the key, stale icons are automatically invalidated on app update.
 *
 * ## Performance
 * - Cache hit (disk read): ~5–10 ms per icon, fully parallelizable (no HWUI pressure)
 * - Cache miss (PackageManager decode): ~30–50 ms per icon, limited by Semaphore(4)
 * - 200 icons cold-start with cache: ~200 ms total vs ~2 000 ms without cache
 *
 * ## Storage
 * Stored in `cacheDir/icon_cache/`. Android may evict this directory under storage
 * pressure, which is fine — a miss simply falls through to PackageManager.
 * Approximate size: 200 icons × ~20 KB (192×192 WebP quality 95) ≈ 4 MB.
 *
 * @param cacheDir The application's [android.content.Context.getCacheDir].
 */
class IconDiskCache(cacheDir: File) {

    companion object {
        private const val TAG = "IconDiskCache"
        private const val DIR_NAME = "icon_cache_v2"  // v2: 192×192 quality 95 (was v1: 96×96 quality 80)

        /** WebP quality 0-100. 95 is visually lossless at 192×192; ~20 KB/icon. */
        private const val WEBP_QUALITY = 95
    }

    private val dir = File(cacheDir, DIR_NAME).also {
        it.mkdirs()
        // Clean up old cache directory from previous version
        val oldDir = File(cacheDir, "icon_cache")
        if (oldDir.exists()) {
            oldDir.deleteRecursively()
        }
    }

    /**
     * Returns the cached [Bitmap] for the given app, or `null` on a cache miss.
     *
     * Must be called on a background thread.
     */
    fun get(packageName: String, versionCode: Long): Bitmap? {
        val file = fileFor(packageName, versionCode)
        if (!file.exists()) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode cached icon for $packageName", e)
            file.delete() // corrupt entry → remove
            null
        }
    }

    /**
     * Stores [bitmap] to disk for future process-death recoveries.
     * Automatically deletes any older version of the same package.
     *
     * Must be called on a background thread. Failures are silently swallowed
     * (best-effort: if the write fails, the next startup loads from PackageManager).
     */
    fun put(packageName: String, versionCode: Long, bitmap: Bitmap) {
        try {
            // Delete stale versions first (different versionCode)
            dir.listFiles { f ->
                f.name.startsWith("${packageName.toSafeFilename()}_") &&
                    f != fileFor(packageName, versionCode)
            }?.forEach { it.delete() }

            val file = fileFor(packageName, versionCode)
            if (file.exists()) return // already cached, nothing to do

            @Suppress("DEPRECATION")
            val format = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                Bitmap.CompressFormat.WEBP
            }
            file.outputStream().buffered().use { out ->
                bitmap.compress(format, WEBP_QUALITY, out)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write icon cache for $packageName", e)
        }
    }

    /**
     * Removes ALL cached icons. Used when switching icon packs so that stale
     * system/pack icons are not served on the next startup.
     *
     * Only called during [setIconPack] (user action, infrequent). The next
     * [LoadAppsUseCase] invocation will rebuild the cache from PackageManager
     * and the new icon pack (~2 s).
     */
    fun clear() {
        try {
            dir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.w(TAG, "clear failed", e)
        }
    }

    /**
     * Removes cached icons for packages no longer installed.
     * Call periodically (e.g., after [LoadAppsUseCase]) to avoid unbounded growth.
     */
    fun evictUnknown(knownPackages: Set<String>) {
        try {
            dir.listFiles()?.forEach { file ->
                // Filename format: "com_example_app_123456.webp"
                // Extract packageName portion (everything before the last underscore+digits)
                val raw = file.nameWithoutExtension
                val lastUnderscore = raw.lastIndexOf('_')
                if (lastUnderscore < 0) { file.delete(); return@forEach }
                val encodedPackage = raw.substring(0, lastUnderscore)
                val originalPackage = encodedPackage.fromSafeFilename()
                if (originalPackage !in knownPackages) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "evictUnknown failed", e)
        }
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private fun fileFor(packageName: String, versionCode: Long): File =
        File(dir, "${packageName.toSafeFilename()}_$versionCode.webp")

    /**
     * Encodes a package name to a filesystem-safe filename.
     * Dots are preserved; forward-slashes and special chars are replaced.
     */
    private fun String.toSafeFilename(): String = replace('/', '_').replace(':', '_')

    private fun String.fromSafeFilename(): String = this // dots kept, no reverse needed
}
