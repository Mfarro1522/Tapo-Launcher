package dev.vive.kdelauncher.data

import android.content.Context
import android.content.SharedPreferences
import dev.vive.kdelauncher.data.model.Profile
import dev.vive.kdelauncher.data.model.ProfileType
import dev.vive.kdelauncher.domain.repository.ProfileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages user profile state and persistence.
 * Stores profile preference, independent favorites per profile, and manual work app flags.
 *
 * Implements [ProfileManager] interface and exposes reactive [StateFlow]
 * properties so the ViewModel no longer needs to manually refresh values.
 */
class ProfileManagerImpl(context: Context) : ProfileManager {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("kdelauncher_prefs", Context.MODE_PRIVATE)

    private val _activeProfile = MutableStateFlow(readActiveProfile())
    override val activeProfile: StateFlow<Profile> = _activeProfile.asStateFlow()

    private val _personalFavorites = MutableStateFlow(readFavorites(Profile.Personal))
    override val personalFavorites: StateFlow<Set<String>> = _personalFavorites.asStateFlow()

    private val _workFavorites = MutableStateFlow(readFavorites(Profile.Work))
    override val workFavorites: StateFlow<Set<String>> = _workFavorites.asStateFlow()

    private val _workApps = MutableStateFlow(readWorkApps())
    override val workApps: StateFlow<Set<String>> = _workApps.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            KEY_ACTIVE_PROFILE -> _activeProfile.value = readActiveProfile()
            KEY_FAVORITES_PERSONAL,
            KEY_FAVORITES_LEGACY -> _personalFavorites.value = readFavorites(Profile.Personal)
            KEY_FAVORITES_WORK -> _workFavorites.value = readFavorites(Profile.Work)
            KEY_WORK_APPS -> _workApps.value = readWorkApps()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    override suspend fun setActiveProfile(profile: Profile) {
        prefs.edit().putString(KEY_ACTIVE_PROFILE, profile.type.name).apply()
        _activeProfile.value = profile
    }

    override suspend fun toggleFavorite(profile: Profile, packageName: String): Boolean {
        val current = currentFavorites(profile).toMutableSet()
        val isFavorite = if (current.contains(packageName)) {
            current.remove(packageName)
            false
        } else {
            current.add(packageName)
            true
        }
        prefs.edit().putStringSet(favoritesKey(profile), current).apply()
        updateFavoritesFlow(profile, current)
        return isFavorite
    }

    override suspend fun clearFavorites(profile: Profile) {
        prefs.edit().putStringSet(favoritesKey(profile), emptySet()).apply()
        updateFavoritesFlow(profile, emptySet())
        if (profile.type == ProfileType.PERSONAL) {
            prefs.edit().remove(KEY_FAVORITES_LEGACY).apply()
        }
    }

    override suspend fun toggleWorkApp(packageName: String): Boolean {
        val current = _workApps.value.toMutableSet()
        val isWork = if (current.contains(packageName)) {
            current.remove(packageName)
            false
        } else {
            current.add(packageName)
            true
        }
        prefs.edit().putStringSet(KEY_WORK_APPS, current).apply()
        _workApps.value = current
        return isWork
    }

    private fun readActiveProfile(): Profile {
        val type = prefs.getString(KEY_ACTIVE_PROFILE, ProfileType.PERSONAL.name)
        return if (type == ProfileType.WORK.name) Profile.Work else Profile.Personal
    }

    private fun readFavorites(profile: Profile): Set<String> {
        val stored = prefs.getStringSet(favoritesKey(profile), null)
        if (stored != null) return stored

        return if (profile.type == ProfileType.PERSONAL) {
            prefs.getStringSet(KEY_FAVORITES_LEGACY, emptySet()) ?: emptySet()
        } else {
            emptySet()
        }
    }

    private fun readWorkApps(): Set<String> {
        return prefs.getStringSet(KEY_WORK_APPS, emptySet()) ?: emptySet()
    }

    private fun currentFavorites(profile: Profile): Set<String> {
        return if (profile.type == ProfileType.WORK) _workFavorites.value else _personalFavorites.value
    }

    private fun updateFavoritesFlow(profile: Profile, favorites: Set<String>) {
        if (profile.type == ProfileType.WORK) {
            _workFavorites.value = favorites
        } else {
            _personalFavorites.value = favorites
        }
    }

    private fun favoritesKey(profile: Profile): String {
        return if (profile.type == ProfileType.WORK) KEY_FAVORITES_WORK else KEY_FAVORITES_PERSONAL
    }

    private companion object {
        const val KEY_ACTIVE_PROFILE = "active_profile"
        const val KEY_FAVORITES_LEGACY = "favorites"
        const val KEY_FAVORITES_PERSONAL = "favorites_personal"
        const val KEY_FAVORITES_WORK = "favorites_work"
        const val KEY_WORK_APPS = "work_apps"
    }
}
