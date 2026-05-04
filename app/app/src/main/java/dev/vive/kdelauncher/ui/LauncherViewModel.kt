package dev.vive.kdelauncher.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.vive.kdelauncher.data.IconPackInfo
import dev.vive.kdelauncher.data.IconPackManager
import dev.vive.kdelauncher.data.ProfileManager
import dev.vive.kdelauncher.data.SettingsManager
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
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application)
    private val profileManager = ProfileManager(application)
    private val settingsManager = SettingsManager(application)
    private val iconPackManager = IconPackManager(application)

    private val _allApps = MutableStateFlow<List<AppModel>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _activeCategory = MutableStateFlow(AppCategory.FAVORITES)
    private val _currentProfile = MutableStateFlow(profileManager.getActiveProfile())
    private val _isDarkTheme = MutableStateFlow(settingsManager.isDarkTheme())
    private val _showSettings = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(true)
    private val _settingsVersion = MutableStateFlow(0)
    private val _installedIconPacks = MutableStateFlow<List<IconPackInfo>>(emptyList())
    private val _selectedIconPack = MutableStateFlow(settingsManager.getSelectedIconPack())
    private val _isLoadingIconPacks = MutableStateFlow(false)

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { loadApps() }
    }

    /**
     * Helper flows combined into a secondary flow to stay within combine's 5-arg limit.
     */
    private val _secondaryState = combine(
        _currentProfile, _isDarkTheme, _showSettings, _isLoading, _settingsVersion
    ) { profile, dark, settings, loading, _ ->
        listOf<Any>(profile, dark, settings, loading)
    }

    private val _iconPackState = combine(
        _installedIconPacks, _selectedIconPack, _isLoadingIconPacks
    ) { packs, selected, loading ->
        Triple(packs, selected, loading)
    }

    val uiState: StateFlow<LauncherUiState> = combine(
        _allApps,
        _searchQuery,
        combine(_activeCategory, _iconPackState) { cat, ipState -> cat to ipState },
        _secondaryState
    ) { allApps, query, (category, ipState), secondary ->

        val profile = secondary[0] as Profile
        val isDark = secondary[1] as Boolean
        val showSettings = secondary[2] as Boolean
        val loading = secondary[3] as Boolean

        @Suppress("UNCHECKED_CAST")
        val installedPacks = ipState.first as List<IconPackInfo>
        val selectedPack = ipState.second as String?
        val loadingPacks = ipState.third as Boolean

        val favorites = profileManager.getFavorites()
        val workApps = profileManager.getWorkApps()

        val categoryConfigs = AppCategory.entries.map { cat ->
            CategoryConfig(
                category = cat,
                displayName = settingsManager.getCategoryDisplayName(cat),
                iconName = settingsManager.getCategoryIconName(cat),
                isHidden = settingsManager.getHiddenCategories().contains(cat.name),
            )
        }

        val hiddenSet = settingsManager.getHiddenCategories()
        val visibleCategories = AppCategory.entries.filter { it.name !in hiddenSet }

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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadApps() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
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

    // ── User actions ────────────────────────────────────────────

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun setActiveCategory(category: AppCategory) {
        _activeCategory.value = category
        _searchQuery.value = ""
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
        _settingsVersion.value++
    }

    fun setCategoryIconName(category: AppCategory, iconName: String) {
        settingsManager.setCategoryIconName(category, iconName)
        _settingsVersion.value++
    }

    fun toggleCategoryHidden(category: AppCategory) {
        val hidden = settingsManager.getHiddenCategories().contains(category.name)
        settingsManager.setCategoryHidden(category, !hidden)
        _settingsVersion.value++
    }

    fun resetSettings() {
        settingsManager.resetAll()
        _isDarkTheme.value = true
        _selectedIconPack.value = null
        appRepository.clearIconPackCache()
        _settingsVersion.value++
        loadApps()
    }

    fun setIconPack(packageName: String?) {
        settingsManager.setSelectedIconPack(packageName)
        _selectedIconPack.value = packageName
        appRepository.clearIconPackCache()
        // Reload all app icons with the new pack
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
        _allApps.value = _allApps.value.toList()
    }

    fun toggleWorkApp(app: AppModel) {
        profileManager.toggleWorkApp(app.packageName)
        _allApps.value = _allApps.value.toList()
    }

    override fun onCleared() {
        super.onCleared()
        try { getApplication<Application>().unregisterReceiver(packageReceiver) }
        catch (_: Exception) {}
    }
}
