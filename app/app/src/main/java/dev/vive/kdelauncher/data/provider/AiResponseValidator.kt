package dev.vive.kdelauncher.data.provider

import org.json.JSONArray
import org.json.JSONObject

object AiResponseValidator {
    fun validate(
        rawJson: String,
        expectedPackages: Set<String>
    ): ValidationResult {
        return try {
            val root = JSONObject(extractJsonObject(rawJson))
            val apps = root.getJSONArray("apps")

            val returnedPackages = (0 until apps.length())
                .map { apps.getJSONObject(it).getString("p") }
                .toSet()

            val missing = expectedPackages - returnedPackages
            val hasMalformedEntries = (0 until apps.length()).any { i ->
                val obj = apps.getJSONObject(i)
                obj.optString("c").isBlank() || obj.optString("i").isBlank()
            }

            when {
                missing.isNotEmpty() -> ValidationResult.MissingApps(missing)
                hasMalformedEntries  -> ValidationResult.MalformedEntries
                else                 -> ValidationResult.Valid(apps)
            }
        } catch (e: Exception) {
            ValidationResult.InvalidJson(e.message)
        }
    }

    private fun extractJsonObject(rawJson: String): String {
        val trimmed = rawJson
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')

        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1)
        }

        return trimmed
    }
}

sealed class ValidationResult {
    data object MalformedEntries : ValidationResult()
    data class MissingApps(val packages: Set<String>) : ValidationResult()
    data class InvalidJson(val reason: String?) : ValidationResult()
    data class Valid(val apps: JSONArray) : ValidationResult()
}
