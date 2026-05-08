package dev.vive.kdelauncher.domain.usecase

import dev.vive.kdelauncher.data.model.AiCategory
import dev.vive.kdelauncher.data.model.AppModel
import org.json.JSONArray
import org.json.JSONObject

class AiPromptBuilder {
    fun buildSystemPrompt(): String = """
        You classify Android apps for TAPO Launcher.

        HARD RULES:
        - Use ONLY the category IDs provided. Never invent categories.
        - Use ONLY the icon names inside the matched category's "icons" array. Never invent icons.
        - Assign exactly ONE category per app.
        - Do not skip any app from the input list.
        - Preserve package names exactly as given.
        - Prioritize user intent over literal app content (e.g. Discord → social, not games).
        - Output ONLY valid raw JSON. No markdown. No code fences. No comments. No explanation.

        GROUPING STRATEGY (very important):
        - Aim for 5 to 7 categories total. NEVER create more than 7 categories.
        - If a category would contain only 1 or 2 apps, merge those apps into the closest
          semantic super-group instead of creating a tiny category.
          Examples:
          • Yape (finance) + Rappi (shopping) → both go to "shopping" (they are both purchase-related).
          • A single browser app → put it in "utilities" rather than a solo "browsers" category.
          • One photo editor + one gallery → merge into "creativity".
          • One VPN + one file manager → merge into "utilities".
        - Prefer fewer, meaningful categories over many sparse ones.

        ICON SELECTION:
        - Each category has its own icon list. Only pick icons from the matched category.
        - Pick the icon that best matches the app's PRIMARY function.
        - If no exact match exists, pick the closest semantic match within the same list.

        OUTPUT SCHEMA:
        {"apps":[{"p":"package.name","c":"categoryId","i":"iconName"}]}
    """.trimIndent()

    fun buildUserPrompt(
        categories: List<AiCategory>,
        apps: List<AppModel>
    ): String {
        val jsonCats = JSONArray()
        categories.forEach { cat ->
            val catObj = JSONObject()
            catObj.put("id", cat.id)
            catObj.put("label", cat.label)
            catObj.put("description", cat.description)
            catObj.put("icons", JSONArray(cat.icons))
            jsonCats.put(catObj)
        }

        val jsonApps = JSONArray()
        apps.forEach { app ->
            val appObj = JSONObject()
            appObj.put("p", app.packageName)
            appObj.put("label", app.label)
            jsonApps.put(appObj)
        }

        val root = JSONObject()
        root.put("categories", jsonCats)
        root.put("apps", jsonApps)

        return root.toString()
    }
}
