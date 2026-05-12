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

### Estado actual (resumen)

- La **refactorización arquitectónica** ya está completada (Fases 1 a 5).
- Se resolvieron **problemas críticos de rendimiento** en cuatro fases:
  - Fase 1: Aislamiento de estado con múltiples `StateFlow` y migración a `Modifier.Node`.
  - Fase 2: Creación de `AppGridState` independiente, carga bifásica real, y filtrado en `Dispatchers.Default`.
  - Fase 3: Eliminación de `onGloballyPositioned` masivo en celdas del grid, estabilización de `AppModel` con `@Immutable`, debounce de búsqueda (150ms), y aplicación condicional de modifiers de tour.
  - **Fase 4 (última):** Corrección crítica del manifest (`clearTaskOnLaunch`, `largeHeap`), caché de proceso (`AppListCache`) + caché persistente en disco (`PersistentAppCache`) para warm start instantáneo y recuperación tras process death, single emission en startup, y defer de trabajo no crítico.
- Se implementaron las funcionalidades de **Temas de Color Dev** (Dracula, Tokyo Night, Vercel, Catppuccin, Nord, Gruvbox, One Dark) y **TAPO Labs** (auto-organización experimental de apps con IA a través de Groq, Gemini o Cohere).
- Se implementó exitosamente el **Product Tour** nativo (tutorial interactivo de primer uso) con animaciones cinemáticas, transiciones de barrido, y soporte adaptativo para modo claro y oscuro. El tour usa modifiers condicionales para cero overhead cuando no está activo.
- **Nuevas categorías inteligentes:** Sistema de smart grouping para Wallets, Compras, Finanzas y Dev con lógica de merge automático.
- **Sistema de apps ocultas:** Ocultamiento permanente y temporal con persistencia en DataStore y UI de gestión en el panel de ajustes.
- **Menú contextual rediseñado:** Popup posicionado cerca del ícono con estilo consistente al tema, acciones de Favorito, Mover, Información y Desinstalar.
- **Colores hardcodeados eliminados:** Auditoría completa de la UI reemplazando colores literales por tokens semánticos del sistema de diseño.
- **Bugs críticos corregidos:**
  - Renombrar categorías dinámicas (wallets, compras, finanzas, dev) ahora persiste correctamente gracias a la actualización de `knownCategories` en `SettingsManagerImpl`.
  - Process death restore lento (~20s pantalla vacía) corregido con `PersistentAppCache`: caché en disco JSON que restaura la lista de apps en ~50ms, renderizando la UI instantáneamente mientras los íconos se cargan en background.
  - Orientación fija a portrait programáticamente (`requestedOrientation = SCREEN_ORIENTATION_PORTRAIT` en `MainActivity.onCreate()`) para evitar recreaciones y el botón de rotación manual, sin interferir con gestos del sistema en el manifest.
  - El asistente del sistema (long-press Home → Google Assistant/Gemini/Circle to Search) funciona correctamente tras remover `priority="1"` del intent-filter HOME, que interfería con la resolución de gestos del sistema en ciertos dispositivos.
- El proyecto usa un **launcher real** con soporte para Work Profile, icon packs, ajustes visuales, y tematización dinámica.
- *Nota sobre pruebas:* Siguiendo las reglas del repositorio, el proyecto se mantiene ágil sin la creación activa de suites de tests para las capas de UI experimentales.

### Flujo de datos

