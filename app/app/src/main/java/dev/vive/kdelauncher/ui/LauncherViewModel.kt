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
import dev.vive.kdelauncher.data.model.AppCategory
import dev.vive.kdelauncher.data.model.AppModel
import dev.vive.kdelauncher.data.model.Profile
import dev.vive.kdelauncher.data.model.ProfileType
import dev.vive.kdelauncher.data.repository.AppRepository
import dev.vive.kdelauncher.ui.components.CategoryConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    val showSettings: Boolean = false,
    val isLoading: Boolean = true,
    val appCounts: Map<AppCategory, Int> = emptyMap(),
    val categoryConfigs: List<CategoryConfig> = emptyList(),
    val visibleCategories: List<AppCategory> = AppCategory.entries,
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
    private val _showSettings = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(true)

    // ── Favorites & work tags — StateFlows so SharedPrefs is NOT read in combine ──
    // Previously getFavorites()/getWorkApps() were called inside the combine lambda
    // on EVERY state emission. Moved to dedicated flows to avoid repeated I/O.
    private val _favorites = MutableStateFlow<Set<String>>(profileManager.getFavorites())
    private val _workApps = MutableStateFlow<Set<String>>(profileManager.getWorkApps())

    // ── Category settings — same: only updated when settings actually change ─
    private val _categoryConfigs = MutableStateFlow(buildCategoryConfigs())
    private val _visibleCategories = MutableStateFlow(buildVisibleCategories())

    // ── Icon packs ───────────────────────────────────────────────────────────
    private val _installedIconPacks = MutableStateFlow<List<IconPackInfo>>(emptyList())
    private val _selectedIconPack = MutableStateFlow(settingsManager.getSelectedIconPack())
    private val _isLoadingIconPacks = MutableStateFlow(false)

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
        _currentProfile, _isDarkTheme, _showSettings, _isLoading
    ) { profile, dark, settings, loading ->
        listOf<Any>(profile, dark, settings, loading)
    }

    /** Icon pack state */
    private val _iconPackState = combine(
        _installedIconPacks, _selectedIconPack, _isLoadingIconPacks
    ) { packs, selected, loading ->
        Triple(packs, selected, loading)
    }

    /** Device/launcher status */
    private val _statusState = combine(
        _isDefaultLauncher, _hasRealWorkProfile, _isWorkProfileLocked
    ) { isDefault, hasWork, workLocked ->
        Triple(isDefault, hasWork, workLocked)
    }

    /** Favorites + work tags (avoids SharedPrefs reads inside the main combine) */
    private val _profileData = combine(_favorites, _workApps) { fav, work -> fav to work }

    // ── Main UI state ────────────────────────────────────────────────────────

    val uiState: StateFlow<LauncherUiState> = combine(
        combine(_allApps, _searchQuery) { apps, q -> apps to q },
        combine(_activeCategory, _iconPackState) { cat, ipState -> cat to ipState },
        combine(_secondaryState, _statusState) { sec, stat -> sec to stat },
        combine(_profileData, combine(_categoryConfigs, _visibleCategories) { c, v -> c to v }) { pd, cv -> pd to cv }
    ) { (allApps, query), (category, ipState), (secondary, status), (profileData, catData) ->

        val profile = secondary[0] as Profile
        val isDark = secondary[1] as Boolean
        val showSettings = secondary[2] as Boolean
        val loading = secondary[3] as Boolean

        @Suppress("UNCHECKED_CAST")
        val installedPacks = ipState.first as List<IconPackInfo>
        val selectedPack = ipState.second as String?
        val loadingPacks = ipState.third as Boolean

        val isDefault = status.first
        val hasWork = status.second
        val workLocked = status.third

        val (favorites, workApps) = profileData
        val (categoryConfigs, visibleCategories) = catData

        // Annotate apps with favorite/work tags — no I/O, pure in-memory ops
        val appsWithMeta = allApps.map { app ->
            app.copy(
                isFavorite = favorites.contains(app.packageName),
                profileTag = if (workApps.contains(app.packageName))
                    ProfileType.WORK else ProfileType.PERSONAL
            )
        }

        val profileFiltered = when (profile.type) {
            ProfileType.WORK -> appsWithMeta.filter { it.profileTag == ProfileType.WORK }
            ProfileType.PERSONAL -> appsWithMeta
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
            showSettings = showSettings,
            isLoading = loading,
            appCounts = counts,
            categoryConfigs = categoryConfigs,
            visibleCategories = visibleCategories,
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
     * Phase 1 — metadata only (instant), makes UI responsive immediately.
     * Phase 2 — icons from LruCache or disk decode.
     */
    private fun loadApps() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Phase 1: names and categories only — nearly instant
                _allApps.value = appRepository.getInstalledAppsMetadata()
                _isLoading.value = false   // UI is usable now

                // Phase 2: icons (from cache if available, decode otherwise)
                _allApps.value = appRepository.getInstalledApps(
                    selectedIconPack = _selectedIconPack.value
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _allApps.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
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
        _selectedIconPack.value = null
        appRepository.clearIconPackCache()
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

    fun launchApp(app: AppModel) {
        val intent = appRepository.getLaunchIntent(app.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            getApplication<Application>().startActivity(intent)
        }
    }

    fun toggleFavorite(app: AppModel) {
        profileManager.toggleFavorite(app.packageName)
        // Update the StateFlow directly — no SharedPrefs read in combine needed
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

    override fun onCleared() {
        super.onCleared()
        try { getApplication<Application>().unregisterReceiver(packageReceiver) }
        catch (_: Exception) {}
    }
}
