# TAPO Labs — Checklist de Refactor para Arquitectura Multi-Proveedor

> Documento técnico de referencia para preparar `TAPO Labs` ante la integración de cualquier proveedor de IA (Groq, Gemini, OpenRouter/Nemotron, Cohere u otros futuros). Los cambios están ordenados por impacto decreciente, no por esfuerzo de implementación.

***

## 1. Capa de Conectores AI — La más crítica

- [x ] **1.1 Definir interfaz `AiProvider`**
  Contrato único que aisla al ViewModel y los use cases de cualquier SDK específico. Implementaciones: `GroqProvider`, `GeminiProvider`, `OpenRouterProvider`.
- [ x] **1.2 Agregar `retryWithBackoff`**
  Antes de caer al fallback, reintentar 2 veces con espera exponencial.
- [ x] **1.3 Validador de respuesta JSON**
  Corre antes del parser. Si falla, activa fallback en lugar de crashear. `AiResponseValidator`.

## 2. Capa de Use Cases

- [ x] **2.1 Extraer `LocalHeuristics` como objeto separado**
  Pre-clasificador que corre antes de cualquier llamada al LLM.
- [ x] **2.2 Filtrado por fases en `OrganizeAppsWithAiUseCase`**
  Fases: Excluir favoritos/trabajo, Caché válida, Match local, LLM, Fallback.
- [ x] **2.3 Extraer `AiPromptBuilder` como clase propia**
  Centralizar la construcción de los prompts. Incluyendo el System Prompt proporcionado:

  ```
  You classify Android apps for TAPO Launcher.

  HARD RULES:
  - Use ONLY the category IDs provided. Never invent categories.
  - Use ONLY the icon names inside the matched category's "icons" array. Never invent icons.
  - Assign exactly ONE category per app.
  - Do not skip any app from the input list.
  - Preserve package names exactly as given.
  - Prioritize user intent over literal app content (e.g. Discord → Social, not Games).
  - Output ONLY valid raw JSON. No markdown. No code fences. No comments. No explanation.

  ICON SELECTION:
  - Each category has its own icon list. Only pick icons from the matched category.
  - Pick the icon that best matches the app's PRIMARY function.
  - If no exact match exists, pick the closest semantic match within the same list.

  OUTPUT SCHEMA:
  {"apps":[{"p":"package.name","c":"categoryId","i":"iconName"}]}
  ```

## 3. Contrato de Datos

- [ x] **3.1 `AiCategory` con íconos embebidos**
  Los íconos viven dentro de la categoría.
- [ x] **3.2 `AiSource` en `AppCategorization`**
  Enum para trackear la fuente: `LOCAL_HEURISTIC`, `GROQ`, `GEMINI`, `OPENROUTER`, `COHERE`, `LEGACY_HEURISTIC`, `FALLBACK_DEFAULT`.
- [ x] **3.3 Clave de caché con `versionCode`**
  `AppCacheKey(packageName, versionCode)`.

## 4. Persistencia en DataStore

- [x ] **4.1 Guardar `source` y `timestamp`**
  En `StoredCategorization`.
- [ x] **4.2 Purgar apps desinstaladas**
  Función `purgeUninstalledApps`.

## 5. Taxonomía

- [ x] **5.1 Agregar `browsers` y `dev`**
  Ajustar las categorías por defecto.

---
Resumen de progreso: (Se actualizará conforme avance)