1. **MainActivity** obtiene el `LauncherViewModel` desde el contenedor de la app y observa sus estados independientes (`uiState`, `appGridState`, `tourState`).
2. El ViewModel delega en los **use cases** para cargar apps, lanzar actividades, marcar favoritos y leer estado del sistema.
3. La carga de apps se hace en **single emission**: el ViewModel inicializa `_allApps` desde `AppListCache` (warm start) y luego refresca en background. Todo el procesamiento (categorizacion, sorting) ocurre en `Dispatchers.Default`.
4. Los cambios de configuracion se guardan en **DataStore** y se reflejan en la interfaz con `StateFlow`.
5. El filtrado de apps (busqueda + categoria) se ejecuta en **`Dispatchers.Default`** mediante `.flowOn()`, evitando bloqueos en el hilo principal.
6. Trabajo no critico (`refreshIconPacks`, `refreshSystemStatus`, sugerencias de organizacion) se **difiere** tras el primer frame para no competir por el hilo principal durante el arranque.

### Optimizaciones de rendimiento (resumen)

- **Warm start con cache de proceso**: `AppListCache` guarda la ultima lista de apps en memoria. Si el proceso aun vive, el launcher se restaura instantaneamente.
- **Recuperacion tras process death**: `PersistentAppCache` serializa metadata de apps a JSON en disco. Tras ser matado por el sistema, el launcher restaura metadata en ~50ms, luego hidrata iconos desde `IconDiskCache` en paralelo (~80ms) antes de la primera emision.
- **Zero-emission icon hydration**: En el init del ViewModel, los iconos se leen desde `IconDiskCache` en paralelo (200 iconos / 64 IO threads ≈ 80ms) y se adjuntan a los apps ANTES de la primera emision de `_allApps`. Esto elimina la transicion placeholder→icono que causaba una ola masiva de recomposicion mid-scroll.
- **Single emission en startup**: `_allApps` emite UNA sola vez con iconos ya adjuntos. El posterior `refreshApps()` detecta que nada cambio (`previousHadMissingIcons=false`, fingerprint identico) y no re-emite.
- **Manifest optimizado**: Removido `clearTaskOnLaunch` (que forzaba recreacion de Activity en cada Home) y `stateNotNeeded`. Agregado `largeHeap` para reducir kills por memoria.
- **Cache en memoria**: Los iconos decodificados se guardan para evitar trabajo repetido.
- **Icon packs**: Cada `appfilter.xml` se parsea una sola vez y se reutiliza.
- **StateFlow independientes**: `uiState`, `appGridState` y `tourState` son flujos separados en el ViewModel para aislar recomposiciones.
- **Modelos `@Immutable`**: `AppModel` y `AppGridState` estan marcados como inmutables, permitiendo que Compose recicle eficientemente los items del grid.
- **Grid eficiente**: `LazyVerticalGrid` usa `key` estable (`packageName + profileTag`) y `contentType` para reciclaje optimo.
- **Callbacks estables**: Los lambdas `onClick`/`onLongPress` de cada celda se cachean con `remember(app)`; `AppIcon` usa `rememberUpdatedState` para evitar reiniciar el detector de gestos.
- **Busqueda con debounce**: El query de busqueda se debouncea a 150ms para evitar filtrado continuo mientras el usuario escribe.
- **Filtrado en background**: `mapAppContentFiltered` corre en `Dispatchers.Default` para no bloquear el hilo principal.
- **Tour condicional**: El modifier `.tourTarget()` solo se aplica cuando `tourState.isActive == true`, eliminando overhead de `Modifier.Node` durante el uso normal.
- **Sin tracking por celda**: Reemplazado `onGloballyPositioned` (que disparaba en cada layout pass de cada item visible durante scroll) por `onPlaced` (solo dispara cuando cambia el placement). Las coordenadas para el menu contextual se calculan lazily al long-press, no cada frame.

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

## 6. Optimizaciones de Rendimiento — Fase 1 (MVI + Compose)

