# AGENTS.md — TAPO-Launcher

> Android launcher app built with Kotlin + Jetpack Compose. Single module, no tests.

## Project structure

- **Real Gradle root:** `app/` (not the repo root).
  - `gradlew`, `settings.gradle.kts`, and `gradle/libs.versions.toml` live here.
  - The module itself is `app/app/` (yes, nested).
- **Repo root** only holds `README.md`, `.github/`, `build-release.sh`, and `releases/`.

## Build

Run all Gradle commands from the `app/` directory:

```bash
cd app
./gradlew assembleDebug
./gradlew assembleRelease   # minify + shrinkResources enabled
```

- `compileSdk = 35`, `minSdk = 26`, Java 17 target.
- Kotlin 2.1.0 with Compose compiler plugin.
- `build-release.sh` (repo root) is **broken**: it `cd`s to the repo root and calls `./gradlew`, but `gradlew` lives in `app/`. It also builds debug, not release.

## CI / Release workflow

- `.github/workflows/release.yml` triggers on tags matching `v*`.
- CI sets `working-directory: app`, deletes `local.properties`, and builds `./gradlew assembleDebug`.
- **CI builds debug APKs for releases**, not release APKs.
- APK artifact path inside `app/`: `app/build/outputs/apk/debug/app-debug.apk`.
- CI renames it to `VoidLauncher_<tag>.apk` and attaches it to a GitHub Release.

## Codegen / generated code

- `app/app/build/` contains generated manifest intermediates; ignore it (already in `.gitignore`).
- No other codegen (no Room, no Dagger, no protobuf).

## Testing

- No unit tests or instrumentation tests exist (`src/test/` and `src/androidTest/` are absent).
- Do not create test tasks or expect existing test suites.

## Notable app behavior

- Manifest declares `QUERY_ALL_PACKAGES` permission and `HOME` intent-filter to act as a launcher.
- Also declares a `NotificationListenerService` (`LauncherNotificationService`).
- Two-phase loading: `AppRepository` emits app metadata immediately, then resolves icons asynchronously via `LruCache`.
- Icon packs are resolved by parsing each pack's `appfilter.xml` once and caching the component→drawable map.
- `WorkProfileManager` uses `crossProfileIntentResolution` APIs; changes here require testing on a device with a real work profile.

## Naming inconsistencies to be aware of

- Repo folder / README: **TAPO-Launcher**
- Gradle project name (`settings.gradle.kts`): **TAPO-Launcher**
- Application ID / package: `dev.vive.kdelauncher`
- CI release name: **TAPO-Launcher**

When editing docs or CI, prefer aligning to the repo name (`TAPO-Launcher`) unless the user specifies otherwise.


