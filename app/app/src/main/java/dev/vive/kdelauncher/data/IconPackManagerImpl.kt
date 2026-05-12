package dev.vive.kdelauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import dev.vive.kdelauncher.domain.repository.IconPackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Represents a detected icon pack app.
 */
data class IconPackInfo(
    val packageName: String,
    val label: String,
    val previewIcon: Bitmap?,
)

/**
 * Manages icon pack detection and icon loading.
 *
 * Icon packs are APKs that:
 *  1. Declare specific intent-filter actions
 *  2. Contain an appfilter.xml asset mapping component names → drawable names
 *
 * Standard intent filter actions used by major launchers:
 */
class IconPackManagerImpl(private val context: Context) : IconPackManager {

    private val ICON_PACK_INTENTS = listOf(
        "org.adw.launcher.THEMES",
        "com.novalauncher.THEME",
        "com.teslacoilsw.launcher.THEME",
        "com.dlto.atom.launcher.THEME",
        "android.intent.action.MAIN",  // some packs use this with category
    )

    // Cache: packageName → (componentName → drawableName)
    private var cachedPackage: String? = null
    private var cachedFilter: Map<String, String> = emptyMap()
    private var cachedResources: Resources? = null

    /**
     * Query PackageManager for all installed icon packs.
     */
    override suspend fun getInstalledPacks(): List<IconPackInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packs = mutableMapOf<String, IconPackInfo>()

        val intents = listOf(
            "org.adw.launcher.THEMES",
            "com.novalauncher.THEME",
            "com.teslacoilsw.launcher.THEME",
            "com.dlto.atom.launcher.THEME",
        )

        for (action in intents) {
            val intent = Intent(action)
            val resolveInfos = try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    pm.queryIntentActivities(
                        intent,
                        PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                    )
                } else {
                    @Suppress("DEPRECATION")
                    pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                }
            } catch (e: Exception) {
                emptyList()
            }

            for (ri in resolveInfos) {
                val pkg = ri.activityInfo.packageName
                if (pkg !in packs) {
                    val label = try {
                        ri.loadLabel(pm).toString()
                    } catch (e: Exception) { pkg }

                    val icon = try {
                        ri.loadIcon(pm)?.toBitmap(96, 96)
                    } catch (e: Exception) { null }

                    packs[pkg] = IconPackInfo(pkg, label, icon)
                }
            }
        }

        packs.values.sortedBy { it.label }
    }

    /**
     * Load a Bitmap for the given package/activity from the icon pack.
     * Returns null if no mapping exists for this component.
     */
    override suspend fun loadIcon(
        iconPackPackage: String,
        componentPackage: String,
        activityName: String
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            ensurePackLoaded(iconPackPackage)
            val resources = cachedResources ?: return@withContext null

            // Try full component first, then just package name
            val componentKey = "$componentPackage/$activityName"
            val pkgKey = "$componentPackage/"

            val drawableName = cachedFilter[componentKey]
                ?: cachedFilter[pkgKey]
                ?: return@withContext null

            val resId = resources.getIdentifier(drawableName, "drawable", iconPackPackage)
            if (resId == 0) return@withContext null

            val drawable: Drawable = resources.getDrawable(resId, null)
            drawable.toBitmap(
                dev.vive.kdelauncher.data.platform.AndroidAppPlatformGateway.ICON_SIZE_PX,
                dev.vive.kdelauncher.data.platform.AndroidAppPlatformGateway.ICON_SIZE_PX
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Load and parse appfilter.xml from the icon pack if not already cached.
     */
    private fun ensurePackLoaded(packageName: String) {
        if (cachedPackage == packageName && cachedResources != null) return

        try {
            cachedResources = context.packageManager
                .getResourcesForApplication(packageName)

            val res = cachedResources ?: return
            cachedFilter = parseAppFilter(packageName, res)
            cachedPackage = packageName
        } catch (e: Exception) {
            cachedResources = null
            cachedFilter = emptyMap()
            cachedPackage = null
        }
    }

    /**
     * Parse appfilter.xml from icon pack resources.
     * Maps "com.pkg/com.pkg.Activity" → "drawable_name"
     */
    private fun parseAppFilter(
        packageName: String,
        resources: Resources
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()

        // Try as XML resource
        val xmlResId = resources.getIdentifier("appfilter", "xml", packageName)
        if (xmlResId != 0) {
            try {
                val parser = resources.getXml(xmlResId)
                parseXmlFilter(parser, result)
                return result
            } catch (_: Exception) {}
        }

        // Try as raw resource (some packs put it in /raw)
        val rawResId = resources.getIdentifier("appfilter", "raw", packageName)
        if (rawResId != 0) {
            try {
                val stream = resources.openRawResource(rawResId)
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = true
                val parser = factory.newPullParser()
                parser.setInput(stream, "UTF-8")
                parseXmlFilter(parser, result)
                stream.close()
                return result
            } catch (_: Exception) {}
        }

        return result
    }

    private fun parseXmlFilter(parser: Any, result: MutableMap<String, String>) {
        when (parser) {
            is XmlPullParser -> {
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                        val component = parser.getAttributeValue(null, "component") ?: ""
                        val drawable = parser.getAttributeValue(null, "drawable") ?: ""
                        if (component.isNotEmpty() && drawable.isNotEmpty()) {
                            // Strip "ComponentInfo{...}" wrapper if present
                            val clean = component
                                .removePrefix("ComponentInfo{")
                                .removeSuffix("}")
                            result[clean] = drawable
                        }
                    }
                    eventType = parser.next()
                }
            }
        }
    }

    override fun clearCache() {
        cachedPackage = null
        cachedFilter = emptyMap()
        cachedResources = null
    }
}
