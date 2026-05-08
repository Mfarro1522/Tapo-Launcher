package dev.vive.kdelauncher.domain.usecase

import dev.vive.kdelauncher.data.model.AiProviderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ConnectAiProviderTypeUseCase {

    suspend operator fun invoke(provider: AiProviderType, apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("API Key cannot be blank"))
            }

            val urlString = when (provider) {
                AiProviderType.GROQ -> "${provider.baseUrl}/models"
                AiProviderType.GEMINI -> "${provider.baseUrl}/models?key=$apiKey"
                AiProviderType.OPENROUTER -> "${provider.baseUrl}/models"
            }

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (provider == AiProviderType.GROQ || provider == AiProviderType.OPENROUTER) {
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
            }
            // OpenRouter recommends setting these headers
            if (provider == AiProviderType.OPENROUTER) {
                connection.setRequestProperty("HTTP-Referer", "https://github.com/Mfarro1522/KDE-Launcher")
                connection.setRequestProperty("X-Title", "TAPO Launcher")
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val models = parseModelsResponse(provider, responseString)
                Result.success(models)
            } else {
                val errorString = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Result.failure(Exception("HTTP $responseCode: $errorString"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseModelsResponse(provider: AiProviderType, responseString: String): List<String> {
        return try {
            val jsonObject = JSONObject(responseString)
            when (provider) {
                AiProviderType.GROQ -> {
                    val data = jsonObject.getJSONArray("data")
                    val models = mutableListOf<String>()
                    for (i in 0 until data.length()) {
                        models.add(data.getJSONObject(i).getString("id"))
                    }
                    models
                }
                AiProviderType.GEMINI -> {
                    val modelsArray = jsonObject.getJSONArray("models")
                    val models = mutableListOf<String>()
                    for (i in 0 until modelsArray.length()) {
                        val name = modelsArray.getJSONObject(i).getString("name")
                        models.add(name.substringAfter("models/"))
                    }
                    models
                }
                AiProviderType.OPENROUTER -> {
                    val data = jsonObject.getJSONArray("data")
                    val models = mutableListOf<String>()
                    for (i in 0 until data.length()) {
                        val modelObj = data.getJSONObject(i)
                        val id = modelObj.getString("id")
                        
                        // We can filter by those that have "free" in the ID or pricing is 0
                        // OpenRouter provides pricing info: "pricing": {"prompt": "0", "completion": "0"}
                        val pricing = modelObj.optJSONObject("pricing")
                        val isFree = id.endsWith(":free") || 
                                     (pricing != null && pricing.optString("prompt") == "0" && pricing.optString("completion") == "0")
                        
                        if (isFree) {
                            models.add(id)
                        }
                    }
                    if (models.isEmpty()) provider.freeModels else models
                }
            }
        } catch (e: Exception) {
            provider.freeModels
        }
    }
}
