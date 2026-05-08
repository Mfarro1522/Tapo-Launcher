package dev.vive.kdelauncher.data.model

import android.graphics.Bitmap
import android.os.UserHandle
import androidx.compose.runtime.Immutable

/**
 * Immutable wrapper for Bitmap so Compose treats it as stable.
 * Without this, Compose assumes Bitmap is mutable and recomposes
 * every AppIcon on every state change.
 */
@Immutable
data class AppIconBitmap(val bitmap: Bitmap) {
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(bitmap)
}

/**
 * Represents a single launchable application on the device.
 */
data class AppModel(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: AppIconBitmap? = null,
    val category: String = AppCategory.ALL,
    val isFavorite: Boolean = false,
    val profileTag: ProfileType = ProfileType.PERSONAL,
    val userHandle: UserHandle? = null,
    val versionCode: Long = 0L
)

/**
 * Fixed categories shown by default in the launcher UI.
 * The AI can also create dynamic categories on the fly.
 */
object AppCategory {
    const val FAVORITES = "favorites"
    const val ALL = "all"
    const val SOCIAL = "social"
    const val PRODUCTIVITY = "productivity"
    const val UTILITIES = "utilities"

    /** Categories always visible in settings / tabs */
    val FIXED = listOf(FAVORITES, ALL, SOCIAL, PRODUCTIVITY, UTILITIES)

    /** Human-readable labels (Spanish). Dynamic categories fallback to capitalized ID. */
    fun displayName(id: String): String = when (id) {
        FAVORITES -> "Favoritos"
        ALL -> "Todas"
        SOCIAL -> "Social"
        PRODUCTIVITY -> "Productividad"
        UTILITIES -> "Utilidades"
        "media" -> "Media"
        "creativity" -> "Creatividad"
        "games" -> "Juegos"
        "finance" -> "Finanzas"
        "shopping" -> "Compras"
        "travel" -> "Viajes"
        "browsers" -> "Navegadores"
        "development" -> "Desarrollo"
        else -> id.replaceFirstChar { it.uppercase() }
    }

    /** Default Material icon name for a category ID. */
    fun defaultIcon(id: String): String = when (id) {
        FAVORITES -> "Star"
        ALL -> "GridView"
        SOCIAL -> "Forum"
        PRODUCTIVITY -> "Work"
        UTILITIES -> "Build"
        "media" -> "PlayArrow"
        "creativity" -> "Palette"
        "games" -> "Gamepad"
        "finance" -> "AttachMoney"
        "shopping" -> "ShoppingCart"
        "travel" -> "Map"
        "browsers" -> "Language"
        "development" -> "Code"
        else -> "Folder"
    }

    val availableIcons = listOf(
        "Star", "GridView", "Code", "Palette", "Language",
        "Gamepad", "MusicNote", "Settings", "FolderOpen",
        "Favorite", "Home", "Work", "School", "Rocket",
        "Terminal", "Cloud", "Camera", "ShoppingCart", "Bolt",
        "Diamond", "Brush", "Build", "Explore", "Forum",
        "Headphones", "LocalCafe", "Movie", "Newspaper", "Science"
    )
}

/**
 * Categorization rules based on Android's ApplicationInfo.category
 * and common package name patterns.
 * Returns a String category ID so the AI / heuristics can use any taxonomy.
 */
object AppCategorizer {

    private data class CategoryRule(
        val pattern: String,
        val category: String,
        val baseScore: Int = 10
    )

    private val exactPackageCategories = mapOf(
        "com.android.vending" to "shopping",
        "com.facebook.pages.app" to "social",
        "com.google.android.apps.photos" to "creativity",
        "com.google.android.youtube" to "media",
        "com.google.android.play.games" to "games"
    )

