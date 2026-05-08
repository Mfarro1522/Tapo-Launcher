package dev.vive.kdelauncher.domain.usecase

import dev.vive.kdelauncher.domain.repository.SettingsManager

class SetCategoryOverrideUseCase(private val settingsManager: SettingsManager) {
    suspend operator fun invoke(key: String, category: String) {
        settingsManager.setCategoryOverride(key, category)
    }

    suspend fun clear(key: String) {
        settingsManager.clearCategoryOverride(key)
    }
}
