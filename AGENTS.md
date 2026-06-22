# AGENTS.md

Compact guide for OpenCode agents working in this repo. Read this first, then `TECHNICAL_DOCUMENTATION.md` for the full architecture deep-dive (entity tables, nav graph, API contracts, version history).

## Project

Single-module Android app (`:app` only, no monorepo). Jetpack Compose + Material 3, MVVM + Clean Architecture, Hilt DI, Room, Media3 ExoPlayer. Package: `com.musicplayer.localmusicplayer`. minSdk 26 / target+compile 35. Current version: 1.3.0 (versionCode 7).

Layers under `app/src/main/java/com/musicplayer/localmusicplayer/`:
- `domain/` — pure Kotlin, no Android deps. Models, repository interfaces, use cases.
- `data/` — Room, MediaStore, DataStore, Retrofit. Repository implementations.
- `presentation/` — Compose screens + ViewModels (StateFlow<UiState>). Navigation via Compose NavHost; routes defined in `Screen.kt` sealed class.
- `service/` — ExoPlayer playback + MediaSessionService.
- `di/` — Hilt modules.
- `util/` — Constants, parsers, locale helper, audio tag reader.

## Build

Windows repo — use `gradlew.bat`, not `./gradlew`:

```
gradlew.bat assembleDebug
gradlew.bat assembleRelease
gradlew.bat lint
gradlew.bat :app:dependencies
```

JDK 21 is required to run Gradle 8.11.1 / AGP 8.7.3. Set `JAVA_HOME` to a JDK 21 install before invoking Gradle. Bytecode target is 17 (`compileOptions`/`jvmTarget` in `app/build.gradle.kts`) — do not raise it without reason.

`local.properties` (gitignored) must contain `sdk.dir` pointing at the Android SDK. Don't commit it.

APK output: `app/build/outputs/apk/debug/app-debug.apk` (release is unsigned — see below).

## Verification

There is **no test suite** (no `app/src/test/`, no `app/src/androidTest/`) and **no CI**. Do not assume `test`/`check` tasks validate anything meaningful. Verification = successful `assembleDebug` build + manual run on a device/emulator.

`lint` is configured with `abortOnError = false` and `checkReleaseBuilds = false` — it will **not** fail the build. Don't rely on lint as a gate.

## Database gotchas

`AppDatabase` is at version 3, `exportSchema = false`, and `app/schemas/` is gitignored — there are **no schema JSON baselines** checked in.

`DatabaseModule.provideDatabase` uses `fallbackToDestructiveMigration()`. This means:
- Changing any entity/table will **wipe all user data** (playlists, playlist-song links) on app upgrade.
- You do not need to write migrations, but flag data loss in PR descriptions. If preserving user data matters, switch to explicit `Migration` objects before bumping the version.
- DB file: `localmusicplayer.db`.

## Release build

`isMinifyEnabled = false` in the release build type — R8/ProGuard shrinking is **off**. `proguard-rules.pro` exists but is not applied. There is **no signing config**, so `assembleRelease` produces `app-release-unsigned.apk`. Enabling minify requires verifying the existing keep rules (Hilt, Room entities, domain models) cover any new reflective code.

## Playback architecture

`MusicPlaybackService` (MediaSessionService) is the **sole owner of ExoPlayer**. `PlaybackManager` is a Hilt `@Singleton` that outlives the service — it delegates control calls to the service's `svc*` methods and mirrors the service's StateFlows for UI consumption. ViewModels observe PlaybackManager's StateFlow directly.

**ExoPlayer's native repeat mode is OFF** (`Player.REPEAT_MODE_OFF`). The app implements repeat/shuffle logic manually in the service's `STATE_ENDED` handler and queue management. Do not enable ExoPlayer's built-in repeat — it would conflict. Default repeat mode is "repeat all"; playback does not stop at end of list.

Skip-to-previous restarts the current track if position > 3 seconds; only goes to previous track otherwise.

Position updates tick every 250ms (`Constants.POSITION_UPDATE_INTERVAL_MS`).

## MediaStore scan

`MediaStoreDataSource` filters with `IS_MUSIC != 0` and `DURATION > 15000` (15s minimum). The `TRACK` column is parsed as `discNumber = raw / 1000`, `trackNumber = raw % 1000`. Preserve these filters when modifying scan logic.

## Locale

Default language is **Chinese** (`Language.Chinese`). `MusicPlayerApplication.onCreate` uses `runBlocking` to load the saved language from DataStore **before** the Activity starts, so locale is applied correctly. This synchronous read is intentional — don't refactor it to suspend without preserving ordering. Language switching goes through `LocaleHelper.applyLocale`.

## Theme

Custom seed-based color scheme — **not** Material You / dynamic color. `ThemeMode` (System/Light/Dark) and `ThemeColor` (seed color) are persisted via DataStore through `ThemeRepository`. `MusicPlayerTheme` in `presentation/theme/Theme.kt` builds light/dark `ColorScheme` from the seed. The theme composable accesses `ThemeRepository` via `Application` cast, not Hilt injection.

## External lyrics API

Online lyrics search hits a third-party endpoint (`https://music-api.gdstudio.xyz/api.php`) — not owned by this repo. Sources: netease/tencent/kuwo/spotify/apple. `usesCleartextTraffic="true"` is set in the manifest. Don't assume the API is stable; failures should degrade gracefully to local `.lrc` files.

## Conventions

- Kotlin code style: `official` (set in `gradle.properties`).
- Annotation processing uses **KSP** (not kapt) for both Room and Hilt.
- Versions centralized in `gradle/libs.versions.toml` — add new deps there and reference via `libs.*`, except the Retrofit/OkHttp/Gson/Paging deps currently inlined in `app/build.gradle.kts`.
- Hilt modules live in `di/`; new bindings go there, not in random files.
- Domain layer must stay Android-free (no `android.*` imports in `domain/`).
- Image loading: **Coil 3** (`io.coil-kt.coil3:coil-compose`).
- Glance (`androidx.glance`) is declared as a dependency but no widget code exists yet.
