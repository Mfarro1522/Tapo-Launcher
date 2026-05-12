package dev.vive.kdelauncher.ui

import dev.vive.kdelauncher.data.IconPackInfo
import dev.vive.kdelauncher.data.model.AppCategory
import dev.vive.kdelauncher.data.model.AppModel
import dev.vive.kdelauncher.data.model.Profile
import dev.vive.kdelauncher.data.model.ProfileType
import dev.vive.kdelauncher.ui.components.CategoryConfig
import dev.vive.kdelauncher.ui.components.IconSize

import dev.vive.kdelauncher.data.model.ColorTheme

internal data class LauncherAppInput(
    val allApps: List<AppModel>,
    val searchQuery: String,
    val activeCategory: String
)

internal data class LauncherSettingsDisplayInput(
    val darkTheme: Boolean,
    val colorTheme: ColorTheme,
    val showAppLabels: Boolean,
    val showSettings: Boolean,
    val isLoading: Boolean,
    val iconSize: IconSize
)

internal data class LauncherSettingsIconInput(
    val showIconBackground: Boolean,
    val gridColumns: Int,
    val selectedIconPack: String?,
    val isLoadingIconPacks: Boolean,
    val installedIconPacks: List<IconPackInfo>
)

internal data class LauncherProfileInput(
    val currentProfile: Profile,
    val personalFavorites: Set<String>,
    val workFavorites: Set<String>,
    val workApps: Set<String>
)

internal data class LauncherCategoryInput(
    val categoryConfigs: List<CategoryConfig>,
    val visibleCategories: List<String>,
    val categoryOverrides: Map<String, String>
)

internal data class LauncherSystemInput(
    val isDefaultLauncher: Boolean,
    val hasRealWorkProfile: Boolean,
    val isWorkProfileLocked: Boolean
)

internal data class LauncherUiProjectionInput(
    val app: LauncherAppInput,
    val settingsDisplay: LauncherSettingsDisplayInput,
    val settingsIcon: LauncherSettingsIconInput,
    val profile: LauncherProfileInput,
    val category: LauncherCategoryInput,
    val system: LauncherSystemInput
)

internal data class LauncherAppContentInput(
    val app: LauncherAppInput,
    val profile: LauncherProfileInput,
    val category: LauncherCategoryInput,
    val system: LauncherSystemInput
)

internal data class LauncherAppContentState(
    val allApps: List<AppModel>,
    val filteredApps: List<AppModel>,
    val appCounts: Map<String, Int>
)

internal object LauncherUiStateMapper {

    fun mapAppsWithMeta(
        apps: List<AppModel>,
        profile: LauncherProfileInput,
        categoryOverrides: Map<String, String>,
        hasRealWorkProfile: Boolean,
        workApps: Set<String>
    ): List<AppModel> {
        return apps.map { appModel ->
            val isWorkApp = isWorkApp(appModel, hasRealWorkProfile, workApps)
            val overrideCategory = categoryOverrides[categoryOverrideKey(appModel, isWorkApp)]
            val favorites = if (isWorkApp) profile.workFavorites else profile.personalFavorites
            appModel.copy(
                isFavorite = favorites.contains(appModel.packageName),
                profileTag = if (isWorkApp) ProfileType.WORK else ProfileType.PERSONAL,
                category = overrideCategory ?: appModel.category
            )
        }
    }

    fun mapAppContentFiltered(
        appsWithMeta: List<AppModel>,
        searchQuery: String,
        activeCategory: String,
        currentProfile: Profile
    ): LauncherAppContentState {
        val profileFiltered = filterByProfile(appsWithMeta, currentProfile.type)
        val filtered = if (searchQuery.isNotBlank()) {
            appsWithMeta.filter {
                it.label.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
            }
        } else when (activeCategory) {
            AppCategory.FAVORITES -> profileFiltered.filter { it.isFavorite }
            AppCategory.ALL -> profileFiltered
            else -> profileFiltered.filter { it.category == activeCategory }
        }

        // Guard against duplicate (packageName, profileTag) pairs that would crash
        // LazyGrid with "Key was already used". MIUI's PackageManager can return the
        // same package multiple times in certain edge cases (e.g. Chrome with multiple
        // launcher activities, race between persistent cache and fresh load, or MIUI's
        // APK overlay system adding phantom entries). distinctBy is O(n) and prevents
        // the fatal IllegalArgumentException in SubcomposeLayout.
        val deduplicatedFiltered = filtered.distinctBy { "${it.packageName}:${it.profileTag.name}" }
        val counts = buildCategoryCounts(profileFiltered)

        return LauncherAppContentState(
            allApps = appsWithMeta,
            filteredApps = deduplicatedFiltered,
            appCounts = counts
        )
    }