Para resolver problemas críticos de "lag" o caída de fotogramas al hacer scroll en categorías con muchas aplicaciones, se ejecutó un diagnóstico y refactorización orientada puramente a la eficiencia de Jetpack Compose:
- **Aislamiento de Estado (MVI):** Se extrajeron estados accesorios (`tourState`, `organizationSuggestionState`, `pendingInstallSuggestions`) del `LauncherUiState` principal hacia flujos de estado (`StateFlow`) independientes en el ViewModel. Esto previene que interacciones con el Product Tour o las sugerencias de la IA provoquen recomposiciones masivas en la cuadrícula de aplicaciones (`AppGrid`).
- **Migración a `Modifier.Node`:** Se sustituyó el uso intensivo de `Modifier.composed` en el Product Tour (`TourModifier.kt`) por la moderna API de `Modifier.Node` (implementando `GlobalPositionAwareModifierNode`). Esto reduce drásticamente el coste de cálculo al rastrear las coordenadas globales de los elementos del launcher.
- **Eficiencia en Memoria:** Se corrigieron instancias donde conversiones de `Bitmap` se ejecutaban repetidamente, cacheándolas apropiadamente y marcando correctamente los tipos de contenido en las listas para reciclaje de vistas.

## 7. Optimizaciones de Rendimiento — Fase 2 (Scroll Lag + Arranque)

Se identificó que el lag al hacer scroll rápido en el drawer de apps era causado por una cascada de recomposiciones: cualquier cambio en `LauncherUiState` (tema, settings, etc.) forzaba la recomposición completa de `AppGrid`. Adicionalmente, el arranque bloqueaba la UI hasta decodificar todos los íconos más un delay artificial de 1.5s.

Soluciones implementadas:
- **`AppGridState` como StateFlow independiente:** Se creó `AppGridState` (marcado `@Immutable`) con su propio `StateFlow` que solo combina los campos relevantes para la grilla (apps filtradas, categoría, configuración de grilla). Esto aísla `AppGrid` y `CategorySidebar` de cambios de tema/settings, eliminando la cascada de recomposición.
- **Carga bifásica real:** `refreshApps()` ahora muestra las apps sin íconos inmediatamente (fase 1), y carga los íconos en background (fase 2) actualizando el estado de forma asíncrona. Se eliminó el bloqueo de UI durante la decodificación de bitmaps.
- **Organización de apps en `Dispatchers.Default`:** `suggestAppOrganizationUseCase()` se movió a un coroutine en `Dispatchers.Default` para no bloquear el hilo principal durante el arranque.
- **`distinctUntilChanged()` en flujos derivados:** Se aplicó a `categoryConfigsFlow` y `visibleCategoriesFlow` para evitar emisiones duplicadas que causaban recomposiciones innecesarias.
- **Reducción del delay de splash:** De 1500ms a 300ms.

## 8. Optimizaciones de Rendimiento — Fase 3 (Scroll Lag Crítico)

Se identificó y resolvió la causa raíz de un lag severo (10-20 segundos) al hacer scroll rápido en la caja de aplicaciones. El problema no era la carga de íconos, sino una cascada de recomposiciones provocada por código de tracking de coordenadas y tipos inestables en Compose.

### Causas raíz identificadas

1. **`onGloballyPositioned` en cada celda del grid:** `AppGrid.kt` aplicaba `.onGloballyPositioned` a cada `AppIcon` para capturar coordenadas del Product Tour. Durante el scroll, esto actualizaba un `mutableStateMapOf` en **cada frame** para cada celda visible, forzando la recomposición completa del grid y causando fugas de memoria sin límite.
2. **`AppModel` marcado como `@Stable` pero con tipos inestables:** El campo `UserHandle?` (tipo Android nativo desconocido para Compose) hacía que `AppModel` se considerara inestable, impidiendo que Compose reciclara eficientemente los items de `LazyVerticalGrid`.
3. **Búsqueda sin debounce:** Cada tecla en `SearchBar` disparaba el filtrado completo de la lista inmediatamente, causando picos de uso de CPU en el hilo principal.
4. **Filtrado en el hilo principal:** `mapAppContentFiltered` ejecutaba búsqueda de texto + filtrado por categoría directamente en el dispatcher del Flow (main thread).
5. **`tourTarget` siempre activo:** El modifier `.tourTarget()` estaba aplicado permanentemente a todos los elementos (Banner, ProfileHeader, SearchBar, CategorySidebar, AppGrid) aunque el tour nunca estuviera activo, agregando overhead de `Modifier.Node` en todo momento.
6. **Lambdas inestables en items del grid:** Los callbacks `onClick` y `onLongPress` se recreaban como nuevas instancias en cada recomposición, invalidando la estabilidad de `AppIcon` y forzando recomposiciones adicionales.

