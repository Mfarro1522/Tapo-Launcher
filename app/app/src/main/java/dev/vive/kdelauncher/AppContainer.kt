package dev.vive.kdelauncher

import android.app.Application
import dev.vive.kdelauncher.data.IconPackManagerImpl
import dev.vive.kdelauncher.data.ProfileManagerImpl
import dev.vive.kdelauncher.data.SettingsManagerImpl
import dev.vive.kdelauncher.data.WorkProfileManagerImpl
import dev.vive.kdelauncher.data.platform.AndroidAppPlatformGateway
import dev.vive.kdelauncher.data.platform.AndroidWorkProfilePlatformGateway
import dev.vive.kdelauncher.data.repository.AppRepositoryImpl
import dev.vive.kdelauncher.data.repository.IconDiskCache
import dev.vive.kdelauncher.domain.repository.AppRepository
import dev.vive.kdelauncher.domain.repository.IconPackManager
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

/**
 * Manual dependency injection container.
 *
 * Creates and holds singleton instances of all repositories, managers and use cases.
 * This is a pragmatic replacement for Hilt while we resolve the javapoet classpath issue.
 */
class AppContainer(private val application: Application) {

    private val appPlatformGateway = AndroidAppPlatformGateway(application)
    private val workProfilePlatformGateway = AndroidWorkProfilePlatformGateway(application)

    val profileManager: ProfileManager = ProfileManagerImpl(application)
    val settingsManager: SettingsManager = SettingsManagerImpl(application)
    val iconPackManager: IconPackManager = IconPackManagerImpl(application)
    val workProfileManager: WorkProfileManager = WorkProfileManagerImpl(workProfilePlatformGateway)
    val appRepository: AppRepository =
        AppRepositoryImpl(application.packageName, appPlatformGateway, iconPackManager)

    /**
     * On-disk icon cache. Keyed by packageName + versionCode (+ icon pack hash).
     * Stored in [android.content.Context.getCacheDir] so Android can reclaim space
     * under storage pressure without affecting app data.
     * Survives process death, eliminating the ~2 s icon reload delay on launcher resume.
     */
    val iconDiskCache = IconDiskCache(application.cacheDir)

    val loadAppsUseCase = LoadAppsUseCase(appRepository, workProfileManager, iconDiskCache)
    val launchAppUseCase = LaunchAppUseCase(application, appRepository, workProfileManager)
    val toggleFavoriteUseCase = ToggleFavoriteUseCase(profileManager)
    val toggleWorkAppUseCase = ToggleWorkAppUseCase(profileManager)
    val loadIconPacksUseCase = LoadIconPacksUseCase(iconPackManager)
    val getSystemStatusUseCase = GetSystemStatusUseCase(application, workProfileManager)
    val setCategoryOverrideUseCase = SetCategoryOverrideUseCase(settingsManager)
    val openSetDefaultLauncherUseCase = OpenSetDefaultLauncherUseCase(application)
    val openAppInfoUseCase = OpenAppInfoUseCase(application)
    val uninstallAppUseCase = UninstallAppUseCase(application)
    val categoryCache = dev.vive.kdelauncher.data.repository.CategoryCache(application)
    val appListCache = dev.vive.kdelauncher.data.repository.AppListCache()
    val persistentAppCache = dev.vive.kdelauncher.data.repository.PersistentAppCache(application)
    val suggestAppOrganizationUseCase = dev.vive.kdelauncher.domain.usecase.SuggestAppOrganizationUseCase()
    val checkProductTourStatusUseCase = CheckProductTourStatusUseCase(settingsManager)
    val dismissProductTourUseCase = DismissProductTourUseCase(settingsManager)
}
