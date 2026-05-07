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
- **Perfiles Personal / Trabajo** — Detecta un Work Profile real y permite lanzar apps del perfil laboral.
- **Búsqueda instantánea** — Filtra apps por nombre o paquete.
- **Ajustes de interfaz** — Tema claro/oscuro, tamaño de íconos, columnas, fondo de íconos y etiquetas.
- **Icon packs** — Detecta packs instalados y resuelve íconos desde su `appfilter.xml`.
- **Personalización de categorías** — Cambia nombre, ícono y visibilidad de cada categoría.
- **Carga en dos fases** — Primero muestra metadatos y luego carga íconos en segundo plano.
- **Notificaciones** — Incluye un `NotificationListenerService` para mostrar estado relacionado con notificaciones.

## Arquitectura

```
app/
├── app/src/main/java/dev/vive/kdelauncher/
│   ├── TAPOLauncherApp.kt        ← `Application` y contenedor principal.
│   ├── AppContainer.kt           ← Inyección manual de dependencias.
│   ├── MainActivity.kt           ← Punto de entrada de la UI.
│   ├── SetDefaultLauncherActivity.kt ← Pantalla puente para fijar el launcher predeterminado.
│   ├── data/
│   │   ├── repository/           ← Repositorios e implementaciones de datos.
│   │   ├── model/                ← Modelos de apps, perfiles y categorías.
│   │   ├── usecase/              ← Lógica principal extraída del ViewModel.
│   │   ├── IconPackManager.kt    ← Resolución de icon packs.
│   │   ├── ProfileManager.kt     ← Favoritos y apps de trabajo.
│   │   ├── SettingsManager.kt    ← Configuración persistida con DataStore.
│   │   ├── WorkProfileManager.kt ← Apps del perfil laboral.
│   │   └── NotificationTracker.kt← Estado de notificaciones.
│   └── ui/
│       ├── LauncherViewModel.kt  ← Estado reactivo de la pantalla principal.
│       ├── screens/              ← Pantalla principal del launcher.
│       ├── components/           ← Grid, sidebar, buscador, header y panel de ajustes.
│       └── theme/                ← Colores, tipografía y tema Material3.
```

### Flujo de datos

1. **MainActivity** obtiene el `LauncherViewModel` desde el contenedor de la app y observa su estado.
2. El ViewModel delega en los **use cases** para cargar apps, lanzar actividades, marcar favoritos y leer estado del sistema.
3. La carga de apps ocurre en **dos fases**: primero llegan metadatos para pintar la UI rápido y después se resuelven los íconos.
4. Los cambios de configuración se guardan en **DataStore** y se reflejan en la interfaz con `StateFlow`.

### Optimizaciones de rendimiento

- **Carga en dos fases**: La interfaz aparece rápido y los íconos se completan después.
- **Caché en memoria**: Los íconos decodificados se guardan para evitar trabajo repetido.
- **Icon packs**: Cada `appfilter.xml` se parsea una sola vez y se reutiliza.
- **StateFlow**: El estado de la UI se mantiene reactivo sin lógica extra en la vista.

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

## Estado actual

- La **refactorización arquitectónica** ya está completada (Fases 1 a 5).
- Se implementaron las funcionalidades de **Temas de Color Dev** (Dracula, Tokyo Night, Vercel, Catppuccin, Nord, Gruvbox, One Dark) y **TAPO Labs** (auto-organización experimental de apps con IA a través de Groq, Gemini o Cohere).
- **NUEVO:** Se implementó exitosamente el **Product Tour** nativo (tutorial interactivo de primer uso) que guía al usuario por las funciones principales de la interfaz. Renderizado totalmente sobre el grid de Compose, cuenta con animaciones cinemáticas fluidas, transiciones de barrido, y soporte total adaptativo para modo claro y oscuro.
- El proyecto usa un **launcher real** con soporte para Work Profile, icon packs, ajustes visuales, y tematización dinámica.
- *Nota sobre pruebas:* Siguiendo las reglas del repositorio, el proyecto se mantiene ágil sin la creación activa de suites de tests para las capas de UI experimentales.

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
