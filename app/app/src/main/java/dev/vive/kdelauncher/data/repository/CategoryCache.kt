package dev.vive.kdelauncher.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.cacheDataStore by preferencesDataStore(name = "ai_category_cache")

data class StoredCategorization(
    val categoryId: String,
    val iconName: String,
    val source: String,
    val versionCode: Long,
    val timestamp: Long
)

data class AppCacheKey(
    val packageName: String,
    val versionCode: Long
)

fun AppCacheKey.serialize(): String = "$packageName::$versionCode"
fun String.toAppCacheKey(): AppCacheKey {
    val parts = split("::")
    return AppCacheKey(parts[0], parts[1].toLong())
}

class CategoryCache(private val context: Context) {

    suspend fun get(packageName: String, versionCode: Long): StoredCategorization? {
        val key = AppCacheKey(packageName, versionCode).serialize()
        val prefKey = stringPreferencesKey(key)
        val jsonStr = context.cacheDataStore.data.map { it[prefKey] }.first() ?: return null
        
        return try {
            val json = JSONObject(jsonStr)
            StoredCategorization(
                categoryId = json.getString("categoryId"),
                iconName = json.getString("iconName"),
                source = json.getString("source"),
                versionCode = json.getLong("versionCode"),
                timestamp = json.getLong("timestamp")
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun contains(packageName: String, versionCode: Long): Boolean {
        return get(packageName, versionCode) != null
    }

    suspend fun put(packageName: String, versionCode: Long, categorization: StoredCategorization) {
        val key = AppCacheKey(packageName, versionCode).serialize()
        val prefKey = stringPreferencesKey(key)
        val jsonStr = JSONObject().apply {
            put("categoryId", categorization.categoryId)
            put("iconName", categorization.iconName)
            put("source", categorization.source)
            put("versionCode", categorization.versionCode)
            put("timestamp", categorization.timestamp)
        }.toString()
        
        context.cacheDataStore.edit { prefs ->
            // Invalidate older versions
            val keysToRemove = prefs.asMap().keys.filter { it.name.startsWith("$packageName::") }
            keysToRemove.forEach { prefs.remove(it) }
            
            prefs[prefKey] = jsonStr
        }
    }

    suspend fun getAllPackageNames(): Set<String> {
        return context.cacheDataStore.data.first().asMap().keys.map { 
            it.name.substringBefore("::")
        }.toSet()
    }

    suspend fun removeAll(packages: Set<String>) {
        context.cacheDataStore.edit { prefs ->
            val keysToRemove = prefs.asMap().keys.filter { key ->
                packages.any { pkg -> key.name.startsWith("$pkg::") }
            }
            keysToRemove.forEach { prefs.remove(it) }
        }
    }

    /**
     * Remove entries for uninstalled apps AND outdated versions of installed apps.
     * This prevents stale cache from surviving app updates or reinstalls.
     */
    suspend fun purge(installedApps: List<Pair<String, Long>>) {
        val installedMap = installedApps.toMap()
        context.cacheDataStore.edit { prefs ->
            val keysToRemove = prefs.asMap().keys.filter { key ->
                val pkg = key.name.substringBefore("::", "")
                val ver = key.name.substringAfter("::", "").toLongOrNull()
                val currentVer = installedMap[pkg]
                // Remove if app is uninstalled or version changed
                currentVer == null || (ver != null && ver != currentVer)
            }
            keysToRemove.forEach { prefs.remove(it) }
        }
    }

    suspend fun clearAll() {
        context.cacheDataStore.edit { it.clear() }
    }
}