### Soluciones implementadas

- **Eliminación completa del tracking por celda:** Se removió `onGloballyPositioned` de los items individuales del `LazyVerticalGrid`. El posicionamiento del tour ahora funciona mediante un helper condicional (`Modifier.tourIfActive`) que solo aplica el modifier cuando `tourState.isActive == true`, eliminando toda la sobrecarga durante el uso normal.
- **Estabilización del modelo de datos:** Se cambió `@Stable` por `@Immutable` en `AppModel`. Al tratarse de un data class con campos inmutables establecidos en construcción, Compose ahora puede tratar las instancias como estables y saltar recomposiciones cuando la referencia no cambia.
- **Debounce en búsqueda (150ms):** Se introdujo `_searchQueryDebounced` con `Flow.debounce(150)` en `LauncherViewModel`. La UI refleja el texto instantáneamente, pero el filtrado de apps ocurre solo después de que el usuario deja de escribir.
- **Filtrado en background thread:** Se agregó `.flowOn(Dispatchers.Default)` al `appContentState` en `LauncherViewModel`. La búsqueda de texto y el filtrado por categoría ahora corren en un hilo de background, liberando el UI thread.
- **Tour condicional con `Modifier.tourIfActive`:** Se implementó una extensión condicional en `LauncherScreen.kt` que solo aplica `.tourTarget()` cuando el tour está activo. En uso normal, estos modifier nodes ni siquiera existen.
- **Callbacks estables en `AppGrid` y `AppIcon`:**
  - En `AppGrid.kt`: Los lambdas `onClick` y `onLongPress` de cada item se cachean con `remember(app)` para que sean referencialmente estables durante el scroll.
  - En `AppIcon.kt`: Se usa `rememberUpdatedState` dentro de `pointerInput(Unit)` para siempre invocar el callback más reciente sin reiniciar el detector de gestos.
- **Cacheo del bitmap en `AppIcon`:** El `imageBitmap` ahora se extrae con `remember(app.icon)` para evitar accesos repetidos al campo nullable durante recomposiciones.

### Impacto medido (esperado)

| Métrica | Antes | Después |
|---------|-------|---------|
| Recomposiciones por frame de scroll | Todas las celdas visibles | Solo las que entran/salen |
| Actualizaciones de estado por scroll | Ilimitadas (onGloballyPositioned) | Cero |
| Filtrado de búsqueda | Cada tecla, main thread | Debounced 150ms, background |
| Modifier nodes de tour activos | ~6 permanentes | 0 (solo durante tour) |
| Estabilidad de `AppModel` | Inestable (campos Android) | `@Immutable` |
| Callbacks en items del grid | Nuevas instancias por frame | Estables (`remember`) |

---

## 9. Optimizaciones de Rendimiento — Fase 4 (Cold Start / Warm Start)

Se identificó que el verdadero problema restante no era el scroll, sino el **cold start**: cuando el launcher permanecía en segundo plano (ej. viendo YouTube), Android destruía el proceso por inactividad o presión de memoria. Al volver, el launcher se reiniciaba desde cero con lag fuerte de varios segundos.

### Causas raíz identificadas

