package dev.vive.kdelauncher.data.model

enum class AiProviderType(
    val displayName: String,
    val baseUrl: String,
    val freeModels: List<String>,
    val docsUrl: String,
    val limitDescription: String,
) {
    GROQ(
        "Groq", 
        "https://api.groq.com/openai/v1",
        listOf("llama3-8b-8192", "llama3-70b-8192", "mixtral-8x7b-32768"),
        "https://console.groq.com/keys", 
        "~14 000 req/día gratis"
    ),
    GEMINI(
        "Google AI Studio", 
        "https://generativelanguage.googleapis.com/v1beta",
        listOf("gemini-2.5-flash", "gemini-2.0-flash-lite"),
        "https://aistudio.google.com/app/apikey", 
        "Free tier generoso, sin tarjeta"
    ),
    OPENROUTER(
        "OpenRouter", 
        "https://openrouter.ai/api/v1",
        listOf("nvidia/llama-3.1-nemotron-70b-instruct:free", "google/gemini-2.5-flash:free", "meta-llama/llama-3.3-70b-instruct:free"),
        "https://openrouter.ai/keys", 
        "Modelos con ':free' al final sin costo"
    ),
}
