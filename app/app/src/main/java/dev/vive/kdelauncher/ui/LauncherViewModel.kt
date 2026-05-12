package dev.vive.kdelauncher.ui

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.vive.kdelauncher.AppContainer
import dev.vive.kdelauncher.data.model.AppCategory
import dev.vive.kdelauncher.data.model.AppIconBitmap
import dev.vive.kdelauncher.data.model.AppModel
import dev.vive.kdelauncher.data.model.Profile
import dev.vive.kdelauncher.data.model.ProfileType
import dev.vive.kdelauncher.data.repository.IconDiskCache
import dev.vive.kdelauncher.domain.repository.AppRepository
import dev.vive.kdelauncher.domain.repository.ProfileManager
import dev.vive.kdelauncher.domain.repository.SettingsManager
import dev.vive.kdelauncher.domain.repository.WorkProfileManager
import dev.vive.kdelauncher.domain.usecase.GetSystemStatusUseCase
import dev.vive.kdelauncher.domain.usecase.LaunchAppUseCase
import dev.vive.kdelauncher.domain.usecase.LoadAppsUseCase
import dev.vive.kdelauncher.domain.usecase.LoadIconPacksUseCase
import dev.vive.kdelauncher.domain.usecase.OpenAppInfoUseCase
import dev.vive.kdelauncher.domain.usecase.OpenSetDefaultLauncherUseCase
import dev.vive.kdelauncher.domain.usecase.SetCategoryOverrideUseCase
import dev.vive.kdelauncher.domain.usecase.ToggleFavoriteUseCase
import dev.vive.kdelauncher.domain.usecase.ToggleWorkAppUseCase
import dev.vive.kdelauncher.domain.usecase.UninstallAppUseCase
import dev.vive.kdelauncher.domain.usecase.CheckProductTourStatusUseCase
import dev.vive.kdelauncher.domain.usecase.DismissProductTourUseCase
import dev.vive.kdelauncher.ui.tour.TourState
import dev.vive.kdelauncher.ui.tour.TourStep
import dev.vive.kdelauncher.service.PackageChangeReceiver
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class OrganizationSuggestionState {
    object Idle : OrganizationSuggestionState()
    object Loading : OrganizationSuggestionState()
    data class Preview(val result: dev.vive.kdelauncher.domain.usecase.SuggestAppOrganizationUseCase.SuggestionResult) : OrganizationSuggestionState()
    object Applied : OrganizationSuggestionState()
}

/**
 * Complete UI state for the launcher screen.
 */
data class LauncherUiState(
    val allApps: List<AppModel> = emptyList(),
    val filteredApps: List<AppModel> = emptyList(),
    val searchQuery: String = "",
    val activeCategory: String = AppCategory.FAVORITES,
    val currentProfile: Profile = Profile.Personal,
    val isDarkTheme: Boolean = true,
    val colorTheme: dev.vive.kdelauncher.data.model.ColorTheme = dev.vive.kdelauncher.data.model.ColorTheme.SYSTEM,
    val showAppLabels: Boolean = true,
    val showSettings: Boolean = false,
    val isLoading: Boolean = true,
    val appCounts: Map<String, Int> = emptyMap(),
    val categoryConfigs: List<CategoryConfig> = emptyList(),
    val visibleCategories: List<String> = AppCategory.FIXED,
    val iconSize: IconSize = IconSize.MEDIUM,
    val showIconBackground: Boolean = true,
    val gridColumns: Int = 3,
    val installedIconPacks: List<dev.vive.kdelauncher.data.IconPackInfo> = emptyList(),
    val selectedIconPack: String? = null,
    val isLoadingIconPacks: Boolean = false,
    val isDefaultLauncher: Boolean = true,
    val hasRealWorkProfile: Boolean = false,
    val isWorkProfileLocked: Boolean = false
)

@Immutable
data class AppGridState(
    val filteredApps: List<AppModel> = emptyList(),
    val searchQuery: String = "",
    val activeCategory: String = AppCategory.FAVORITES,
    val categoryConfigs: List<CategoryConfig> = emptyList(),
    val visibleCategories: List<String> = AppCategory.FIXED,
    val appCounts: Map<String, Int> = emptyMap(),
    val showAppLabels: Boolean = true,
    val iconSize: IconSize = IconSize.MEDIUM,
    val showIconBackground: Boolean = true,
    val gridColumns: Int = 3
)