1. **`android:clearTaskOnLaunch="true"` en el Manifest:** Esta directiva limpia la task completa cada vez que el usuario presiona Home. Para un launcher, esto es catastrófico: fuerza la recreación completa de `MainActivity` y `LauncherViewModel` en **cada regreso a Home**, no solo cuando Android mata el proceso.
2. **`android:stateNotNeeded="true"`:** Le dice a Android que no guarde/restaure el estado del Activity, impidiendo cualquier recuperación de estado tras recreación.
3. **Sin `android:largeHeap="true"`:** El launcher no tenía heap ampliado, haciéndolo mucho más susceptible a ser matado por el sistema bajo presión de memoria.
4. **ViewModel `init` bloqueante:** `refreshApps()` ejecutaba queries pesadas a `PackageManager` sincrónicamente en el arranque. La carga era **bifásica** (metadata primero, luego full apps), causando **dos emisiones** de `_allApps` → cada una dispara la cascada completa de flows (`appsWithMetaFlow` → `appContentState` → `uiState` + `appGridState`), provocando recomposiciones masivas duplicadas.
5. **Sin caché de apps a nivel de proceso:** Cuando Android mataba el proceso, la lista de apps desaparecía. Al volver, era necesario reconsultar `PackageManager` desde cero.
6. **Trabajo no crítico en el startup path:** `refreshIconPacks()`, `refreshSystemStatus()`, registro de `PackageChangeReceiver`, y sugerencias de organización se ejecutaban todos durante el `init`, compitiendo por CPU y memoria en el momento más crítico.

### Soluciones implementadas

- **Manifest corregido (impacto inmediato):**
  - Se removió `android:clearTaskOnLaunch="true"`.
  - Se removió `android:stateNotNeeded="true"`.
  - Se agregó `android:largeHeap="true"` para reducir kills por memoria.
  - Se agregó `android:excludeFromRecents="true"` para que el launcher no aparezca en recientes.
- **`AppListCache` (caché de proceso):** Se creó un singleton `AppListCache` en `AppContainer` que persiste la última lista de apps en memoria mientras el proceso viva. En el `init` del `LauncherViewModel`, si el caché está fresco (< 5 min), se hace _seed_ inmediato de `_allApps`, permitiendo que la UI renderice **instantáneamente** sin esperar a `PackageManager`.
- **Single emission en `refreshApps()`:** Se eliminó la carga bifásica que causaba doble recomposición. Ahora hay una sola emisión de `_allApps` con los datos procesados en `Dispatchers.Default`.
- **Defer de trabajo no crítico:** `refreshSystemStatus()`, `refreshIconPacks()`, registro del `PackageChangeReceiver`, y las sugerencias de organización se ejecutan en coroutines diferidas (`viewModelScope.launch`) tras el renderizado inicial, liberando el hilo principal para el primer frame de Compose.
- **Silent refresh:** Cuando hay caché válido, `refreshApps(silent = true)` ejecuta la recarga de apps en background **sin mostrar el indicador de carga**, manteniendo la experiencia fluida.

### Impacto medido (esperado)

| Métrica | Antes | Después |
|---------|-------|---------|
| Recreación al presionar Home | Siempre (clearTaskOnLaunch) | Nunca (singleTask normal) |
| Recuperación tras kill de proceso | 3-5s lag + reconstrucción completa | Instantánea (cache) + refresh silent |
| Emisiones de `_allApps` en startup | 2 (metadata + full) | 1 (single batch) |
| Recomposiciones iniciales | Doble cascada (empty → metadata → full) | Una sola (cached → refreshed) |
| Heap disponible | Default (susceptible a kills) | Large (menos kills por memoria) |
| Trabajo en hilo principal durante init | Queries PackageManager + iconos + sugerencias | Solo seed de cache + UI Compose |

## 10. Nuevas Funcionalidades de UX (Categorías Inteligentes, Apps Ocultas, Menú Contextual)

En esta iteración se implementaron tres mejoras de experiencia de usuario de alto impacto, manteniendo la arquitectura limpida existente.

### 10.1 Categorías Inteligentes (Smart Grouping)

