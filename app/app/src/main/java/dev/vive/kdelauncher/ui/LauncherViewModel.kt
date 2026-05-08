package dev.vive.kdelauncher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.vive.kdelauncher.AppContainer
import dev.vive.kdelauncher.data.model.AppCategory
import dev.vive.kdelauncher.data.model.AppModel
import dev.vive.kdelauncher.data.model.Profile
import dev.vive.kdelauncher.data.model.ProfileType
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AiConnectionState {
    object Idle : AiConnectionState()
    object Loading : AiConnectionState()
    data class Connected(val models: List<String>) : AiConnectionState()
    data class Error(val message: String) : AiConnectionState()
}

sealed class OrganizationState {
    object Idle : OrganizationState()
    object Loading : OrganizationState()
    data class Preview(val result: dev.vive.kdelauncher.domain.usecase.OrganizationResult) : OrganizationState()
    object Applied : OrganizationState()
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
    val isWorkProfileLocked: Boolean = false,
    val labsEnabled: Boolean = false,
    val aiProvider: dev.vive.kdelauncher.data.model.AiProviderType = dev.vive.kdelauncher.data.model.AiProviderType.GROQ,
    val aiConnectionState: AiConnectionState = AiConnectionState.Idle,
    val aiModel: String = "",
    val organizationState: OrganizationState = OrganizationState.Idle,
    val tourState: TourState = TourState()
)

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
    private val connectAiProviderTypeUseCase: dev.vive.kdelauncher.domain.usecase.ConnectAiProviderTypeUseCase,
    private val organizeAppsWithAiUseCase: dev.vive.kdelauncher.domain.usecase.OrganizeAppsWithAiUseCase,
    private val categoryCache: dev.vive.kdelauncher.data.repository.CategoryCache,
    private val checkProductTourStatusUseCase: CheckProductTourStatusUseCase,
    private val dismissProductTourUseCase: DismissProductTourUseCase,
) : AndroidViewModel(application) {

    // ── UI-controlled state ──────────────────────────────────────────────────
    private val _allApps = MutableStateFlow<List<AppModel>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _activeCategory = MutableStateFlow(AppCategory.FAVORITES)
    private val _showSettings = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(true)
    private val _isLoadingIconPacks = MutableStateFlow(false)
    private val _installedIconPacks = MutableStateFlow<List<dev.vive.kdelauncher.data.IconPackInfo>>(emptyList())
    private val _homeResetCounter = MutableStateFlow(0)

    private val _aiConnectionState = MutableStateFlow<AiConnectionState>(AiConnectionState.Idle)
    private val _organizationState = MutableStateFlow<OrganizationState>(OrganizationState.Idle)
    private val _tourState = MutableStateFlow(TourState())

    // ── System status ────────────────────────────────────────────────────────
    private val _isDefaultLauncher = MutableStateFlow(true)
    private val _hasRealWorkProfile = MutableStateFlow(false)
    private val _isWorkProfileLocked = MutableStateFlow(false)

    // ── Package change receiver ──────────────────────────────────────────────
    private val packageChangeReceiver = PackageChangeReceiver(
        onPackageChanged = { refreshApps() }
    )

    // ── Derived flows from reactive repositories ─────────────────────────────
    private val categoryConfigsFlow = combine(
        _allApps,
        settingsManager.hiddenCategories,
        settingsManager.categoryDisplayNames,
        settingsManager.categoryIconNames,
        settingsManager.categoryOrder
    ) { apps, hidden, displayNames, iconNames, order ->
        val presentCategories = apps.map { it.category }.toSortedSet()
        val allCategories = (presentCategories + AppCategory.FIXED).toSortedSet()
        val sortedCategories = allCategories.sortedBy { cat ->
            val idx = order.indexOf(cat)
            if (idx >= 0) idx else Int.MAX_VALUE
        }
        sortedCategories.map { cat ->
            CategoryConfig(
                category = cat,
                displayName = displayNames[cat] ?: AppCategory.displayName(cat),
                iconName = iconNames[cat] ?: AppCategory.defaultIcon(cat),
                isHidden = cat in hidden
            )
        }
    }

    private val visibleCategoriesFlow = categoryConfigsFlow.map { configs ->
        configs.filter { !it.isHidden }.map { it.category }
    }

    private val appInput = combine(_allApps, _searchQuery, _activeCategory) { apps, q, cat ->
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

    private val appContentInput = combine(
        appInput,
        profileInput,
        categoryInput,
        systemInput
    ) { app, profile, category, system ->
        LauncherAppContentInput(
            app = app,
            profile = profile,
            category = category,
            system = system
        )
    }

    private val aiSettingsInput = combine(
        settingsManager.labsEnabled,
        settingsManager.aiProvider,
        settingsManager.aiModel,
        _aiConnectionState,
        _organizationState
    ) { enabled, providerStr, model, connState, orgState ->
        val provider = runCatching { dev.vive.kdelauncher.data.model.AiProviderType.valueOf(providerStr.uppercase()) }
            .getOrDefault(dev.vive.kdelauncher.data.model.AiProviderType.GROQ)
        LauncherAiInput(enabled, provider, model, connState, orgState)
    }

    private val uiInput = combine(
        combine(appInput, settingsDisplayInput, settingsIconInput) { app, display, icon ->
            Triple(app, display, icon)
        },
        combine(profileInput, categoryInput, systemInput) { profile, cat, sys ->
            Triple(profile, cat, sys)
        },
        aiSettingsInput
    ) { appSettings, profileCatSys, aiSettings ->
        LauncherUiProjectionInput(
            app = appSettings.first,
            settingsDisplay = appSettings.second,
            settingsIcon = appSettings.third,
            profile = profileCatSys.first,
            category = profileCatSys.second,
            system = profileCatSys.third,
            ai = aiSettings
        )
    }

    private val appContentState = appContentInput
        .map(LauncherUiStateMapper::mapAppContent)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LauncherAppContentState(
                allApps = emptyList(),
                filteredApps = emptyList(),
                appCounts = emptyMap()
            )
        )

    val uiState: StateFlow<LauncherUiState> = combine(uiInput, appContentState, _tourState) { input, appContent, tour ->
        LauncherUiStateMapper.map(input, appContent, tour)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LauncherUiState()
    )

    val homeResetCounter: StateFlow<Int> = _homeResetCounter

    init {
        try {
            refreshApps()
            refreshIconPacks()
            packageChangeReceiver.register(getApplication())
            refreshSystemStatus()

            viewModelScope.launch {
                _isLoading.first { !it }
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

    fun refreshApps() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val selectedPack = settingsManager.selectedIconPack.first()
                val (_, fullApps) = loadAppsUseCase(selectedPack)
                val fixedSet = AppCategory.FIXED.toSet()
                _allApps.value = fullApps.map { app ->
                    if (app.category in fixedSet) app
                    else app.copy(category = AppCategory.ALL)
                }
                categoryCache.purge(_allApps.value.map { it.packageName to it.versionCode })
            } catch (e: Exception) {
                e.printStackTrace()
                _allApps.value = emptyList()
            } finally {
                _isLoading.value = false
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
        return if (_showSettings.value) {
            _showSettings.value = false
            true
        } else {
            false
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
            if (_activeCategory.value == category) {
                _activeCategory.value = AppCategory.ALL
            }
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
            refreshApps()
        }
    }

    fun setIconPack(packageName: String?) {
        viewModelScope.launch {
            settingsManager.setSelectedIconPack(packageName)
            appRepository.clearIconPackCache()
            refreshApps()
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

    // ── TAPO Labs (AI) ───────────────────────────────────────────────────────

    fun setLabsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setLabsEnabled(enabled)
        }
    }

    fun connectAiProviderType(provider: dev.vive.kdelauncher.data.model.AiProviderType, apiKey: String) {
        viewModelScope.launch {
            _aiConnectionState.value = AiConnectionState.Loading
            settingsManager.setAiProviderType(provider.name)
            settingsManager.setAiApiKey(apiKey)

            val result = connectAiProviderTypeUseCase(provider, apiKey)
            result.onSuccess { models ->
                _aiConnectionState.value = AiConnectionState.Connected(models)
                if (models.isNotEmpty()) {
                    settingsManager.setAiModel(models.first())
                } else {
                    settingsManager.setAiModel("")
                }
            }.onFailure { error ->
                _aiConnectionState.value = AiConnectionState.Error(error.message ?: "Error desconocido")
                settingsManager.clearAiApiKey()
            }
        }
    }

    fun disconnectAiProviderType() {
        viewModelScope.launch {
            settingsManager.clearAiApiKey()
            _aiConnectionState.value = AiConnectionState.Idle
            _organizationState.value = OrganizationState.Idle
        }
    }

    fun setAiModel(model: String) {
        viewModelScope.launch {
            settingsManager.setAiModel(model)
        }
    }

    fun organizeAppsWithAi() {
        viewModelScope.launch {
            _organizationState.value = OrganizationState.Loading
            try {
                val providerStr = settingsManager.aiProvider.first()
                val apiKey = settingsManager.aiApiKey.first()
                val model = settingsManager.aiModel.first()

                val providerType = runCatching { dev.vive.kdelauncher.data.model.AiProviderType.valueOf(providerStr.uppercase()) }
                    .getOrDefault(dev.vive.kdelauncher.data.model.AiProviderType.GROQ)

                val provider = when (providerType) {
                    dev.vive.kdelauncher.data.model.AiProviderType.GROQ -> dev.vive.kdelauncher.data.provider.GroqProvider(apiKey, model.ifBlank { "llama-3.3-70b-versatile" })
                    dev.vive.kdelauncher.data.model.AiProviderType.GEMINI -> dev.vive.kdelauncher.data.provider.GeminiProvider(apiKey, model.ifBlank { "gemini-2.5-flash-lite-preview-06-17" })
                    dev.vive.kdelauncher.data.model.AiProviderType.OPENROUTER -> dev.vive.kdelauncher.data.provider.OpenRouterProvider(apiKey, model.ifBlank { "nvidia/nemotron-3-super-120b-a12b:free" })
                }

                val apps = _allApps.value
                val result = organizeAppsWithAiUseCase(apps, provider, useCache = false)
                _organizationState.value = OrganizationState.Preview(result)
            } catch (e: Exception) {
                _organizationState.value = OrganizationState.Idle
                _aiConnectionState.value = AiConnectionState.Error(e.message ?: "No se pudo organizar con IA")
            }
        }
    }

    fun applyAiSuggestions(selected: List<dev.vive.kdelauncher.data.model.AppCategorization>) {
        viewModelScope.launch {
            _organizationState.value = OrganizationState.Loading

            profileManager.clearFavorites(Profile.Personal)
            profileManager.clearFavorites(Profile.Work)
            settingsManager.clearAllCategoryOverrides()
            settingsManager.resetCategoryPresentation()
            categoryCache.clearAll()

            AppCategory.FIXED.forEach { category ->
                settingsManager.setCategoryHidden(category, false)
            }
            settingsManager.setCategoryOrder(AppCategory.FIXED)
            _activeCategory.value = AppCategory.ALL

            selected.forEach { suggestion ->
                val app = _allApps.value.find { it.packageName == suggestion.packageName }
                if (app != null) {
                    val isWorkApp = app.userHandle != null || app.profileTag == ProfileType.WORK
                    val key = categoryOverrideKey(app, isWorkApp)
                    setCategoryOverrideUseCase(key, suggestion.categoryId)
                }
            }
            _organizationState.value = OrganizationState.Applied
        }
    }

    fun cancelAiOrganization() {
        _organizationState.value = OrganizationState.Idle
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

        if (uiState.value.labsEnabled) {
            val finishIndex = baseSteps.indexOf(TourStep.FINISH)
            if (finishIndex != -1) {
                baseSteps.add(finishIndex, TourStep.LABS)
            } else {
                baseSteps.add(TourStep.LABS)
            }
        }
        return baseSteps
    }

    fun nextTourStep() {
        val current = _tourState.value
        if (current.currentStepIndex < current.steps.lastIndex) {
            val nextStep = current.steps[current.currentStepIndex + 1]
            if (nextStep == TourStep.LABS) {
                _showSettings.value = true
            }
            _tourState.value = current.copy(currentStepIndex = current.currentStepIndex + 1)
        } else {
            dismissProductTour()
        }
    }

    fun previousTourStep() {
        val current = _tourState.value
        if (current.currentStepIndex > 0) {
            val prevStep = current.steps[current.currentStepIndex - 1]
            if (current.currentStep() == TourStep.LABS && prevStep != TourStep.LABS) {
                _showSettings.value = false
            }
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
                connectAiProviderTypeUseCase = container.connectAiProviderTypeUseCase,
                organizeAppsWithAiUseCase = container.organizeAppsWithAiUseCase,
                categoryCache = container.categoryCache,
                checkProductTourStatusUseCase = container.checkProductTourStatusUseCase,
                dismissProductTourUseCase = container.dismissProductTourUseCase,
            ) as T
        }
    }

    override fun onCleared() {
        super.onCleared()
        packageChangeReceiver.unregister(getApplication())
    }
}