internal data class GridContentInput(
    val filteredApps: List<AppModel>,
    val appCounts: Map<String, Int>,
    val query: String,
    val category: String,
    val configs: List<CategoryConfig>,
    val visible: List<String>
)

internal data class GridSettingsInput(
    val labels: Boolean,
    val size: IconSize,
    val bg: Boolean,
    val cols: Int
)

@OptIn(kotlinx.coroutines.FlowPreview::class)
class LauncherViewModel(
    application: Application,
    private val appRepository: AppRepository,
    private val profileManager: ProfileManager,
    private val settingsManager: SettingsManager,
    private val workProfileManager: WorkProfileManager,
    private val loadAppsUseCase: LoadAppsUseCase,
    private val launchAppUseCase: LaunchAppUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val toggleWorkAppUseCase: ToggleWorkAppUseCase,
    private val loadIconPacksUseCase: LoadIconPacksUseCase,
    private val getSystemStatusUseCase: GetSystemStatusUseCase,
    private val setCategoryOverrideUseCase: SetCategoryOverrideUseCase,
    private val openSetDefaultLauncherUseCase: OpenSetDefaultLauncherUseCase,
    private val openAppInfoUseCase: OpenAppInfoUseCase,
    private val uninstallAppUseCase: UninstallAppUseCase,
    private val suggestAppOrganizationUseCase: dev.vive.kdelauncher.domain.usecase.SuggestAppOrganizationUseCase,
    private val categoryCache: dev.vive.kdelauncher.data.repository.CategoryCache,
    private val appListCache: dev.vive.kdelauncher.data.repository.AppListCache,
    private val persistentAppCache: dev.vive.kdelauncher.data.repository.PersistentAppCache,
    private val checkProductTourStatusUseCase: CheckProductTourStatusUseCase,
    private val dismissProductTourUseCase: DismissProductTourUseCase,
    private val iconDiskCache: IconDiskCache,
) : AndroidViewModel(application) {

    // ── UI-controlled state ──────────────────────────────────────────────────
    private val _allApps = MutableStateFlow<List<AppModel>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _searchQueryDebounced = _searchQuery
        .debounce(150)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ""
        )
    private val _activeCategory = MutableStateFlow(AppCategory.FAVORITES)
    private val _showSettings = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(true)
    private val _isLoadingIconPacks = MutableStateFlow(false)
    private val _installedIconPacks = MutableStateFlow<List<dev.vive.kdelauncher.data.IconPackInfo>>(emptyList())
    private val _homeResetCounter = MutableStateFlow(0)
    private val _firstLaunchCompleted = MutableStateFlow(false)

    private val _tourState = MutableStateFlow(TourState())

    // ── Auto-organize suggestions (offline) ──────────────────────────────────
    private val _organizationSuggestionState = MutableStateFlow<OrganizationSuggestionState>(OrganizationSuggestionState.Idle)

    // ── Pending install suggestions (proactive categorization) ───────────────
    private val _pendingInstallSuggestions = MutableStateFlow<List<dev.vive.kdelauncher.domain.usecase.SuggestAppOrganizationUseCase.Suggestion>>(emptyList())

    // ── System status ────────────────────────────────────────────────────────
    private val _isDefaultLauncher = MutableStateFlow(true)
    private val _hasRealWorkProfile = MutableStateFlow(false)
    private val _isWorkProfileLocked = MutableStateFlow(false)

    // ── Package change receiver ──────────────────────────────────────────────
    private val packageChangeReceiver = PackageChangeReceiver(
        onPackageChanged = { refreshApps() }
    )

    // ── Hidden apps state ────────────────────────────────────────────────────
    private val _hiddenApps = MutableStateFlow<Set<String>>(emptySet())
    private val _tempHiddenApps = MutableStateFlow<Map<String, Long>>(emptyMap())
    // Ephemeral flag: when true, ALL hidden apps become visible in the UI.
    // Dies with the process (default = false). Does NOT touch DataStore.
    private val _showAllHiddenTemporarily = MutableStateFlow(false)

    // ── Derived flows from reactive repositories ─────────────────────────────
    private val visibleAppsFlow = combine(
        _allApps,
        _hiddenApps,
        _tempHiddenApps,
        _showAllHiddenTemporarily
    ) { apps, hidden, tempHidden, showAll ->
        if (showAll) return@combine apps
        val now = System.currentTimeMillis()
        apps.filter { app ->
            app.packageName !in hidden && (tempHidden[app.packageName] ?: 0L) <= now
        }
    }

    private data class CategoryBaseInput(
        val apps: List<AppModel>,
        val hidden: Set<String>,
        val displayNames: Map<String, String>,
        val iconNames: Map<String, String>,
        val order: List<String>
    )

    private val categoryBaseFlow = combine(
        visibleAppsFlow,
        settingsManager.hiddenCategories,
        settingsManager.categoryDisplayNames,
        settingsManager.categoryIconNames,
        settingsManager.categoryOrder
    ) { apps, hidden, displayNames, iconNames, order ->
        CategoryBaseInput(apps, hidden, displayNames, iconNames, order)
    }

    private val categoryConfigsFlow = categoryBaseFlow.combine(
        settingsManager.customCategories
    ) { base, custom ->
        val presentCategories = base.apps.map { it.category }.toSortedSet()
        val allCategories = (presentCategories + AppCategory.FIXED + custom).toSortedSet()
        val sortedCategories = allCategories.sortedBy { cat ->
            val idx = base.order.indexOf(cat)
            if (idx >= 0) idx else Int.MAX_VALUE
        }
        sortedCategories.map { cat ->
            CategoryConfig(
                category = cat,
                displayName = base.displayNames[cat] ?: AppCategory.displayName(cat),
                iconName = base.iconNames[cat] ?: AppCategory.defaultIcon(cat),
                isHidden = cat in base.hidden
            )
        }
    }

    private val visibleCategoriesFlow = categoryConfigsFlow.map { configs ->
        configs.filter { !it.isHidden }.map { it.category }
    }

    private val appInput = combine(visibleAppsFlow, _searchQuery, _activeCategory) { apps, q, cat ->
        LauncherAppInput(apps, q, cat)
    }

    private val settingsThemeInput = combine(
        settingsManager.darkTheme,
        settingsManager.colorTheme
    ) { dark, themeStr ->
        val theme = runCatching { dev.vive.kdelauncher.data.model.ColorTheme.valueOf(themeStr.uppercase()) }
            .getOrDefault(dev.vive.kdelauncher.data.model.ColorTheme.SYSTEM)
        Pair(dark, theme)
    }

    private val settingsDisplayInput = combine(
        settingsThemeInput,
        settingsManager.showAppLabels,
        _showSettings,
        _isLoading,
        settingsManager.iconSize.map { parseIconSize(it) }
    ) { themeInput, labels, showSettings, loading, size ->
        LauncherSettingsDisplayInput(themeInput.first, themeInput.second, labels, showSettings, loading, size)
    }

    private val settingsIconInput = combine(
        settingsManager.showIconBackground,
        settingsManager.gridColumns,
        settingsManager.selectedIconPack,
        _isLoadingIconPacks,
        _installedIconPacks
    ) { bg, cols, pack, loadingPacks, packs ->
        LauncherSettingsIconInput(bg, cols, pack, loadingPacks, packs)
    }

    private val profileInput = combine(
        profileManager.activeProfile,
        profileManager.personalFavorites,
        profileManager.workFavorites,
        profileManager.workApps
    ) { profile, personalFavorites, workFavorites, workApps ->
        LauncherProfileInput(profile, personalFavorites, workFavorites, workApps)
    }

    private val categoryInput = combine(
        categoryConfigsFlow,
        visibleCategoriesFlow,
        settingsManager.categoryOverrides
    ) { configs, visible, overrides ->
        LauncherCategoryInput(configs, visible, overrides)
    }

    private val systemInput = combine(
        _isDefaultLauncher,
        _hasRealWorkProfile,
        _isWorkProfileLocked
    ) { def, work, locked ->
        LauncherSystemInput(def, work, locked)
    }

    // Separate metadata computation (expensive) from filtering (cheap)
    private val appsWithMetaFlow = combine(
        visibleAppsFlow,
        profileInput,
        categoryInput,
        systemInput
    ) { apps, profile, category, system ->
        LauncherUiStateMapper.mapAppsWithMeta(
            apps = apps,
            profile = profile,
            categoryOverrides = category.categoryOverrides,
            hasRealWorkProfile = system.hasRealWorkProfile,
            workApps = profile.workApps
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private val appContentState = combine(
        appsWithMetaFlow,
        _searchQueryDebounced,
        _activeCategory,
        profileInput
    ) { appsWithMeta, query, activeCategory, profile ->
        LauncherUiStateMapper.mapAppContentFiltered(
            appsWithMeta = appsWithMeta,
            searchQuery = query,
            activeCategory = activeCategory,
            currentProfile = profile.currentProfile
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = LauncherAppContentState(
                allApps = emptyList(),
                filteredApps = emptyList(),
                appCounts = emptyMap()
            )
        )

    private val uiInput = combine(
        combine(appInput, settingsDisplayInput, settingsIconInput) { app, display, icon ->
            Triple(app, display, icon)
        },
        combine(profileInput, categoryInput, systemInput) { profile, cat, sys ->
            Triple(profile, cat, sys)
        }
    ) { appSettings, profileCatSys ->
        LauncherUiProjectionInput(
            app = appSettings.first,
            settingsDisplay = appSettings.second,
            settingsIcon = appSettings.third,
            profile = profileCatSys.first,
            category = profileCatSys.second,
            system = profileCatSys.third
        )
    }

    val uiState: StateFlow<LauncherUiState> = combine(
        uiInput,
        appContentState
    ) { input, appContent ->
        LauncherUiStateMapper.map(input, appContent)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = LauncherUiState()
    )

    private val gridSettingsFlow = combine(
        settingsManager.showAppLabels.distinctUntilChanged(),
        settingsManager.iconSize.map { parseIconSize(it) }.distinctUntilChanged(),
        settingsManager.showIconBackground.distinctUntilChanged(),
        settingsManager.gridColumns.distinctUntilChanged()
    ) { labels, size, bg, cols ->
        GridSettingsInput(labels, size, bg, cols)
    }

    val appGridState: StateFlow<AppGridState> = combine(
        combine(
            appContentState,
            _searchQuery,
            _activeCategory,
            categoryConfigsFlow.distinctUntilChanged(),
            visibleCategoriesFlow.distinctUntilChanged()
        ) { content, query, category, configs, visible ->
            GridContentInput(content.filteredApps, content.appCounts, query, category, configs, visible)
        },
        gridSettingsFlow
    ) { content, settings ->
        AppGridState(
            filteredApps = content.filteredApps,
            searchQuery = content.query,
            activeCategory = content.category,
            categoryConfigs = content.configs,
            visibleCategories = content.visible,
            appCounts = content.appCounts,
            showAppLabels = settings.labels,
            iconSize = settings.size,
            showIconBackground = settings.bg,
            gridColumns = settings.cols
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppGridState()
    )

    val tourState: StateFlow<TourState> = _tourState
    val organizationSuggestionState: StateFlow<OrganizationSuggestionState> = _organizationSuggestionState
    val pendingInstallSuggestions: StateFlow<List<dev.vive.kdelauncher.domain.usecase.SuggestAppOrganizationUseCase.Suggestion>> = _pendingInstallSuggestions

    val homeResetCounter: StateFlow<Int> = _homeResetCounter
    val firstLaunchCompleted: StateFlow<Boolean> = _firstLaunchCompleted
    val hiddenApps: StateFlow<Set<String>> = _hiddenApps
    val tempHiddenApps: StateFlow<Map<String, Long>> = _tempHiddenApps
    val showAllHiddenTemporarily: StateFlow<Boolean> = _showAllHiddenTemporarily

    init {
        try {
            // ── Phase 1: Optimistic restore with icon hydration ────────────────
            // The KEY insight: emitting apps without icons and then re-emitting
            // with icons causes a massive recomposition wave mid-scroll — every
            // AppModel changes (icon: null → bitmap), triggering the full combine
            // cascade and GPU texture uploads for all visible items in one frame.
            //
            // Fix: read icons from IconDiskCache IN PARALLEL during init, attach
            // them to the cached apps, and emit ONCE with icons already present.
            // The subsequent refreshApps() finds previousHadMissingIcons=false
            // and skips re-emitting unless the package list actually changed.
            viewModelScope.launch {
                val cachedApps = persistentAppCache.read()
                if (!cachedApps.isNullOrEmpty()) {
                    // Hydrate icons from disk cache before emitting.
                    // Parallel IO reads: ~200 icons × 10ms / 64 IO threads ≈ 30-80ms.
                    val hydratedApps = withContext(Dispatchers.IO) {
                        val selectedPack = settingsManager.selectedIconPack.first()
                        cachedApps.map { app ->
                            async {
                                val cacheKey = if (selectedPack.isNullOrEmpty()) app.versionCode
                                    else "${selectedPack.hashCode()}_${app.versionCode}".hashCode().toLong()
                                val bitmap = iconDiskCache.get(app.packageName, cacheKey)
                                if (bitmap != null) {
                                    val icon = AppIconBitmap(bitmap)
                                    icon.imageBitmap // pre-warm GPU texture on IO thread
                                    app.copy(icon = icon)
                                } else app
                            }
                        }.awaitAll()
                    }
                    _allApps.value = hydratedApps
                    _isLoading.value = false
                    appListCache.update(hydratedApps)
                } else if (appListCache.isFresh()) {
                    // Fallback: in-memory cache survived (e.g. config change).
                    _allApps.value = appListCache.lastApps
                    _isLoading.value = false
                }

                // Always refresh in background to detect newly installed/removed
                // apps. If we restored from cache with icons, this is silent AND
                // the smart emission check will skip re-emitting (no change).
                refreshApps(silent = _allApps.value.isNotEmpty())
            }

            // Load hidden apps state and clean expired temp entries.
            viewModelScope.launch {
                _hiddenApps.value = settingsManager.hiddenApps.first()
                val now = System.currentTimeMillis()
                val temp = settingsManager.tempHiddenApps.first()
                val expired = temp.filter { it.value <= now }.keys
                expired.forEach { settingsManager.clearTempHidden(it) }
                _tempHiddenApps.value = temp.filter { it.value > now }
            }

            // Defer non-critical startup work so the main thread is free for
            // Compose's first frame.
            viewModelScope.launch {
                refreshSystemStatus()
                refreshIconPacks()
                packageChangeReceiver.register(getApplication())
            }

            viewModelScope.launch {
                val completed = settingsManager.firstLaunchCompleted.first()
                _firstLaunchCompleted.value = completed
            }

            // Defer product-tour check until the UI has settled.
            viewModelScope.launch {
                _isLoading.first { !it }
                kotlinx.coroutines.delay(500)
                val isCompleted = checkProductTourStatusUseCase().first()
                if (!isCompleted) {
                    startProductTour()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── Refresh methods ──────────────────────────────────────────────────────

    fun refreshApps(silent: Boolean = false, forceEmit: Boolean = false) {
        viewModelScope.launch {
            try {
                if (!silent) _isLoading.value = true
                val selectedPack = settingsManager.selectedIconPack.first()
                val respectedCategories = AppCategory.FIXED.toSet() + AppCategory.AI_EXCLUDED

                val (metadataApps, fullApps) = loadAppsUseCase(selectedPack)

                // Do all categorisation and sorting on Default dispatcher so the
                // main thread is never blocked by heavy list operations.
                val processedApps = withContext(Dispatchers.Default) {
                    fullApps
                        .map { app ->
                            if (app.category in respectedCategories) app
                            else app.copy(category = AppCategory.ALL)
                        }
                        // Deduplicate by packageName + userHandle to eliminate any
                        // residual duplicates introduced by MIUI's PackageManager,
                        // PersistentAppCache race conditions, or smart-grouping side
                        // effects. This is the last line of defense before the list
                        // enters the reactive state flow and reaches LazyGrid keys.
                        .distinctBy { "${it.packageName}|${it.userHandle?.hashCode() ?: 0}" }
                        .sortedBy { it.label.lowercase() }
                }

                // ── Smart emission: avoid triggering the entire combine cascade ──
                // The combine chain (visibleAppsFlow → appsWithMetaFlow →
                // appContentState → appGridState + uiState) recalculates everything
                // when _allApps emits. We must emit when:
                //   a) Package list changed (app installed/uninstalled/updated)
                //   b) Previous list had null icons (process-death recovery in progress)
                // We must NOT emit when:
                //   c) Returning from YouTube after 30 min and nothing changed
                //      (icons already loaded from IconDiskCache, same packages)
                val previousApps = _allApps.value
                val previousFingerprint = previousApps.map { it.packageName to it.versionCode }
                val newFingerprint = processedApps.map { it.packageName to it.versionCode }
                val packageListChanged = previousFingerprint != newFingerprint
                val previousHadMissingIcons = previousApps.any { it.icon == null }

                if (packageListChanged || previousHadMissingIcons || forceEmit) {
                    _allApps.value = processedApps
                    appListCache.update(processedApps)
                }

                // Disk write only when the package list actually changed.
                if (packageListChanged) {
                    persistentAppCache.write(processedApps)
                }

                if (!silent) _isLoading.value = false

                if (!_firstLaunchCompleted.value) {
                    settingsManager.setFirstLaunchCompleted(true)
                    _firstLaunchCompleted.value = true
                }

                // Deferred low-priority work: organisation suggestions and cache
                // housekeeping do not need to block the startup path.
                viewModelScope.launch(Dispatchers.Default) {
                    categoryCache.purge(processedApps.map { it.packageName to it.versionCode })
                    try {
                        val overrides = settingsManager.categoryOverrides.first()
                        val suggestionResult = suggestAppOrganizationUseCase(processedApps)
                        val newSuggestions = suggestionResult.suggestions.filter { suggestion ->
                            val personalKey = "personal:${suggestion.packageName}"
                            val workKey = "work:${suggestion.packageName}"
                            overrides[personalKey] == null && overrides[workKey] == null
                        }
                        _pendingInstallSuggestions.value = newSuggestions
                    } catch (_: Exception) {
                        // Silently ignore suggestion failures during startup.
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (_allApps.value.isEmpty()) {
                    _allApps.value = emptyList()
                }
                if (!silent) _isLoading.value = false
            }
        }
    }

    fun refreshIconPacks() {
        viewModelScope.launch {
            try {
                _isLoadingIconPacks.value = true
                _installedIconPacks.value = loadIconPacksUseCase()
            } catch (e: Exception) {
                e.printStackTrace()
                _installedIconPacks.value = emptyList()
            } finally {
                _isLoadingIconPacks.value = false
            }
        }
    }

    fun refreshSystemStatus() {
        val status = getSystemStatusUseCase()
        _isDefaultLauncher.value = status.isDefaultLauncher
        _hasRealWorkProfile.value = status.hasRealWorkProfile
        _isWorkProfileLocked.value = status.isWorkProfileLocked
    }

    // ── User actions ─────────────────────────────────────────────────────────

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun hideApp(packageName: String) {
        viewModelScope.launch {
            settingsManager.setAppHidden(packageName, true)
            _hiddenApps.value = _hiddenApps.value + packageName
        }
    }

    fun unhideApp(packageName: String) {
        viewModelScope.launch {
            settingsManager.setAppHidden(packageName, false)
            settingsManager.clearTempHidden(packageName)
            _hiddenApps.value = _hiddenApps.value - packageName
            _tempHiddenApps.value = _tempHiddenApps.value - packageName
        }
    }

    fun tempHideApp(packageName: String, durationMinutes: Int) {
        viewModelScope.launch {
            val until = System.currentTimeMillis() + durationMinutes * 60_000L
            settingsManager.setTempHidden(packageName, until)
            _tempHiddenApps.value = _tempHiddenApps.value + (packageName to until)
        }
    }

    fun toggleShowHiddenTemporarily() {
        _showAllHiddenTemporarily.value = !_showAllHiddenTemporarily.value
    }

    fun setActiveCategory(category: String) {
        _activeCategory.value = category
        _searchQuery.value = ""
    }

    fun resetToHome() {
        _searchQuery.value = ""
        _activeCategory.value = AppCategory.FAVORITES
        _showSettings.value = false
        _homeResetCounter.value = _homeResetCounter.value + 1
    }

    fun handleBackPress(): Boolean {
        // Priority order: close settings → clear search → reset to favorites.
        // We always consume the event since a HOME activity has nowhere to go back to.
        return when {
            _showSettings.value -> {
                _showSettings.value = false
                true
            }
            _searchQuery.value.isNotBlank() -> {
                _searchQuery.value = ""
                true
            }
            _activeCategory.value != AppCategory.FAVORITES -> {
                _activeCategory.value = AppCategory.FAVORITES
                true
            }
            else -> {
                // Already at home state. Consume the event — a launcher must
                // never delegate back to the framework or the activity restarts.
                true
            }
        }
    }

    fun toggleProfile() {
        viewModelScope.launch {
            val newProfile = if (profileManager.activeProfile.value.type == ProfileType.PERSONAL)
                Profile.Work else Profile.Personal
            profileManager.setActiveProfile(newProfile)
        }
    }

    fun toggleSettings() { _showSettings.value = !_showSettings.value }

    fun toggleTheme() {
        viewModelScope.launch {
            settingsManager.setDarkTheme(!settingsManager.darkTheme.first())
        }
    }

    fun setColorTheme(theme: dev.vive.kdelauncher.data.model.ColorTheme) {
        viewModelScope.launch {
            settingsManager.setColorTheme(theme.name)
        }
    }

    fun setShowAppLabels(show: Boolean) {
        viewModelScope.launch { settingsManager.setShowAppLabels(show) }
    }

    fun setIconSize(size: IconSize) {
        viewModelScope.launch { settingsManager.setIconSize(size.name.lowercase()) }
    }

    fun setShowIconBackground(show: Boolean) {
        viewModelScope.launch { settingsManager.setShowIconBackground(show) }
    }

    fun setGridColumns(columns: Int) {
        viewModelScope.launch { settingsManager.setGridColumns(columns) }
    }

    fun setCategoryDisplayName(category: String, name: String) {
        viewModelScope.launch { settingsManager.setCategoryDisplayName(category, name) }
    }

    fun setCategoryIconName(category: String, iconName: String) {
        viewModelScope.launch { settingsManager.setCategoryIconName(category, iconName) }
    }

    fun toggleCategoryHidden(category: String) {
        viewModelScope.launch {
            val hidden = settingsManager.hiddenCategories.first().contains(category)
            settingsManager.setCategoryHidden(category, !hidden)
        }
    }

    fun setCategoryOrder(order: List<String>) {
        viewModelScope.launch { settingsManager.setCategoryOrder(order) }
    }

    fun deleteCategory(category: String, appCount: Int) {
        viewModelScope.launch {
            _allApps.value = _allApps.value.map { app ->
                if (app.category == category) app.copy(category = AppCategory.ALL) else app
            }
            settingsManager.setCategoryHidden(category, true)
            settingsManager.removeCustomCategory(category)
            if (_activeCategory.value == category) {
                _activeCategory.value = AppCategory.ALL
            }
        }
    }

    fun addCustomCategory(name: String) {
        viewModelScope.launch {
            val id = "custom_${System.currentTimeMillis()}"
            settingsManager.addCustomCategory(id)
            settingsManager.setCategoryDisplayName(id, name)
            settingsManager.setCategoryIconName(id, "Folder")
            val currentOrder = settingsManager.categoryOrder.first()
            settingsManager.setCategoryOrder(currentOrder + id)
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            _allApps.value = emptyList()
            _activeCategory.value = AppCategory.FAVORITES
            _searchQuery.value = ""
            _showSettings.value = false
            settingsManager.resetAll()
            categoryCache.clearAll()
            AppCategory.FIXED.forEach { cat ->
                settingsManager.setCategoryHidden(cat, false)
            }
            appRepository.clearIconPackCache()
            iconDiskCache.clear()
            refreshApps(forceEmit = true)
        }
    }

    fun setIconPack(packageName: String?) {
        viewModelScope.launch {
            settingsManager.setSelectedIconPack(packageName)
            appRepository.clearIconPackCache()
            // Clear disk cache so stale system/old-pack icons aren't served.
            // Without this, loadIconWithCache() would miss with the new cache key,
            // fall through to the icon pack, and if the component match fails,
            // re-cache the system icon under the new key — silently ignoring the pack.
            iconDiskCache.clear()
            refreshApps(forceEmit = true)
        }
    }

    fun setCategoryOverride(app: AppModel, category: String) {
        viewModelScope.launch {
            val isWorkApp = app.userHandle != null || app.profileTag == ProfileType.WORK
            val key = categoryOverrideKey(app, isWorkApp)
            setCategoryOverrideUseCase(key, category)
        }
    }

    fun clearCategoryOverride(app: AppModel) {
        viewModelScope.launch {
            val isWorkApp = app.userHandle != null || app.profileTag == ProfileType.WORK
            val key = categoryOverrideKey(app, isWorkApp)
            setCategoryOverrideUseCase.clear(key)
        }
    }

    fun launchApp(app: AppModel) {
        launchAppUseCase(app)
    }

    fun toggleFavorite(app: AppModel) {
        viewModelScope.launch {
            val profile = if (app.userHandle != null || app.profileTag == ProfileType.WORK) {
                Profile.Work
            } else {
                Profile.Personal
            }
            toggleFavoriteUseCase(profile, app.packageName)
        }
    }

    fun toggleWorkApp(app: AppModel) {
        viewModelScope.launch {
            toggleWorkAppUseCase(app.packageName)
        }
    }

    fun openSetDefaultLauncherScreen() {
        try {
            openSetDefaultLauncherUseCase()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun openAppInfo(app: AppModel) {
        try {
            openAppInfoUseCase(app)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun uninstallApp(app: AppModel) {
        try {
            uninstallAppUseCase(app)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun refreshStatus() {
        refreshSystemStatus()
    }

    // ── Auto-organize (offline heuristics) ───────────────────────────────────

    fun suggestOrganization() {
        viewModelScope.launch {
            _organizationSuggestionState.value = OrganizationSuggestionState.Loading
            val result = suggestAppOrganizationUseCase(_allApps.value)
            _organizationSuggestionState.value = OrganizationSuggestionState.Preview(result)
        }
    }

    fun applyOrganizationSuggestions(suggestions: List<dev.vive.kdelauncher.domain.usecase.SuggestAppOrganizationUseCase.Suggestion>) {
        viewModelScope.launch {
            suggestions.forEach { suggestion ->
                val app = _allApps.value.find { it.packageName == suggestion.packageName }
                if (app != null) {
                    val isWorkApp = app.userHandle != null || app.profileTag == ProfileType.WORK
                    val key = categoryOverrideKey(app, isWorkApp)
                    setCategoryOverrideUseCase(key, suggestion.proposedCategory)
                }
            }
            _pendingInstallSuggestions.value = emptyList()
            _organizationSuggestionState.value = OrganizationSuggestionState.Applied
        }
    }

    fun cancelOrganization() {
        _organizationSuggestionState.value = OrganizationSuggestionState.Idle
    }

    fun clearPendingInstallSuggestions() {
        _pendingInstallSuggestions.value = emptyList()
    }

    // ── Product Tour ─────────────────────────────────────────────────────────

    fun startProductTour() {
        _tourState.value = TourState(
            isActive = true,
            currentStepIndex = 0,
            steps = buildTourSteps()
        )
    }

    private fun buildTourSteps(): List<TourStep> {
        val baseSteps = TourStep.entries.filter {
            it != TourStep.LABS && it != TourStep.DEFAULT_LAUNCHER_BANNER
        }.toMutableList()

        if (!_isDefaultLauncher.value) {
            baseSteps.add(1, TourStep.DEFAULT_LAUNCHER_BANNER)
        }

        return baseSteps
    }

    fun nextTourStep() {
        val current = _tourState.value
        if (current.currentStepIndex < current.steps.lastIndex) {
            _tourState.value = current.copy(currentStepIndex = current.currentStepIndex + 1)
        } else {
            dismissProductTour()
        }
    }

    fun previousTourStep() {
        val current = _tourState.value
        if (current.currentStepIndex > 0) {
            _tourState.value = current.copy(currentStepIndex = current.currentStepIndex - 1)
        }
    }

    fun skipProductTour() {
        dismissProductTour()
    }

    fun dismissProductTour() {
        viewModelScope.launch {
            _tourState.value = _tourState.value.copy(isActive = false)
            dismissProductTourUseCase()
        }
    }

    // ── Factory ──────────────────────────────────────────────────────────────

    class Factory(
        private val container: AppContainer,
        private val application: Application
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LauncherViewModel(
                application = application,
                appRepository = container.appRepository,
                profileManager = container.profileManager,
                settingsManager = container.settingsManager,
                workProfileManager = container.workProfileManager,
                loadAppsUseCase = container.loadAppsUseCase,
                launchAppUseCase = container.launchAppUseCase,
                toggleFavoriteUseCase = container.toggleFavoriteUseCase,
                toggleWorkAppUseCase = container.toggleWorkAppUseCase,
                loadIconPacksUseCase = container.loadIconPacksUseCase,
                getSystemStatusUseCase = container.getSystemStatusUseCase,
                setCategoryOverrideUseCase = container.setCategoryOverrideUseCase,
                openSetDefaultLauncherUseCase = container.openSetDefaultLauncherUseCase,
                openAppInfoUseCase = container.openAppInfoUseCase,
                uninstallAppUseCase = container.uninstallAppUseCase,
                suggestAppOrganizationUseCase = container.suggestAppOrganizationUseCase,
                categoryCache = container.categoryCache,
                appListCache = container.appListCache,
                persistentAppCache = container.persistentAppCache,
                checkProductTourStatusUseCase = container.checkProductTourStatusUseCase,
                dismissProductTourUseCase = container.dismissProductTourUseCase,
                iconDiskCache = container.iconDiskCache,
            ) as T
        }
    }

    override fun onCleared() {
        super.onCleared()
        packageChangeReceiver.unregister(getApplication())
    }
}
