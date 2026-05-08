package dev.vive.kdelauncher.domain.repository

import dev.vive.kdelauncher.data.model.Profile
import kotlinx.coroutines.flow.StateFlow

interface ProfileManager {
    val activeProfile: StateFlow<Profile>
    suspend fun setActiveProfile(profile: Profile)
    val personalFavorites: StateFlow<Set<String>>
    val workFavorites: StateFlow<Set<String>>
    suspend fun toggleFavorite(profile: Profile, packageName: String): Boolean
    suspend fun clearFavorites(profile: Profile)
    val workApps: StateFlow<Set<String>>
    suspend fun toggleWorkApp(packageName: String): Boolean
}