    private val categoryRules = listOf(
        // Development
        CategoryRule("termux", "development", baseScore = 20),
        CategoryRule("terminal", "development"),
        CategoryRule("editor", "development"),
        CategoryRule("ide", "development"),
        CategoryRule("code", "development"),
        CategoryRule("github", "development"),
        CategoryRule("gitlab", "development"),
        CategoryRule("docker", "development"),
        CategoryRule("database", "development"),
        CategoryRule("sql", "development"),
        CategoryRule("dev", "development", baseScore = 6),

        // Graphics / Creativity
        CategoryRule("gallery", "creativity"),
        CategoryRule("photo", "creativity"),
        CategoryRule("photos", "creativity", baseScore = 14),
        CategoryRule("camera", "creativity"),
        CategoryRule("draw", "creativity"),
        CategoryRule("paint", "creativity"),
        CategoryRule("image", "creativity"),
        CategoryRule("sketch", "creativity"),
        CategoryRule("canva", "creativity"),
        CategoryRule("snapseed", "creativity", baseScore = 16),

        // Browsers
        CategoryRule("browser", "browsers"),
        CategoryRule("chrome", "browsers"),
        CategoryRule("firefox", "browsers"),
        CategoryRule("brave", "browsers"),

        // Social / Internet
        CategoryRule("mail", "productivity"),
        CategoryRule("email", "productivity"),
        CategoryRule("gmail", "productivity"),
        CategoryRule("slack", "productivity"),
        CategoryRule("telegram", "social"),
        CategoryRule("whatsapp", "social"),
        CategoryRule("discord", "social"),
        CategoryRule("twitter", "social"),
        CategoryRule("reddit", "social"),
        CategoryRule("instagram", "social"),
        CategoryRule("messenger", "social"),
        CategoryRule("signal", "social"),
        CategoryRule("facebook", "social"),
        CategoryRule("pages", "social"),

        // Shopping
        CategoryRule("store", "shopping"),
        CategoryRule("vending", "shopping", baseScore = 14),

        // Games
        CategoryRule("game", "games", baseScore = 14),
        CategoryRule("games", "games", baseScore = 16),
        CategoryRule("play", "games", baseScore = 5),

        // Media
        CategoryRule("music", "media", baseScore = 14),
        CategoryRule("spotify", "media", baseScore = 16),
        CategoryRule("video", "media", baseScore = 14),
        CategoryRule("youtube", "media", baseScore = 16),
        CategoryRule("vlc", "media", baseScore = 16),
        CategoryRule("podcast", "media", baseScore = 14),
        CategoryRule("player", "media", baseScore = 16),
        CategoryRule("audio", "media", baseScore = 14),
        CategoryRule("netflix", "media", baseScore = 16),
        CategoryRule("tiktok", "media", baseScore = 14),

        // System / Utilities
        CategoryRule("settings", "utilities", baseScore = 18),
        CategoryRule("monitor", "utilities"),
        CategoryRule("filemanager", "utilities"),
        CategoryRule("files", "utilities"),
        CategoryRule("manager", "utilities", baseScore = 8),
        CategoryRule("updater", "utilities"),
        CategoryRule("calculator", "utilities", baseScore = 14),
        CategoryRule("calc", "utilities"),
        CategoryRule("clock", "utilities"),
        CategoryRule("alarm", "utilities"),
        CategoryRule("calendar", "productivity"),
        CategoryRule("notes", "productivity"),
        CategoryRule("weather", "utilities"),
        CategoryRule("maps", "travel", baseScore = 14),
        CategoryRule("translate", "utilities"),
        CategoryRule("compass", "travel"),
        CategoryRule("flashlight", "utilities"),
        CategoryRule("recorder", "utilities"),
        CategoryRule("contacts", "social"),
        CategoryRule("phone", "social"),
        CategoryRule("dialer", "social"),
        CategoryRule("messages", "social"),
        CategoryRule("sms", "social")
    )

    /**
     * Categorize an app based on its package name and Android category info.
     */
    fun categorize(packageName: String, androidCategory: Int): String {
        val lowerPkg = packageName.lowercase()
        exactPackageCategories[lowerPkg]?.let { return it }

        val bestMatch = categoryRules
            .mapNotNull { rule ->
                scoreRule(lowerPkg, rule)?.let { rule.category to it }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, scores) -> scores.maxOrNull() ?: 0 }
            .maxByOrNull { it.value }

        if (bestMatch != null && bestMatch.value > 0) {
            return bestMatch.key
        }

        // Fall back to Android's built-in category
        return when (androidCategory) {
            android.content.pm.ApplicationInfo.CATEGORY_GAME -> "games"
            android.content.pm.ApplicationInfo.CATEGORY_AUDIO -> "media"
            android.content.pm.ApplicationInfo.CATEGORY_VIDEO -> "media"
            android.content.pm.ApplicationInfo.CATEGORY_IMAGE -> "creativity"
            android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> "social"
            android.content.pm.ApplicationInfo.CATEGORY_NEWS -> "browsers"
            android.content.pm.ApplicationInfo.CATEGORY_MAPS -> "travel"
            android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> "productivity"
            else -> AppCategory.ALL
        }
    }

    private fun scoreRule(packageName: String, rule: CategoryRule): Int? {
        if (!packageName.contains(rule.pattern)) return null

        val exactTokenMatch = packageName
            .split('.', '_', '-', '/')
            .any { token -> token == rule.pattern }

        val exactTokenBonus = if (exactTokenMatch) 20 else 0
        val substringBonus = rule.pattern.length
        return rule.baseScore + exactTokenBonus + substringBonus
    }
}
