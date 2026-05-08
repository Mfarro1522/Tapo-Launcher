package dev.vive.kdelauncher.domain.usecase

import dev.vive.kdelauncher.data.model.AiSource
import dev.vive.kdelauncher.data.model.AppCategorization
import dev.vive.kdelauncher.data.model.AppModel
import dev.vive.kdelauncher.data.model.defaultCategories
import dev.vive.kdelauncher.data.provider.AiProvider
import dev.vive.kdelauncher.data.provider.AiResponseValidator
import dev.vive.kdelauncher.data.provider.ValidationResult
import dev.vive.kdelauncher.data.repository.CategoryCache
import dev.vive.kdelauncher.data.repository.StoredCategorization
import dev.vive.kdelauncher.data.model.AppCategorizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BatchDebugInfo(
    val batchIndex: Int,
    val appCount: Int,
    val status: String,
    val detail: String
)

data class OrganizationDebugInfo(
    val providerName: String,
    val modelId: String,
    val candidateCount: Int,
    val localMatchCount: Int,
    val aiCandidateCount: Int,
    val totalBatches: Int,
    val batchReports: List<BatchDebugInfo>
)

data class OrganizationResult(
    val fromCache: List<AppCategorization>,
    val fromLocal: List<AppCategorization>,
    val fromAi: List<AppCategorization>,
    val fromFallback: List<AppCategorization>,
    val debug: OrganizationDebugInfo
)

