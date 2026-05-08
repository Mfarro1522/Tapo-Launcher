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

internal data class LauncherAiInput(
    val labsEnabled: Boolean,
    val aiProvider: dev.vive.kdelauncher.data.model.AiProviderType,
    val aiModel: String,
    val aiConnectionState: AiConnectionState,
    val organizationState: OrganizationState
)

internal data class LauncherUiProjectionInput(
    val app: LauncherAppInput,
    val settingsDisplay: LauncherSettingsDisplayInput,
    val settingsIcon: LauncherSettingsIconInput,
    val profile: LauncherProfileInput,
    val category: LauncherCategoryInput,
    val system: LauncherSystemInput,
    val ai: LauncherAiInput
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

    fun mapAppContent(input: LauncherAppContentInput): LauncherAppContentState {
        val appsWithMeta = input.app.allApps.map { appModel ->
            val isWorkApp = isWorkApp(appModel, input.system.hasRealWorkProfile, input.profile.workApps)
            val overrideCategory = input.category.categoryOverrides[categoryOverrideKey(appModel, isWorkApp)]
            val favorites = if (isWorkApp) input.profile.workFavorites else input.profile.personalFavorites
            appModel.copy(
                isFavorite = favorites.contains(appModel.packageName),
                profileTag = if (isWorkApp) ProfileType.WORK else ProfileType.PERSONAL,
                category = overrideCategory ?: appModel.category
            )
        }

        val profileFiltered = filterByProfile(appsWithMeta, input.profile.currentProfile.type)
        val filtered = filterVisibleApps(
            apps = appsWithMeta,
            profileFilteredApps = profileFiltered,
            searchQuery = input.app.searchQuery,
            activeCategory = input.app.activeCategory
        )
        val counts = buildCategoryCounts(profileFiltered)

        return LauncherAppContentState(
            allApps = appsWithMeta,
            filteredApps = filtered,
            appCounts = counts
        )
    }

    fun map(input: LauncherUiProjectionInput, appContent: LauncherAppContentState, tourState: dev.vive.kdelauncher.ui.tour.TourState): LauncherUiState {
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
            isWorkProfileLocked = input.system.isWorkProfileLocked,
            labsEnabled = input.ai.labsEnabled,
            aiProvider = input.ai.aiProvider,
            aiConnectionState = input.ai.aiConnectionState,
            aiModel = input.ai.aiModel,
            organizationState = input.ai.organizationState,
            tourState = tourState
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

    private fun filterVisibleApps(
        apps: List<AppModel>,
        profileFilteredApps: List<AppModel>,
        searchQuery: String,
        activeCategory: String
    ): List<AppModel> {
        if (searchQuery.isNotBlank()) {
            return apps.filter {
                it.label.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }

        return when (activeCategory) {
            AppCategory.FAVORITES -> profileFilteredApps.filter { it.isFavorite }
            AppCategory.ALL -> profileFilteredApps
            else -> profileFilteredApps.filter { it.category == activeCategory }
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
