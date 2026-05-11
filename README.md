# TAPO-Launcher

![Demo](pics/AppEnUso.gif)

Launcher para Android hecho con **Kotlin + Jetpack Compose**. Busca ser ligero, minimalista y práctico para uso diario, con soporte para categorías, perfiles de trabajo, icon packs y ajustes visuales.

<p align="center">
  <img src="pics/ScreenApp1_whiteTheme.png" width="200" alt="Pantalla principal - Tema claro" />
  <img src="pics/ScreenApp2.png" width="200" alt="Categorías y sidebar" />
  <img src="pics/ScreenApp3.png" width="200" alt="Panel de ajustes" />
</p>

## Características

- **Categorías configurables** — Favoritos, Desarrollo, Gráficos, Internet, Juegos, Multimedia, Sistema y Utilidades.
- **Categorías inteligentes** — Smart grouping automático para Wallets (Binance, Yape, PayPal), Compras (Rappi, Amazon), Finanzas (merge inteligente), y Dev (Termux, GitHub).
- **Perfiles Personal / Trabajo** — Detecta un Work Profile real y permite lanzar apps del perfil laboral.
- **Apps ocultas** — Oculta apps permanentemente o temporalmente (5 min, hasta reinicio, indefinido) con persistencia en DataStore. Gestión completa desde el panel de ajustes.
- **Menú contextual** — Long-press en cualquier app abre un menú flotante posicionado cerca del ícono con acciones: Favorito, Mover, Información, Desinstalar.
- **Búsqueda instantánea** — Filtra apps por nombre o paquete.
- **Ajustes de interfaz** — Tema claro/oscuro, tamaño de íconos, columnas, fondo de íconos y etiquetas.
- **Icon packs** — Detecta packs instalados y resuelve íconos desde su `appfilter.xml`.
- **Personalización de categorías** — Cambia nombre, ícono y visibilidad de cada categoría.
- **Carga optimizada** — Single emission en startup con caché de proceso (`AppListCache`) + caché persistente en disco (`PersistentAppCache`) para recuperación instantánea tras process death.
- **Warm start instantáneo** — Caché de lista de apps a nivel de proceso (`AppListCache`) y caché en disco (`PersistentAppCache`) para recuperación fluida tras estar en background o ser matado por el sistema.
- **Notificaciones** — Incluye un `NotificationListenerService` para mostrar estado relacionado con notificaciones.

## Arquitectura

```text
app/
├── app/src/main/java/dev/vive/kdelauncher/
│   ├── KDELauncherApp.kt         ← `Application` y punto de entrada.
│   ├── MainActivity.kt           ← Punto de entrada de la UI.
│   ├── SetDefaultLauncherActivity.kt ← Pantalla puente para fijar el launcher predeterminado.
│   ├── data/
│   │   ├── model/                ← Modelos de apps (`@Immutable`), perfiles y categorías.
│   │   ├── platform/             ← Gateways para interactuar con Android OS (PackageManager).
│   │   ├── provider/             ← Clientes HTTP/API para proveedores de IA (Labs).
│   │   ├── repository/           ← Implementaciones de repositorios y caches (DataStore, PersistentAppCache).
│   │   ├── IconPackManagerImpl.kt← Resolución de icon packs.
│   │   ├── ProfileManagerImpl.kt ← Favoritos y apps de trabajo.
│   │   ├── SettingsManagerImpl.kt← Configuración persistida.
│   │   └── WorkProfileManagerImpl.kt ← Apps del perfil laboral.
│   ├── di/
│   │   └── AppContainer.kt       ← Contenedor e inyección manual de dependencias.
│   │                               Incluye `AppListCache` (memoria) y `PersistentAppCache` (disco)
│   │                               para warm start instantáneo y recuperación tras process death.
│   ├── domain/
│   │   ├── repository/           ← Interfaces (contratos) de repositorios y Managers.
│   │   └── usecase/              ← Lógica principal y reglas de negocio (SRP).
│   ├── service/
│   │   └── PackageChangeReceiver.kt ← Receptor de broadcast de cambios en paquetes instalados.
│   └── ui/
│       ├── LauncherViewModel.kt  ← ViewModel con múltiples StateFlows independientes
│       │                             (uiState, appGridState, tourState) para aislar
│       │                             recomposiciones.
│       ├── LauncherUiStateMapper.kt ← Mapeador de proyección y filtrado de estado.
│       │                              El filtrado corre en `Dispatchers.Default`.
│       ├── screens/              ← Pantalla principal del launcher (`LauncherScreen`).
│       ├── components/           ← Grid (`LazyVerticalGrid` con keys + contentType),
│       │                             sidebar, buscador, header y panel de ajustes.
│       ├── theme/                ← Colores, tipografía y temas dinámicos (Dev Themes).
│       └── tour/                 ← Product Tour con modifier condicional
│                                     (solo activo cuando `tourState.isActive`).
```

### Flujo de datos

