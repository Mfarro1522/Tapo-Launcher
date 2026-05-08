package dev.vive.kdelauncher.domain.usecase

import android.app.Application
import android.content.Intent
import dev.vive.kdelauncher.data.model.AppIconBitmap
import dev.vive.kdelauncher.data.model.AppModel
import dev.vive.kdelauncher.domain.repository.AppRepository
import dev.vive.kdelauncher.domain.repository.WorkProfileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class LoadAppsUseCase(
    private val appRepository: AppRepository,
    private val workProfileManager: WorkProfileManager
) {

    /**
     * Two-phase loading:
     * 1. Returns metadata-only apps instantly
     * 2. Returns fully-loaded apps with icons
     */
    suspend operator fun invoke(selectedIconPack: String?): Pair<List<AppModel>, List<AppModel>> {
        return withContext(Dispatchers.IO) {
            val personalMeta = appRepository.getInstalledAppsMetadata()
            val workMeta = if (workProfileManager.hasRealWorkProfile()) {
                workProfileManager.getWorkProfileApps(loadIcons = false).map { app ->
                    AppModel(
                        packageName = app.packageName,
                        activityName = app.activityName,
                        label = app.label,
                        icon = app.icon,
                        category = dev.vive.kdelauncher.data.model.AppCategorizer.categorize(
                            app.packageName, app.androidCategory
                        ),
                        userHandle = app.userHandle,
                        versionCode = app.versionCode
                    )
                }
            } else emptyList()

            val metadataApps = mergeApps(personalMeta, workMeta)

            val iconsByKey = withContext(Dispatchers.IO) {
                val personalDeferreds = personalMeta.map { app ->
                    async {
                        val key = iconKey(app)
                        val bitmap = appRepository.getAppIcon(
                            packageName = app.packageName,
                            activityName = app.activityName,
                            selectedIconPack = selectedIconPack
                        )
                        key to bitmap
                    }
                }
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

            val fullApps = metadataApps.map { app ->
                val key = iconKey(app)
                val bitmap = iconsByKey[key]
                if (bitmap != null) app.copy(icon = AppIconBitmap(bitmap)) else app
            }

            metadataApps to fullApps
        }
    }

    private fun mergeApps(
        personalApps: List<AppModel>,
        workApps: List<AppModel>
    ): List<AppModel> = (personalApps + workApps).sortedBy { it.label.lowercase() }

    private fun iconKey(app: AppModel): String {
        val handleId = app.userHandle?.hashCode() ?: 0
        return "${app.packageName}|${app.activityName}|$handleId"
    }
}