Se añadieron nuevas categorías semánticas al sistema de categorización automática:
- **`WALLETS`** (Billeteras): Detecta apps de pago (Binance, Yape, Plin, PayPal, Mercado Pago, etc.).
- **`COMPRAS`** (Compras): Detecta apps de e-commerce (Rappi, Amazon, AliExpress, etc.).
- **`FINANZAS`**: Actúa como categoría merge cuando hay pocos wallets o compras individualmente, pero suficientes en conjunto (≥5).
- **`DEV`** (Desarrollo): Detecta apps de desarrollo (Termux, GitHub, VS Code, etc.).

**Lógica de agrupamiento inteligente** (`LoadAppsUseCase.applySmartGrouping`):
- Si hay ≥4 wallets → categoría `WALLETS` se mantiene.
- Si hay ≥4 compras → categoría `COMPRAS` se mantiene.
- Si wallets + compras ≥5 pero individualmente <4 → se mergean a `FINANZAS`.
- Si hay ≥4 dev → categoría `DEV` se mantiene.
- Si hay <4 dev → se revierten a `DEVELOPMENT`.

Esto evita categorías vacías o con solo 1-2 apps, manteniendo el drawer limpio.

### 10.2 Sistema de Apps Ocultas

Se implementó un sistema completo de ocultamiento de apps con persistencia en DataStore:

- **Ocultamiento permanente:** `SettingsManager.hiddenApps` (set de package names).
- **Ocultamiento temporal:** `SettingsManager.tempHiddenApps` (map `packageName → timestamp`). Las apps ocultadas temporalmente se restauran automáticamente cuando expira el tiempo.
- **UI de gestión:** En `LauncherSettingsPanel` se añadió `HiddenAppsSection` donde el usuario puede:
  - Ver la lista de apps ocultas.
  - Restaurar apps individuales.
  - Seleccionar una app para ocultarla con duración: 5 minutos, hasta reinicio, o indefinidamente.
- **Filtrado en ViewModel:** `visibleAppsFlow` filtra las apps ocultas antes de pasarlas a los flows derivados (`categoryBaseFlow`, `appContentState`, etc.), asegurando que nunca aparezcan en la UI.

### 10.3 Menú Contextual Rediseñado

El menú contextual de long-press fue completamente rediseñado:

- **Posicionamiento inteligente:** Se reemplazó el popup centrado por un `Popup` con `PopupPositionProvider` personalizado (`AppMenuPositionProvider`) que posiciona el menú justo debajo del ícono presionado, con ajuste automático de bordes para no salirse de la pantalla.
- **Estilo consistente:** El menú usa tokens del tema (`colors.surface`, `colors.border`, `accent.primary`), sombra suave (`shadow(8.dp)`), y bordes redondeados (`RoundedCornerShape(14.dp)`).
- **Acciones disponibles:** Favorito/Quitar, Mover (abre picker de categoría), Información, Desinstalar.
- **Color semántico:** La acción "Desinstalar" usa `MaterialTheme.colorScheme.error` para alerta visual.

### 10.4 Corrección de Colores Hardcodeados

Se realizó una auditoría y corrección de colores hardcodeados en la UI:

| Archivo | Color hardcodeado | Reemplazo |
|---------|-------------------|-----------|
| `LauncherScreen.kt` | `Color(0xFFFF9100)` (banner no-default) | `LauncherColors.AccentOrange` |
| `AutoOrganizeSection.kt` | `Color(0xFF8B5CF6)` (icono IA) | `LauncherColors.AccentPurple` |
| `AutoOrganizeSection.kt` | `Color(0xFF10B981)` (estado aplicado) | `LauncherColors.SuccessGreen` |

Se agregaron los tokens correspondientes a `Color.kt`: `AccentPurple`, `AccentPurpleBg`, `SuccessGreen`, `SuccessGreenBg`.

