# Contexto Histórico y Deuda Técnica (MORE CONTEXT)

> Este documento consolida la historia técnica y estado actual del código fuente, complementando las características funcionales que ya están documentadas en el `README.md`.

## 1. ¿Cómo estaba antes? (Deuda Técnica Identificada)

Originalmente, el launcher funcionaba a nivel de usuario, pero estaba construido sobre una base frágil e inescalable con una concentración extrema de responsabilidades:

- **ViewModel God Object:** `LauncherViewModel` era un bloque gigante (cerca de 700 líneas) que contenía toda la lógica de negocio, manejo de BroadcastReceivers, notificaciones y persistencia. Anidaba múltiples llamadas `combine()` con conversores de tipo inseguros (`unchecked casts`).
- **Persistencia Acoplada e ineficiente:** Managers (como `SettingsManager`) usaban `SharedPreferences` de forma directa con continuas llamadas síncronas a `.apply()`, causando I/O disperso y afectando el rendimiento.
- **Categorización Imprecisa:** `AppCategorizer` agrupaba apps usando heurísticas frágiles (ej. catalogaba como "Games" cualquier app que contuviera la palabra "play", asignando erróneamente la "Google Play Store").
- **Cero Testing:** No existían tests unitarios ni instrumentados en absoluto, lo que hacía muy riesgoso cualquier cambio funcional.
- **CI/CD Roto e Inseguro:** El pipeline de GitHub Actions generaba APKs de tipo "Debug" en los artefactos de la "Release". Tampoco había archivo `proguard-rules.pro` para la ofuscación y el script `build-release.sh` ejecutaba rutas incorrectas.
- **Lag en el Renderizado (Grid):** Las imágenes y bitmaps de los iconos causaban recomposiciones infinitas de UI al hacer scroll en Compose.

## 2. ¿Qué se hizo? (El Proceso de Refactorización)

La limpieza se ejecutó en 5 fases controladas:

- **Fase 1 (Fixes Core & CI):**
  - Se corrigió el pipeline para ejecutar `assembleRelease` y generar binarios optimizados en producción.
  - Se agregaron las directivas base en `proguard-rules.pro` para salvaguardar las clases de Jetpack Compose.

- **Fase 2 (Desacoplamiento Estructural):**
  - Se aplicaron principios SOLID: La lógica del `LauncherViewModel` se extrajo en UseCases puros y delegó responsabilidades a repositorios e interfaces.
  - Se implementó un `AppContainer` para inyección de dependencias manual.
  - La persistencia migró por completo de `SharedPreferences` a flujos reactivos con **DataStore**.

- **Fase 3 (Testing & Hotfixes):**
  - **Rendimiento UI:** Se mitigó el lag envolviendo los metadatos visuales en clases de datos `@Immutable` y se corrigió una doble emisión de estado al cargar el grid.
  - Se estableció una suite de pruebas usando **Kotest**, **MockK** y **Kover**. Se crearon más de 85 tests unitarios asegurando la lógica de todos los Managers y Categorizadores.

- **Fase 4 (Endurecimiento de Reglas de Negocio):**
  - Se fortaleció `AppCategorizer` implementando un sistema de "Scoring" ponderado y una lista de permisos blanca (*Allowlist*) por `packageName` para excepciones históricas conflictivas.
  - El filtrado de UI (Perfil de Trabajo, Búsqueda, etc.) se separó estrictamente detrás de *Gateways* testeables.

- **Fase 5 (Mantenibilidad a Futuro):**
  - Se protegió la rama `main` agregando un flujo `pr.yml` que corre en cada Pull Request.
  - Se impuso un *Quality Gate* mediante **Kover**, obligando a un mínimo de 20% de cobertura en futuras iteraciones.
  - Se documentó formalmente la justificación del uso de permisos de sistema (`PERMISSIONS.md`).
  - Se incluyó un "Smoke Test" instrumentado (`LauncherStartupTest`) que valida la correcta inicialización del UI en Android sin crasheos.

## 3. ¿Cómo está ahora? (Estado Actual)

