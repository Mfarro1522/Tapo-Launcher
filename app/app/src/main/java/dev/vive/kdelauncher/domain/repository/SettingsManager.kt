package dev.vive.kdelauncher.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsManager {
    val darkTheme: Flow<Boolean>
    suspend fun setDarkTheme(isDark: Boolean)

    val selectedIconPack: Flow<String?>
    suspend fun setSelectedIconPack(packageName: String?)

    val showAppLabels: Flow<Boolean>
    suspend fun setShowAppLabels(show: Boolean)

    val iconSize: Flow<String>
    suspend fun setIconSize(size: String)

    val showIconBackground: Flow<Boolean>
    suspend fun setShowIconBackground(show: Boolean)

    val gridColumns: Flow<Int>
    suspend fun setGridColumns(columns: Int)

    val categoryDisplayNames: Flow<Map<String, String>>
    suspend fun setCategoryDisplayName(category: String, name: String)

    val categoryIconNames: Flow<Map<String, String>>
    suspend fun setCategoryIconName(category: String, iconName: String)

    val hiddenCategories: Flow<Set<String>>
    suspend fun setCategoryHidden(category: String, hidden: Boolean)

    val categoryOrder: Flow<List<String>>
    suspend fun setCategoryOrder(order: List<String>)

    val categoryOverrides: Flow<Map<String, String>>
    suspend fun setCategoryOverride(key: String, category: String)
    suspend fun clearCategoryOverride(key: String)
    suspend fun clearAllCategoryOverrides()
    suspend fun resetCategoryPresentation()

    val colorTheme: Flow<String>
    suspend fun setColorTheme(theme: String)

    val labsEnabled: Flow<Boolean>
    suspend fun setLabsEnabled(enabled: Boolean)

    val aiProvider: Flow<String>
    suspend fun setAiProviderType(provider: String)

    val aiApiKey: Flow<String>
    suspend fun setAiApiKey(key: String)
    suspend fun clearAiApiKey()

    val aiModel: Flow<String>
    suspend fun setAiModel(model: String)

    val productTourCompleted: Flow<Boolean>
    suspend fun setProductTourCompleted(completed: Boolean)

    suspend fun resetAll()
}
