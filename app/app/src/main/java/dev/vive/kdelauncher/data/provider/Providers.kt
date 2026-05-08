package dev.vive.kdelauncher.data.provider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

suspend fun <T> retryWithBackoff(
    times: Int = 2,
    initialDelay: Long = 500L,
    block: suspend () -> T
): T {
    repeat(times) { attempt ->
        runCatching { return block() }
        delay(initialDelay * (attempt + 1))
    }
    return block() // Último intento — si falla, lanza la excepción
}

abstract class HttpAiProvider(protected val apiKey: String) : AiProvider {

    protected suspend fun postRequest(
        urlStr: String,
        headers: Map<String, String>,
        body: JSONObject,
        contentExtractor: (JSONObject) -> String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val result = retryWithBackoff {
                val url = URL(urlStr)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 45000
                headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }

                connection.outputStream.use { os ->
                    val input = body.toString().toByteArray()
                    os.write(input, 0, input.size)
                }

                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseString)
                    contentExtractor(json)
                } else {
                    val errorString = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                    throw Exception("HTTP $responseCode: $errorString")
                }
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GroqProvider(
    apiKey: String,
    override val modelId: String = "llama-3.3-70b-versatile"
) : HttpAiProvider(apiKey) {
    override val name = "Groq"
    override val supportsStructuredOutput = false

    override suspend fun classify(systemPrompt: String, userPrompt: String): Result<String> {
        val urlStr = "https://api.groq.com/openai/v1/chat/completions"
        val headers = mapOf(
            "Authorization" to "Bearer $apiKey",
            "Content-Type" to "application/json"
        )
        val body = JSONObject().apply {
            put("model", modelId)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
        }
        return postRequest(urlStr, headers, body) { json ->
            json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
    }
}

class GeminiProvider(
    apiKey: String,
    override val modelId: String = "gemini-2.5-flash-lite-preview-06-17"
) : HttpAiProvider(apiKey) {
    override val name = "Gemini"
    override val supportsStructuredOutput = true

    override suspend fun classify(systemPrompt: String, userPrompt: String): Result<String> {
        val urlStr = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$apiKey"
        val headers = mapOf("Content-Type" to "application/json")
        val body = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemPrompt))
                        put(JSONObject().put("text", userPrompt))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }
        return postRequest(urlStr, headers, body) { json ->
            json.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        }
    }
}

class OpenRouterProvider(
    apiKey: String,
    override val modelId: String = "nvidia/nemotron-3-super-120b-a12b:free"
) : HttpAiProvider(apiKey) {
    override val name = "OpenRouter"
    override val supportsStructuredOutput = false

    override suspend fun classify(systemPrompt: String, userPrompt: String): Result<String> {
        val urlStr = "https://openrouter.ai/api/v1/chat/completions"
        val headers = mapOf(
            "Authorization" to "Bearer $apiKey",
            "Content-Type" to "application/json",
            "HTTP-Referer" to "app://dev.vive.kdelauncher",
            "X-Title" to "TAPO Launcher"
        )
        val body = JSONObject().apply {
            put("model", modelId)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
        }
        return postRequest(urlStr, headers, body) { json ->
            json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
    }
}