class OrganizeAppsWithAiUseCase(
    private val categoryCache: CategoryCache,
    private val aiPromptBuilder: AiPromptBuilder
) {
    suspend operator fun invoke(
        apps: List<AppModel>,
        provider: AiProvider,
        useCache: Boolean = false
    ): OrganizationResult = withContext(Dispatchers.IO) {

        val candidates = apps.filter { it.profileTag == dev.vive.kdelauncher.data.model.ProfileType.PERSONAL }

        val cachedList = mutableListOf<AppCategorization>()
        val uncachedList = mutableListOf<AppModel>()

        if (useCache) {
            for (app in candidates) {
                val cached = categoryCache.get(app.packageName, app.versionCode)
                if (cached != null) {
                    cachedList.add(
                        AppCategorization(
                            packageName = app.packageName,
                            categoryId = cached.categoryId,
                            iconName = cached.iconName,
                            source = runCatching { AiSource.valueOf(cached.source) }.getOrDefault(AiSource.FALLBACK_DEFAULT),
                            timestamp = cached.timestamp
                        )
                    )
                } else {
                    uncachedList.add(app)
                }
            }
        } else {
            uncachedList.addAll(candidates)
        }

        val localList = mutableListOf<AppCategorization>()
        val needsAiList = mutableListOf<AppModel>()

        for (app in uncachedList) {
            val localMatch = LocalHeuristics.classify(app.packageName)
            if (localMatch != null) {
                val categorization = AppCategorization(
                    packageName = app.packageName,
                    categoryId = localMatch.first,
                    iconName = localMatch.second,
                    source = AiSource.LOCAL_HEURISTIC
                )
                localList.add(categorization)
                categoryCache.put(
                    app.packageName,
                    app.versionCode,
                    StoredCategorization(
                        categorization.categoryId,
                        categorization.iconName,
                        categorization.source.name,
                        app.versionCode,
                        categorization.timestamp
                    )
                )
            } else {
                needsAiList.add(app)
            }
        }

        val aiList = mutableListOf<AppCategorization>()
        val fallbackList = mutableListOf<AppCategorization>()
        val batchReports = mutableListOf<BatchDebugInfo>()
        val aiSourceEnum = runCatching { AiSource.valueOf(provider.name.uppercase()) }.getOrDefault(AiSource.GROQ)

        val chunks = needsAiList.chunked(20)
        for ((index, chunk) in chunks.withIndex()) {
            val systemPrompt = aiPromptBuilder.buildSystemPrompt()
            val userPrompt = aiPromptBuilder.buildUserPrompt(defaultCategories, chunk)
            val result = provider.classify(systemPrompt, userPrompt)

            if (result.isSuccess) {
                val rawJson = result.getOrNull() ?: ""
                val expectedPackages = chunk.map { it.packageName }.toSet()

                when (val validation = AiResponseValidator.validate(rawJson, expectedPackages)) {
                    is ValidationResult.Valid -> {
                        val returnedPackages = mutableSetOf<String>()
                        for (i in 0 until validation.apps.length()) {
                            val obj = validation.apps.getJSONObject(i)
                            val pkg = obj.getString("p")
                            val catId = obj.getString("c")
                            val iconName = obj.getString("i")
                            val app = chunk.find { it.packageName == pkg }
                            if (app != null) {
                                returnedPackages.add(pkg)
                                val cat = AppCategorization(
                                    packageName = pkg,
                                    categoryId = catId,
                                    iconName = iconName,
                                    source = aiSourceEnum
                                )
                                aiList.add(cat)
                                categoryCache.put(
                                    app.packageName,
                                    app.versionCode,
                                    StoredCategorization(
                                        cat.categoryId,
                                        cat.iconName,
                                        cat.source.name,
                                        app.versionCode,
                                        cat.timestamp
                                    )
                                )
                            }
                        }

                        val missingFromChunk = chunk.filter { it.packageName !in returnedPackages }
                        if (missingFromChunk.isNotEmpty()) {
                            applyFallback(missingFromChunk, fallbackList, categoryCache)
                        }
                        val detail = if (missingFromChunk.isEmpty()) {
                            "JSON válido. ${returnedPackages.size}/${chunk.size} apps clasificadas por IA."
                        } else {
                            "JSON válido pero incompleto. IA ${returnedPackages.size}/${chunk.size}; faltantes → fallback ${missingFromChunk.size}."
                        }
                        batchReports.add(
                            BatchDebugInfo(
                                batchIndex = index + 1,
                                appCount = chunk.size,
                                status = if (missingFromChunk.isEmpty()) "AI_OK" else "AI_PARTIAL",
                                detail = detail
                            )
                        )
                    }

                    is ValidationResult.InvalidJson -> {
                        applyFallback(chunk, fallbackList, categoryCache)
                        batchReports.add(
                            BatchDebugInfo(
                                batchIndex = index + 1,
                                appCount = chunk.size,
                                status = "INVALID_JSON",
                                detail = validation.reason ?: "La respuesta no contenía un objeto JSON válido."
                            )
                        )
                    }

                    is ValidationResult.MissingApps -> {
                        applyFallback(chunk, fallbackList, categoryCache)
                        batchReports.add(
                            BatchDebugInfo(
                                batchIndex = index + 1,
                                appCount = chunk.size,
                                status = "MISSING_APPS",
                                detail = "Faltaron ${validation.packages.size} apps en la respuesta del modelo."
                            )
                        )
                    }

                    is ValidationResult.MalformedEntries -> {
                        applyFallback(chunk, fallbackList, categoryCache)
                        batchReports.add(
                            BatchDebugInfo(
                                batchIndex = index + 1,
                                appCount = chunk.size,
                                status = "MALFORMED_ENTRIES",
                                detail = "La respuesta incluyó categorías o íconos vacíos."
                            )
                        )
                    }
                }
            } else {
                applyFallback(chunk, fallbackList, categoryCache)
                batchReports.add(
                    BatchDebugInfo(
                        batchIndex = index + 1,
                        appCount = chunk.size,
                        status = "REQUEST_FAILED",
                        detail = result.exceptionOrNull()?.message ?: "Fallo desconocido del proveedor."
                    )
                )
            }
        }

        OrganizationResult(
            fromCache = cachedList,
            fromLocal = localList,
            fromAi = aiList,
            fromFallback = fallbackList,
            debug = OrganizationDebugInfo(
                providerName = provider.name,
                modelId = provider.modelId,
                candidateCount = candidates.size,
                localMatchCount = localList.size,
                aiCandidateCount = needsAiList.size,
                totalBatches = chunks.size,
                batchReports = batchReports
            )
        )
    }

    private suspend fun applyFallback(
        apps: List<AppModel>,
        fallbackList: MutableList<AppCategorization>,
        categoryCache: CategoryCache
    ) {
        apps.forEach { app ->
            val legacyCategoryId = AppCategorizer.categorize(app.packageName, -1)
            val fallbackCat = AppCategorization(
                packageName = app.packageName,
                categoryId = legacyCategoryId,
                iconName = "settings",
                source = AiSource.LEGACY_HEURISTIC
            )
            fallbackList.add(fallbackCat)
            categoryCache.put(
                app.packageName,
                app.versionCode,
                StoredCategorization(
                    fallbackCat.categoryId,
                    fallbackCat.iconName,
                    fallbackCat.source.name,
                    app.versionCode,
                    fallbackCat.timestamp
                )
            )
        }
    }
}