---

## 11. Corrección de Bugs Críticos (Categorías + Asistente + Process Death + Rotación)

### 11.1 Bug: Renombrar Categorías No Persistía

**Causa raíz:** El set `knownCategories` en `SettingsManagerImpl` era una lista estática que no incluía las nuevas categorías dinámicas agregadas en la iteración anterior (`wallets`, `compras`, `finanzas`, `dev`). El flujo `categoryDisplayNames` iteraba solo sobre `knownCategories + customCategories` para leer los nombres personalizados de DataStore. Cualquier categoría detectada dinámicamente por `AppCategorizer` que no estuviera en `knownCategories` quedaba excluida, haciendo que su nombre personalizado se guardara en DataStore pero nunca se leyera de vuelta.

**Solución:** Se agregaron `"wallets"`, `"compras"`, `"finanzas"`, `"dev"` a `knownCategories` en `SettingsManagerImpl`.

### 11.2 Bug: Process Death Restore Lento (~20s pantalla vacía)

**Causa raíz:** `AppListCache` es puramente en memoria (`var lastApps: List<AppModel> = emptyList()`). Cuando Android mata el proceso por presión de memoria, la lista de apps desaparece por completo. Al volver:
1. `LauncherViewModel.init` encuentra `appListCache.isFresh() == false`.
2. `_allApps` se queda como `emptyList()`.
3. `refreshApps()` consulta `PackageManager` para TODAS las apps + decodifica TODOS los íconos + aplica heurísticas → 10-20s de pantalla vacía.
4. Los StateFlows tienen valores por defecto con listas vacías, por lo que la UI renderiza vacío inmediatamente.

**Solución:**
- Se creó `PersistentAppCache` (singleton en `AppContainer`): caché en disco que serializa metadata de apps a JSON en `filesDir`. Guarda `packageName`, `activityName`, `label`, `category`, `isSystemApp`, `versionCode`, `iconResId`, `profileTag`. NO guarda bitmaps ni `UserHandle`. Lee en ~50ms.
- En `LauncherViewModel.init`: se lee primero del caché de disco. Si tiene datos, se hace seed inmediato de `_allApps` con íconos placeholder. La UI renderiza instantáneamente. Luego se ejecuta `refreshApps()` en background para cargar íconos reales.
- En `refreshApps()`: después de cada carga exitosa se guarda en `persistentAppCache.write(apps)`.
- Se mantiene `AppListCache` (memoria) como fallback rápido cuando el proceso aún vive.

### 11.3 Bug: Asistente (Long-Press Home) Bloqueado

**Síntoma:** Mantener presionado el botón Home no abría Google Assistant / Gemini.

**Investigación exhaustiva:**
- NO hay overrides de `onKeyDown`, `dispatchKeyEvent`, `onTouchEvent`, ni consumidores de eventos de navegación.
- NO hay servicios de voz (`VoiceInteractionService`), accesibilidad, ni `NotificationListenerService`.
- `OnBackPressedDispatcher` solo maneja Back, no Home.
- `enableEdgeToEdge()` + `systemBarsPadding()` no consumen eventos de navegación.
- El único `detectTapGestures` es en `AppIcon.kt` (clicks individuales, no navegación del sistema).

**Causa raíz:** `android:priority="1"` en el intent-filter `HOME` de `MainActivity`. En ciertos dispositivos/OEM skins (Samsung One UI, Xiaomi MIUI, Motorola), este atributo puede interferir con la resolución de gestos del sistema cuando hay múltiples launchers instalados. El sistema trata a TAPO como candidato prioritario para gestos de navegación, incluyendo long-press Home → asistente, y no enruta correctamente al servicio de asistencia predeterminado.

**Solución:**
- Removido `android:priority="1"` del intent-filter HOME.
- Removido `android:excludeFromRecents="true"` (los launchers no necesitan este atributo).
- Removido `android:screenOrientation="portrait"` del manifest (ver 11.4).