    // Legacy overload kept for compatibility
    fun mapAppContent(input: LauncherAppContentInput): LauncherAppContentState {
        val appsWithMeta = mapAppsWithMeta(
            apps = input.app.allApps,
            profile = input.profile,
            categoryOverrides = input.category.categoryOverrides,
            hasRealWorkProfile = input.system.hasRealWorkProfile,
            workApps = input.profile.workApps
        )
        return mapAppContentFiltered(
            appsWithMeta = appsWithMeta,
            searchQuery = input.app.searchQuery,
            activeCategory = input.app.activeCategory,
            currentProfile = input.profile.currentProfile
        )
    }

    fun map(
        input: LauncherUiProjectionInput,
        appContent: LauncherAppContentState
    ): LauncherUiState {
        return LauncherUiState(
            allApps = appContent.allApps,
            filteredApps = appContent.filteredApps,
            searchQuery = input.app.searchQuery,
            activeCategory = input.app.activeCategory,
            currentProfile = input.profile.currentProfile,
            isDarkTheme = input.settingsDisplay.darkTheme,
            colorTheme = input.settingsDisplay.colorTheme,
            showAppLabels = input.settingsDisplay.showAppLabels,
            showSettings = input.settingsDisplay.showSettings,
            isLoading = input.settingsDisplay.isLoading,
            appCounts = appContent.appCounts,
            categoryConfigs = input.category.categoryConfigs,
            visibleCategories = input.category.visibleCategories,
            iconSize = input.settingsDisplay.iconSize,
            showIconBackground = input.settingsIcon.showIconBackground,
            gridColumns = input.settingsIcon.gridColumns,
            installedIconPacks = input.settingsIcon.installedIconPacks,
            selectedIconPack = input.settingsIcon.selectedIconPack,
            isLoadingIconPacks = input.settingsIcon.isLoadingIconPacks,
            isDefaultLauncher = input.system.isDefaultLauncher,
            hasRealWorkProfile = input.system.hasRealWorkProfile,
            isWorkProfileLocked = input.system.isWorkProfileLocked
        )
    }

    private fun isWorkApp(
        app: AppModel,
        hasRealWorkProfile: Boolean,
        workApps: Set<String>
    ): Boolean {
        return if (hasRealWorkProfile) app.userHandle != null else workApps.contains(app.packageName)
    }

    private fun filterByProfile(apps: List<AppModel>, profileType: ProfileType): List<AppModel> {
        return when (profileType) {
            ProfileType.WORK -> apps.filter { it.profileTag == ProfileType.WORK }
            ProfileType.PERSONAL -> apps.filter { it.profileTag == ProfileType.PERSONAL }
        }
    }

    private fun buildCategoryCounts(profileFilteredApps: List<AppModel>): Map<String, Int> {
        val fixed = AppCategory.FIXED.associateWith { category ->
            when (category) {
                AppCategory.FAVORITES -> profileFilteredApps.count { it.isFavorite }
                AppCategory.ALL -> profileFilteredApps.size
                else -> profileFilteredApps.count { it.category == category }
            }
        }
        val dynamic = profileFilteredApps
            .filter { it.category !in AppCategory.FIXED }
            .groupingBy { it.category }
            .eachCount()
        return fixed + dynamic
    }
}

internal fun categoryOverrideKey(app: AppModel, isWorkApp: Boolean): String {
    val scope = if (isWorkApp) "work" else "personal"
    return "$scope:${app.packageName}"
}
