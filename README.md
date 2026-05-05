# TAPO-Launcher

![Demo](pics/AppEnUso.gif)

Un launcher para Android con el estilo visual del menú de aplicaciones de **KDE Plasma**. Ligero, minimalista y altamente configurable.

<p align="center">
  <img src="pics/ScreenApp1_whiteTheme.png" width="200" alt="Pantalla principal - Tema claro" />
  <img src="pics/ScreenApp2.png" width="200" alt="Categorías y sidebar" />
  <img src="pics/ScreenApp3.png" width="200" alt="Panel de ajustes" />
</p>

## Características

- **Categorías estilo KDE** — Favoritos, Desarrollo, Gráficos, Internet, Juegos, Multimedia, Sistema y Utilidades.
- **Perfiles Personal/Trabajo** — Alterna entre perfiles con acentos teal/naranja. Soporte para Android Work Profile real.
- **Búsqueda instantánea** — Filtra apps por nombre o paquete.
- **Panel de ajustes** — Tema oscuro/claro, tamaño de íconos, columnas de cuadrícula, fondo de íconos y etiquetas.
- **Icon packs** — Detecta packs instalados (ADW, Nova, TeslaCoil, Atom) y aplica sus íconos automáticamente.
- **Personalización de categorías** — Renombra, cambia íconos y oculta categorías.
- **Carga en dos fases** — La UI se muestra al instante con metadatos; los íconos se cargan en segundo plano.

## Arquitectura

```
app/
├── app/src/main/java/dev/vive/kdelauncher/
│   ├── MainActivity.kt          ← Entry point. Inyecta ViewModel, maneja lifecycle.
│   ├── KDELauncherApp.kt        ← Application class.
│   ├── SetDefaultLauncherActivity.kt ← Trampolín para abrir ajustes de launcher predeterminado.
│   ├── data/
│   │   ├── model/
│   │   │   ├── AppModel.kt      ← Modelo de app + AppCategory + AppCategorizer.
│   │   │   └── Profile.kt       ← Perfil (Personal/Work) con colores de acento.
│   │   ├── repository/
│   │   │   └── AppRepository.kt ← Consulta PackageManager, cachea íconos (LruCache).
│   │   ├── IconPackManager.kt   ← Detecta y resuelve icon packs vía appfilter.xml.
│   │   ├── ProfileManager.kt    ← Persiste favoritos y apps de trabajo en SharedPreferences.
│   │   ├── SettingsManager.kt   ← Persiste toda la configuración del launcher.
│   │   └── WorkProfileManager.kt← Detecta Android Work Profile y lanza apps cross-user.
│   └── ui/
│       ├── LauncherViewModel.kt ← StateFlow reactivo, combine de 7 flujos → LauncherUiState.
│       ├── screens/
│       │   └── LauncherScreen.kt← Layout principal (banner → header → settings → search → grid).
│       ├── components/
│       │   ├── AppGrid.kt       ← LazyVerticalGrid con íconos.
│       │   ├── AppIcon.kt       ← Ícono individual con menú contextual.
│       │   ├── CategorySidebar.kt ← Barra lateral de categorías animada.
│       │   ├── SearchBar.kt     ← Campo de búsqueda con estilo KDE.
│       │   ├── ProfileHeader.kt ← Avatar + selector de perfil + botón de ajustes.
│       │   ├── LauncherSettingsPanel.kt ← Panel colapsable con todas las opciones.
│       │   └── IconResolver.kt  ← Mapea strings de íconos a ImageVector.
│       └── theme/
│           ├── Color.kt         ← Paletas dark/light + colores de acento.
│           ├── Theme.kt         ← MaterialTheme + CompositionLocals (accent, colors).
│           └── Type.kt          ← Escala tipográfica con mono y sans-serif.
```

### Flujo de datos

1. **MainActivity** crea `LauncherViewModel` y recolecta `uiState` (StateFlow).
2. **loadApps()** — Fase 1: consulta `AppRepository.getInstalledAppsMetadata()` → emite la lista sin íconos. Fase 2: carga todos los bitmaps en paralelo con async/awaitAll → emite la lista completa.
3. **combine** une 7 flujos (_allApps, search, category, profile, settings, iconPack, status) en un solo `LauncherUiState` reactivo.
4. La UI (composables) consume `LauncherUiState` y delega acciones al ViewModel, que persiste cambios y actualiza los MutableStateFlow correspondientes.

### Optimizaciones de rendimiento

- **Carga en dos fases**: La UI es usable en ~50ms con nombres y categorías; los íconos se cargan asíncronamente.
- **LruCache**: Los bitmaps decodificados se cachean en memoria (25% del heap, máximo 32 MB).
- **Icon packs**: El appfilter.xml se parsea una sola vez y se cachea por pack.
- **Compose StateFlow**: Las recomposiciones son mínimas gracias a la combinación eficiente de flujos.

## Tech stack

| Capa | Tecnología |
|------|-----------|
| Lenguaje | **Kotlin** 100% |
| UI | **Jetpack Compose** + Material3 |
| Arquitectura | **MVVM** con **AndroidViewModel** + **StateFlow** |
| Gradle | **Kotlin DSL** + Version Catalog (`libs.versions.toml`) |
| Mínimo SDK | **API 26** (Android 8.0) |
| Target SDK | **API 35** (Android 15) |
| Compilación | Java 17, Compose BOM 2024 |

## Build

```bash
# Debug APK
./gradlew assembleDebug

# Producción (minificado + shrinkResources)
./gradlew assembleRelease

# Usando el script helper
./build-release.sh
```

El APK generado estará en `app/app/build/outputs/apk/debug/` o en `releases/`.

## Licencia

[Apache License 2.0](LICENSE)