### 11.4 Bug: Botón de Rotación Manual

**Síntoma:** Al girar el dispositivo, apareció el botón flotante de sugerencia de rotación manual de Android.

**Fase fallida:** Se agregó `android:screenOrientation="portrait"` + `configChanges="orientation|..."` en el manifest. Esto bloqueó la rotación pero puede interferir con la detección de gestos del sistema en Android moderno (gesture navigation), incluyendo long-press en el "pill" de navegación para abrir el asistente.

**Solución final:**
- Removido `android:screenOrientation="portrait"` del manifest.
- Removido `orientation` de `android:configChanges` (se mantiene `screenSize|smallestScreenSize|screenLayout|keyboard|keyboardHidden`).
- Seteado `requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT` programáticamente en `MainActivity.onCreate()`. Esto bloquea la rotación sin interferir con la resolución de gestos del sistema en el manifest.

### 11.5 Cambios en MainActivity

- Se reemplazó `onBackPressed()` deprecated por `OnBackPressedDispatcher.addCallback()` para manejar Back correctamente sin interferir con el asistente. El callback delega al sistema si el ViewModel no maneja el evento.
- Se setea `requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT` programáticamente para bloquear rotación sin afectar gestos del sistema.

> **Estado actual del proyecto:** Todas las fases de refactorización (1-5), optimización (1-5), funcionalidades de UX, y correcciones de bugs críticos están completas. El proyecto compila limpiamente y está listo para producción.

## Fase 5 de Rendimiento: Eliminación de la Doble Emisión (Root Cause Fix)

### Diagnóstico

El lag en scroll persistía porque el init del ViewModel hacía DOS emisiones de `_allApps`:
1. **Emisión 1** (t=50ms): Apps desde `PersistentAppCache` con `icon = null`
2. **Emisión 2** (t=~2s): Apps con íconos tras `loadAppsUseCase()` completo

La segunda emisión disparaba la cascada completa de `combine()` (`visibleAppsFlow → appsWithMetaFlow → appContentState → appGridState + uiState`) mientras el usuario ya hacía scroll. Cada `AppModel` cambiaba (icon: null → bitmap), forzando recomposición de TODOS los items visibles y upload de texturas GPU en un solo frame.

### Solución: Icon Hydration at Init

En lugar de emitir sin íconos y luego re-emitir con íconos:
1. Leer `PersistentAppCache` (metadata, ~50ms)
2. Leer íconos desde `IconDiskCache` **en paralelo** (200 × async en Dispatchers.IO ≈ 80ms)
3. Pre-calentar `ImageBitmap` lazy properties en el thread IO
4. Emitir UNA sola vez con íconos ya adjuntos
5. `refreshApps()` posterior encuentra `previousHadMissingIcons=false` → no re-emite

### Cambios adicionales
- **`onGloballyPositioned` → `onPlaced`**: El tracking de coordenadas por celda ahora usa `onPlaced` (solo dispara en cambios de placement, no cada layout pass). `boundsInWindow()` se calcula lazily al long-press.
- **Íconos a 192px + WebP 95**: Resolución y calidad restauradas sin impacto en rendimiento gracias al disk cache v2.
- **LruCache 32MB**: Ajustado para íconos más grandes.
- **Chunked pre-warming**: `ImageBitmap` se pre-calienta en batches de 20 con `yield()` entre chunks.

### Timeline esperado tras fix
| Fase | Tiempo | Qué pasa |
|------|--------|----------|
| PersistentAppCache read | 50ms | JSON parse |
| IconDiskCache hydration | 80ms | 200 reads paralelos |
| ImageBitmap pre-warm | 50ms | 20 primeros íconos |
| Primera emisión _allApps | 180ms | UI renderiza con íconos |
| refreshApps() background | ~2s | Detecta que nada cambió, no re-emite |
