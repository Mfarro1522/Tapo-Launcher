package dev.vive.kdelauncher.ui

import android.app.Application
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.vive.kdelauncher.SetDefaultLauncherActivity
import dev.vive.kdelauncher.data.IconPackInfo
import dev.vive.kdelauncher.data.IconPackManager
import dev.vive.kdelauncher.data.ProfileManager
import dev.vive.kdelauncher.data.SettingsManager
import dev.vive.kdelauncher.data.WorkProfileManager
import dev.vive.kdelauncher.data.model.AppCategorizer
import dev.vive.kdelauncher.data.model.AppCategory
import dev.vive.kdelauncher.data.model.AppModel
import dev.vive.kdelauncher.data.model.Profile
import dev.vive.kdelauncher.data.model.ProfileType
import dev.vive.kdelauncher.data.repository.AppRepository
import dev.vive.kdelauncher.ui.components.CategoryConfig
import dev.vive.kdelauncher.ui.components.IconSize
import dev.vive.kdelauncher.ui.components.parseIconSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Complete UI state for the launcher screen.
 */
data class LauncherUiState(
    val allApps: List<AppModel> = emptyList(),
    val filteredApps: List<AppModel> = emptyList(),
    val searchQuery: String = "",
    val activeCategory: AppCategory = AppCategory.FAVORITES,
    val currentProfile: Profile = Profile.Personal,
    val isDarkTheme: Boolean = true,
    val showAppLabels: Boolean = true,
    val showSettings: Boolean = false,
    val isLoading: Boolean = true,
    val appCounts: Map<AppCategory, Int> = emptyMap(),
    val categoryConfigs: List<CategoryConfig> = emptyList(),
    val visibleCategories: List<AppCategory> = AppCategory.entries,
    // Icon settings
    val iconSize: IconSize = IconSize.MEDIUM,
    val showIconBackground: Boolean = true,
    val gridColumns: Int = 3,
    // Icon packs
    val installedIconPacks: List<IconPackInfo> = emptyList(),
    val selectedIconPack: String? = null,
    val isLoadingIconPacks: Boolean = false,
    // Launcher & Work Profile status
    val isDefaultLauncher: Boolean = true,
    val hasRealWorkProfile: Boolean = false,
    val isWorkProfileLocked: Boolean = false,
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application)
    private val profileManager = ProfileManager(application)
    private val settingsManager = SettingsManager(application)
    private val iconPackManager = IconPackManager(application)
    private val workProfileManager = WorkProfileManager(application)

    // ── Core app list ────────────────────────────────────────────────────────
    private val _allApps = MutableStateFlow<List<AppModel>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _activeCategory = MutableStateFlow(AppCategory.FAVORITES)

    // ── Profile & theme ──────────────────────────────────────────────────────
    private val _currentProfile = MutableStateFlow(profileManager.getActiveProfile())
    private val _isDarkTheme = MutableStateFlow(settingsManager.isDarkTheme())
    private val _showAppLabels = MutableStateFlow(settingsManager.isShowAppLabels())
    private val _showSettings = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(true)
    private val _homeResetCounter = MutableStateFlow(0)

    // ── Favorites & work tags — StateFlows so SharedPrefs is NOT read in combine ──
    private val _favorites = MutableStateFlow<Set<String>>(profileManager.getFavorites())
    private val _workApps = MutableStateFlow<Set<String>>(profileManager.getWorkApps())

    // ── Category settings — same: only updated when settings actually change ─
    private val _categoryConfigs = MutableStateFlow(buildCategoryConfigs())
    private val _visibleCategories = MutableStateFlow(buildVisibleCategories())
    private val _categoryOverrides =
        MutableStateFlow(settingsManager.getCategoryOverrides())

    // ── Icon packs ───────────────────────────────────────────────────────────
    private val _installedIconPacks = MutableStateFlow<List<IconPackInfo>>(emptyList())
    private val _selectedIconPack = MutableStateFlow(settingsManager.getSelectedIconPack())
    private val _isLoadingIconPacks = MutableStateFlow(false)

    // ── Icon settings ────────────────────────────────────────────────────────
    private val _iconSize = MutableStateFlow(parseIconSize(settingsManager.getIconSize()))
    private val _showIconBackground = MutableStateFlow(settingsManager.isShowIconBackground())
    private val _gridColumns = MutableStateFlow(settingsManager.getGridColumns())

    // ── System status ────────────────────────────────────────────────────────
    private val _isDefaultLauncher = MutableStateFlow(true)
    private val _hasRealWorkProfile = MutableStateFlow(false)
    private val _isWorkProfileLocked = MutableStateFlow(false)

    // ── Package change receiver ──────────────────────────────────────────────
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { loadApps() }
    }

    // ── Sub-combines to stay within 5-arg limit ──────────────────────────────

    /** Profile + theme + UI toggles + loading state */
    private val _secondaryState = combine(
        _currentProfile, _isDarkTheme, _showAppLabels, _showSettings, _isLoading
    ) { profile, dark, showLabels, settings, loading ->
        listOf<Any>(profile, dark, showLabels, settings, loading)
    }

    /** Icon pack state */
    private val _iconPackState = combine(
        _installedIconPacks, _selectedIconPack, _isLoadingIconPacks
    ) { packs, selected, loading ->
        Triple(packs, selected, loading)
    }

    /** Icon settings (size, background, columns) */
    private val _iconSettingsState = combine(
        _iconSize, _showIconBackground, _gridColumns
    ) { size, showBg, cols ->
        Triple(size, showBg, cols)
    }

    /** Device/launcher status */
    private val _statusState = combine(
        _isDefaultLauncher, _hasRealWorkProfile, _isWorkProfileLocked
    ) { isDefault, hasWork, workLocked ->
        Triple(isDefault, hasWork, workLocked)
    }

    /** Favorites + work tags (avoids SharedPrefs reads inside the main combine) */
    private val _profileData = combine(_favorites, _workApps) { fav, work -> fav to work }

    /** Category config + visibility + per-app overrides */
    private val _categoryState = combine(
        _categoryConfigs, _visibleCategories, _categoryOverrides
    ) { configs, visible, overrides ->
        Triple(configs, visible, overrides)
    }

    // ── Main UI state ────────────────────────────────────────────────────────

    val uiState: StateFlow<LauncherUiState> = combine(
        combine(_allApps, _searchQuery) { apps, q -> apps to q },
        combine(_activeCategory, _iconPackState, _iconSettingsState) { cat, ipState, iconSettings -> Triple(cat, ipState, iconSettings) },
        combine(_secondaryState, _statusState) { sec, stat -> sec to stat },
        combine(_profileData, _categoryState) { pd, cs -> pd to cs }
    ) { (allApps, query), (category, ipState, iconSettingsData), (secondary, status), (profileData, catData) ->

        val profile = secondary[0] as Profile
        val isDark = secondary[1] as Boolean
        val showLabels = secondary[2] as Boolean
        val showSettings = secondary[3] as Boolean
        val loading = secondary[4] as Boolean

        @Suppress("UNCHECKED_CAST")
        val installedPacks = ipState.first as List<IconPackInfo>
        val selectedPack = ipState.second as String?
        val loadingPacks = ipState.third as Boolean

        val iconSize = iconSettingsData.first as IconSize
        val showIconBackground = iconSettingsData.second as Boolean
        val gridColumns = iconSettingsData.third as Int

        val isDefault = status.first
        val hasWork = status.second
        val workLocked = status.third

        val (favorites, workApps) = profileData
        val (categoryConfigs, visibleCategories, categoryOverrides) = catData

        // Annotate apps with favorite/work tags — no I/O, pure in-memory ops
        val appsWithMeta = allApps.map { app ->
            val isWorkApp = if (hasWork) app.userHandle != null
            else workApps.contains(app.packageName)
            val overrideKey = categoryOverrideKey(app, isWorkApp)
            val overrideCategory = categoryOverrides[overrideKey]
            app.copy(
                isFavorite = favorites.contains(app.packageName),
                profileTag = if (isWorkApp)
                    ProfileType.WORK else ProfileType.PERSONAL,
                category = overrideCategory ?: app.category
            )
        }

        val profileFiltered = when (profile.type) {
            ProfileType.WORK -> appsWithMeta.filter { it.profileTag == ProfileType.WORK }
            ProfileType.PERSONAL -> appsWithMeta.filter { it.profileTag == ProfileType.PERSONAL }
        }

        val filtered = if (query.isNotBlank()) {
            appsWithMeta.filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
        } else when (category) {
            AppCategory.FAVORITES -> profileFiltered.filter { it.isFavorite }
            AppCategory.ALL -> profileFiltered
            else -> profileFiltered.filter { it.category == category }
        }

        val counts = AppCategory.entries.associateWith { cat ->
            when (cat) {
                AppCategory.FAVORITES -> profileFiltered.count { it.isFavorite }
                AppCategory.ALL -> profileFiltered.size
                else -> profileFiltered.count { it.category == cat }
            }
        }

        LauncherUiState(
            allApps = appsWithMeta,
            filteredApps = filtered,
            searchQuery = query,
            activeCategory = category,
            currentProfile = profile,
            isDarkTheme = isDark,
            showAppLabels = showLabels,
            showSettings = showSettings,
            isLoading = loading,
            appCounts = counts,
            categoryConfigs = categoryConfigs,
            visibleCategories = visibleCategories,
            iconSize = iconSize,
            showIconBackground = showIconBackground,
            gridColumns = gridColumns,
            installedIconPacks = installedPacks,
            selectedIconPack = selectedPack,
            isLoadingIconPacks = loadingPacks,
            isDefaultLauncher = isDefault,
            hasRealWorkProfile = hasWork,
            isWorkProfileLocked = workLocked,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LauncherUiState()
    )

    val homeResetCounter: StateFlow<Int> = _homeResetCounter

    init {
        try {
            loadApps()
            loadIconPacks()
            registerPackageReceiver()
            checkLauncherStatus()
            checkWorkProfile()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── Private loaders ──────────────────────────────────────────────────────

    /**
     * Two-phase loading:
     * 1. Show metadata-only apps → UI is instantly usable with names/categories
     * 2. Batch-load all icons in parallel → single state update when done
     *
     * This avoids the previous approach of 150+ separate LaunchedEffect calls,
     * each triggering a full state recomputation for a single icon.
     */
    private fun loadApps() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Phase 1: metadata only — near instant
                val personalMeta = appRepository.getInstalledAppsMetadata()
                val workMeta = withContext(Dispatchers.IO) {
                    if (!workProfileManager.hasRealWorkProfile()) emptyList()
                    else workProfileManager.getWorkProfileApps(loadIcons = false).map { app ->
                        AppModel(
                            packageName = app.packageName,
                            activityName = app.activityName,
                            label = app.label,
                            iconBitmap = null,
                            category = AppCategorizer.categorize(app.packageName, app.androidCategory),
                            userHandle = app.userHandle
                        )
                    }
                }
                val metadataApps = mergeApps(personalMeta, workMeta)

                // Show apps immediately (no icons yet but names visible)
                _allApps.value = metadataApps
                _isLoading.value = false   // UI usable NOW

                // Phase 2: batch-load ALL icons in parallel → single emission
                val selectedPack = _selectedIconPack.value
                val iconsByKey = withContext(Dispatchers.IO) {
                    val personalDeferreds = personalMeta.map { app ->
                        async {
                            val key = iconKey(app)
                            val bitmap = appRepository.getAppIcon(
                                packageName = app.packageName,
                                activityName = app.activityName,
                                selectedIconPack = selectedPack
                            )
                            key to bitmap
                        }
                    }
                    // Load work profile icons too if they exist
                    val workDeferreds = if (workMeta.isNotEmpty()) {
                        workMeta.map { app ->
                            async {
                                val key = iconKey(app)
                                val bitmap = if (app.userHandle != null) {
                                    workProfileManager.loadWorkAppIcon(
                                        packageName = app.packageName,
                                        activityName = app.activityName,
                                        userHandle = app.userHandle
                                    )
                                } else null
                                key to bitmap
                            }
                        }
                    } else emptyList()

                    (personalDeferreds + workDeferreds).awaitAll().toMap()
                }

                // Single emission with all icons populated
                _allApps.value = metadataApps.map { app ->
                    val key = iconKey(app)
                    val bitmap = iconsByKey[key]
                    if (bitmap != null) app.copy(iconBitmap = bitmap) else app
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _allApps.value = emptyList()
            } finally {
                // Ensure loading is false even if something failed mid-way
                _isLoading.value = false
            }
        }
    }

    private fun mergeApps(
        personalApps: List<AppModel>,
        workApps: List<AppModel>
    ): List<AppModel> {
        return (personalApps + workApps).sortedBy { it.label.lowercase() }
    }

    private fun loadIconPacks() {
        viewModelScope.launch {
            try {
                _isLoadingIconPacks.value = true
                _installedIconPacks.value = iconPackManager.getInstalledPacks()
            } catch (e: Exception) {
                e.printStackTrace()
                _installedIconPacks.value = emptyList()
            } finally {
                _isLoadingIconPacks.value = false
            }
        }
    }

    private fun registerPackageReceiver() {
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addDataScheme("package")
            }
            ContextCompat.registerReceiver(
                getApplication<Application>(),
                packageReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Check if we are the current default home/launcher app. */
    private fun checkLauncherStatus() {
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val isDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val roleManager = app.getSystemService(Context.ROLE_SERVICE) as RoleManager
                    roleManager.isRoleHeld(RoleManager.ROLE_HOME)
                } else {
                    val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                        addCategory(android.content.Intent.CATEGORY_HOME)
                    }
                    val resolveInfo = app.packageManager.resolveActivity(
                        intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
                    )
                    resolveInfo?.activityInfo?.packageName == app.packageName
                }
                _isDefaultLauncher.value = isDefault
            } catch (e: Exception) {
                _isDefaultLauncher.value = true
            }
        }
    }

    /** Detect real Android Work Profile via UserManager. */
    private fun checkWorkProfile() {
        viewModelScope.launch {
            try {
                _hasRealWorkProfile.value = workProfileManager.hasRealWorkProfile()
                if (_hasRealWorkProfile.value) {
                    _isWorkProfileLocked.value = workProfileManager.isWorkProfileLocked()
                }
            } catch (e: Exception) {
                _hasRealWorkProfile.value = false
            }
        }
    }

    /**
     * Builds CategoryConfig list from SettingsManager.
     * Called once at init and again when settings change — NOT on every state emission.
     */
    private fun buildCategoryConfigs(): List<CategoryConfig> =
        AppCategory.entries.map { cat ->
            CategoryConfig(
                category = cat,
                displayName = settingsManager.getCategoryDisplayName(cat),
                iconName = settingsManager.getCategoryIconName(cat),
                isHidden = settingsManager.getHiddenCategories().contains(cat.name),
            )
        }

    private fun buildVisibleCategories(): List<AppCategory> {
        val hiddenSet = settingsManager.getHiddenCategories()
        return AppCategory.entries.filter { it.name !in hiddenSet }
    }

    // ── User actions ─────────────────────────────────────────────────────────

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun setActiveCategory(category: AppCategory) {
        _activeCategory.value = category
        _searchQuery.value = ""
    }

    /**
     * Reset the launcher to its initial home state:
     * FAVORITES category, no search, settings closed.
     */
    fun resetToHome() {
        _searchQuery.value = ""
        _activeCategory.value = AppCategory.FAVORITES
        _showSettings.value = false
        _homeResetCounter.value = _homeResetCounter.value + 1
    }

    fun handleBackPress(): Boolean {
        return if (_showSettings.value) {
            _showSettings.value = false
            true
        } else {
            false
        }
    }

    fun toggleProfile() {
        val newProfile = if (_currentProfile.value.type == ProfileType.PERSONAL)
            Profile.Work else Profile.Personal
        _currentProfile.value = newProfile
        profileManager.setActiveProfile(newProfile)
    }

    fun toggleSettings() { _showSettings.value = !_showSettings.value }

    fun toggleTheme() {
        val newVal = !_isDarkTheme.value
        _isDarkTheme.value = newVal
        settingsManager.setDarkTheme(newVal)
    }

    fun setShowAppLabels(show: Boolean) {
        _showAppLabels.value = show
        settingsManager.setShowAppLabels(show)
    }

    fun setIconSize(size: IconSize) {
        _iconSize.value = size
        settingsManager.setIconSize(size.name.lowercase())
    }

    fun setShowIconBackground(show: Boolean) {
        _showIconBackground.value = show
        settingsManager.setShowIconBackground(show)
    }

    fun setGridColumns(columns: Int) {
        _gridColumns.value = columns
        settingsManager.setGridColumns(columns)
    }

    fun setCategoryDisplayName(category: AppCategory, name: String) {
        settingsManager.setCategoryDisplayName(category, name)
        _categoryConfigs.value = buildCategoryConfigs()
    }

    fun setCategoryIconName(category: AppCategory, iconName: String) {
        settingsManager.setCategoryIconName(category, iconName)
        _categoryConfigs.value = buildCategoryConfigs()
    }

    fun toggleCategoryHidden(category: AppCategory) {
        val hidden = settingsManager.getHiddenCategories().contains(category.name)
        settingsManager.setCategoryHidden(category, !hidden)
        _categoryConfigs.value = buildCategoryConfigs()
        _visibleCategories.value = buildVisibleCategories()
    }

    fun resetSettings() {
        settingsManager.resetAll()
        _isDarkTheme.value = true
        _showAppLabels.value = settingsManager.isShowAppLabels()
        _selectedIconPack.value = null
        appRepository.clearIconPackCache()
        _categoryOverrides.value = emptyMap()
        _categoryConfigs.value = buildCategoryConfigs()
        _visibleCategories.value = buildVisibleCategories()
        loadApps()
    }

    fun setIconPack(packageName: String?) {
        settingsManager.setSelectedIconPack(packageName)
        _selectedIconPack.value = packageName
        appRepository.clearIconPackCache()
        loadApps()
    }

    fun setCategoryOverride(app: AppModel, category: AppCategory) {
        val isWorkApp = app.userHandle != null || app.profileTag == ProfileType.WORK
        val key = categoryOverrideKey(app, isWorkApp)
        settingsManager.setCategoryOverride(key, category)
        _categoryOverrides.value = settingsManager.getCategoryOverrides()
    }

    fun clearCategoryOverride(app: AppModel) {
        val isWorkApp = app.userHandle != null || app.profileTag == ProfileType.WORK
        val key = categoryOverrideKey(app, isWorkApp)
        settingsManager.clearCategoryOverride(key)
        _categoryOverrides.value = settingsManager.getCategoryOverrides()
    }

    fun launchApp(app: AppModel) {
        val userHandle = app.userHandle
        if (userHandle != null) {
            workProfileManager.launchWorkApp(
                packageName = app.packageName,
                activityName = app.activityName,
                userHandle = userHandle
            )
            return
        }

        val intent = appRepository.getLaunchIntent(app.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            getApplication<Application>().startActivity(intent)
        }
    }

    fun toggleFavorite(app: AppModel) {
        profileManager.toggleFavorite(app.packageName)
        _favorites.value = profileManager.getFavorites()
    }

    fun toggleWorkApp(app: AppModel) {
        profileManager.toggleWorkApp(app.packageName)
        _workApps.value = profileManager.getWorkApps()
    }

    fun openSetDefaultLauncherScreen() {
        try {
            val app = getApplication<Application>()
            val intent = Intent(app, SetDefaultLauncherActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Refresh work profile and launcher status (called from onResume). */
    fun refreshStatus() {
        checkLauncherStatus()
        checkWorkProfile()
    }

    private fun iconKey(app: AppModel): String {
        val handleId = app.userHandle?.hashCode() ?: 0
        return "${app.packageName}|${app.activityName}|$handleId"
    }

    private fun categoryOverrideKey(app: AppModel, isWorkApp: Boolean): String {
        val scope = if (isWorkApp) "work" else "personal"
        return "$scope:${app.packageName}"
    }

    override fun onCleared() {
        super.onCleared()
        try { getApplication<Application>().unregisterReceiver(packageReceiver) }
        catch (_: Exception) {}
    }
}