1. **MainActivity** obtiene el `LauncherViewModel` desde el contenedor de la app y observa sus estados independientes (`uiState`, `appGridState`, `tourState`).
2. El ViewModel delega en los **use cases** para cargar apps, lanzar actividades, marcar favoritos y leer estado del sistema.
3. La carga de apps se hace en **single emission**: el ViewModel inicializa `_allApps` desde `AppListCache` (warm start) y luego refresca en background. Todo el procesamiento (categorización, sorting) ocurre en `Dispatchers.Default`.
4. Los cambios de configuración se guardan en **DataStore** y se reflejan en la interfaz con `StateFlow`.
5. El filtrado de apps (búsqueda + categoría) se ejecuta en **`Dispatchers.Default`** mediante `.flowOn()`, evitando bloqueos en el hilo principal.
6. Trabajo no crítico (`refreshIconPacks`, `refreshSystemStatus`, sugerencias de organización) se **difiere** tras el primer frame para no competir por el hilo principal durante el arranque.

### Optimizaciones de rendimiento

- **Warm start con caché de proceso**: `AppListCache` guarda la última lista de apps en memoria. Si el proceso aún vive, el launcher se restaura instantáneamente.
- **Recuperación tras process death**: `PersistentAppCache` serializa metadata de apps a JSON en disco. Tras ser matado por el sistema, el launcher se restaura en ~50ms con nombres y categorías, luego refresca íconos en background.
- **Single emission en startup**: Eliminada la doble emisión de `_allApps` (metadata + full). Ahora hay una sola emisión procesada en `Dispatchers.Default`.
- **Manifest optimizado**: Removido `clearTaskOnLaunch` (que forzaba recreación de Activity en cada Home) y `stateNotNeeded`. Agregado `largeHeap` para reducir kills por memoria.
- **Caché en memoria**: Los íconos decodificados se guardan para evitar trabajo repetido.
- **Icon packs**: Cada `appfilter.xml` se parsea una sola vez y se reutiliza.
- **StateFlow independientes**: `uiState`, `appGridState` y `tourState` son flujos separados en el ViewModel para aislar recomposiciones.
- **Modelos `@Immutable`**: `AppModel` y `AppGridState` están marcados como inmutables, permitiendo que Compose recicle eficientemente los items del grid.
- **Grid eficiente**: `LazyVerticalGrid` usa `key` estable (`packageName + profileTag`) y `contentType` para reciclaje óptimo.
- **Callbacks estables**: Los lambdas `onClick`/`onLongPress` de cada celda se cachean con `remember(app)`; `AppIcon` usa `rememberUpdatedState` para evitar reiniciar el detector de gestos.
- **Búsqueda con debounce**: El query de búsqueda se debouncea a 150ms para evitar filtrado continuo mientras el usuario escribe.
- **Filtrado en background**: `mapAppContentFiltered` corre en `Dispatchers.Default` para no bloquear el hilo principal.
- **Tour condicional**: El modifier `.tourTarget()` solo se aplica cuando `tourState.isActive == true`, eliminando overhead de `Modifier.Node` durante el uso normal.
- **Sin tracking por celda**: Eliminado `onGloballyPositioned` de los items individuales del grid, que causaba recomposiciones masivas en cada frame de scroll.

## Tech stack

| Capa | Tecnología |
|------|-----------|
| Lenguaje | **Kotlin** 100% |
| UI | **Jetpack Compose** + Material3 |
| Arquitectura | **MVVM** + **use cases** + contenedor manual de dependencias |
| Gradle | **Kotlin DSL** + Version Catalog (`libs.versions.toml`) |
| Mínimo SDK | **API 26** (Android 8.0) |
| Target SDK | **API 35** (Android 15) |
| Compilación | Java 17, Compose BOM |

## Build

```bash
cd app

# Debug APK
./gradlew assembleDebug

# Producción firmada (minificado + shrinkResources)
./gradlew assembleRelease

# Usando el script helper
./build-release.sh
```

El APK generado estará en `app/app/build/outputs/apk/debug/` o `app/app/build/outputs/apk/release/`. El script `build-release.sh` copia el release final a `releases/`.

### Firma de release

Los builds `release` necesitan una keystore real para generar un APK instalable fuera de Android Studio.

Variables de entorno requeridas para `assembleRelease`:

```bash
export TAPO_RELEASE_KEYSTORE_PATH="/ruta/a/tu/release-keystore.jks"
export TAPO_RELEASE_STORE_PASSWORD="tu_store_password"
export TAPO_RELEASE_KEY_ALIAS="tu_alias"
export TAPO_RELEASE_KEY_PASSWORD="tu_key_password"
```

Secrets requeridos en GitHub Actions para publicar releases instalables:

- `ANDROID_RELEASE_KEYSTORE_BASE64`
- `ANDROID_RELEASE_STORE_PASSWORD`
- `ANDROID_RELEASE_KEY_ALIAS`
- `ANDROID_RELEASE_KEY_PASSWORD`

Para `ANDROID_RELEASE_KEYSTORE_BASE64`, sube el contenido base64 del archivo `.jks`:

```bash
base64 -w 0 release-keystore.jks
```

## Licencia

[Apache License 2.0](LICENSE)

## Privacidad y Permisos

Consulta el documento de [Permisos y Privacidad](PERMISSIONS.md) para conocer la justificación del uso del permiso `QUERY_ALL_PACKAGES` y otros permisos requeridos.