- **Arquitectura Mantenible:** El proyecto respeta la separación de responsabilidades a través de capas lógicas predecibles.
- **Listos para Crecer:** El repositorio está ahora en un estado técnico maduro para integrar características más complejas (ej. widgets, nuevos módulos, o animaciones avanzadas) de forma segura y sin deudas pendientes críticas.

## 4. Evolución Reciente (Temas Dev & TAPO Labs)

En la iteración más reciente, se integraron funcionalidades de alto impacto manteniendo la arquitectura limpia existente:

- **Temas de Color Dev:** Se implementó un sistema de *theming* dinámico con 7 paletas curadas (Dracula, Tokyo Night, Vercel, Catppuccin, Nord, Gruvbox, One Dark). La persistencia se gestiona eficientemente con `DataStore` y la UI reacciona mediante `CompositionLocal`.
- **TAPO Labs (IA):** Se añadió una sección experimental para la auto-organización inteligente de aplicaciones usando LLMs (Groq, Gemini, Cohere). Las llamadas a la IA y la lógica de organización se desacoplaron en nuevos casos de uso (`ConnectAiProviderUseCase`, `OrganizeAppsWithAiUseCase`), manejando los estados reactivos (`AiConnectionState`, `OrganizationState`) directamente desde el `LauncherViewModel`.
- **Nota técnica (Pruebas y Emuladores):** Siguiendo las directivas del proyecto (`AGENTS.md`), no se introdujeron pruebas automatizadas para estas capas. Además, se identificó que en ciertos emuladores el uso intensivo de estados con Compose puede generar inestabilidad (`InputDispatcher` crash), aunque la experiencia en dispositivos físicos permanece estable.

## 5. Implementación del Product Tour

Se añadió un tutorial interactivo (Product Tour) diseñado de forma 100% nativa con Jetpack Compose y arquitecturado para no contaminar la lógica central del launcher:
- **Gestión de Estado:** La capa de persistencia se resolvió añadiendo `productTourCompleted` al `DataStore` (vía `SettingsManager`). Se añadieron UseCases específicos para consultar y modificar este estado.
- **Componentes Puros:** La UI del tutorial (`TourTooltip`, `TourHighlighter`, `ProductTourOverlay`) reside en el paquete `ui/tour/` como componentes "puros" que reciben el estado del ViewModel y pintan el overlay respectivo con soporte de accesibilidad (TalkBack).
- **Modifier Extension (`Modifier.tourTarget`):** Para posicionar el tutorial dinámicamente sobre elementos existentes (SearchBar, SettingsButton, etc.) sin inyectar código pesado en la UI base, se creó un `Modifier` especial que extrae sus coordenadas en pantalla (`onGloballyPositioned`) única y exclusivamente cuando el tour está activo para ese paso, cacheando dichas medidas eficientemente.

### Rediseño y Pulido Final (Product Tour & Labs)
Para asegurar una experiencia de primer nivel ("premium"), se implementaron las siguientes mejoras finales en el Product Tour:
- **Transiciones y Barridos (Sweep):** El tooltip del tour calcula el centro de la pantalla para realizar transiciones fluidas de entrada y salida ("barridos"). El uso de `AnimatedContent` asegura un *crossfade* elegante del texto sin parpadeos rápidos, y el estado de la posición utiliza `lastKnownBounds` para evitar que la interfaz pierda el foco al cambiar de pasos de forma acelerada.
- **Efectos Cinemáticos:** El highlighter que enfoca los elementos de la interfaz ahora cuenta con animaciones de escala (`scaleIn` y `scaleOut`), creando un efecto dinámico de encendido y apagado de foco.
- **Soporte de Temas (Light/Dark):** La tarjeta del tooltip se desvinculó de colores oscuros estáticos y ahora utiliza nativamente los tokens del sistema de diseño (`surfaceVariant`), adaptándose perfectamente al tema blanco y oscuro.
- **Flujo de Privacidad (TAPO Labs):** Se estabilizó el paso experimental en el Tour, integrando un diálogo de consentimiento explícito de privacidad previo al envío de datos de apps hacia APIs de LLM.
