package dev.vive.kdelauncher.data.model

enum class AiSource {
    LOCAL_HEURISTIC,
    GROQ,
    GEMINI,
    OPENROUTER,
    COHERE,
    LEGACY_HEURISTIC,
    FALLBACK_DEFAULT
}

data class AppCategorization(
    val packageName: String,
    val categoryId: String,
    val iconName: String,
    val source: AiSource,
    val confidence: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis()
)
