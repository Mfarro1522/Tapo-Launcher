package dev.vive.kdelauncher.data.provider

interface AiProvider {
    val name: String
    val modelId: String
    val supportsStructuredOutput: Boolean

    suspend fun classify(
        systemPrompt: String,
        userPrompt: String
    ): Result<String>
}
