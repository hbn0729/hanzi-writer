# PROJECT KNOWLEDGE BASE

**Generated:** 2026-02-25T15:15:00+08:00
**Commit:** e3e2eb6
**Branch:** main
**Commit:** 9baef9d
**Branch:** main

## OVERVIEW
Android Hanzi learning app (Kotlin + Compose + Room + manual DI). Core domain is stroke-writing practice with local assets and a custom Python asset-generation pipeline. UI uses a claymorphism design system with SeniorTheme (large fonts for elderly users).

## STRUCTURE
```text
./
├── app/                          # Android application module (:app)
│   ├── src/main/java/com/hanzi/learner/
│   │   ├── app/                  # app wiring, DI modules, navigation, theme
│   │   ├── features/             # feature modules (home/practice/admin/common)
│   │   ├── data/                 # Room + repositories + models
│   │   ├── character_writer/     # stroke rendering/matching engine
│   │   └── speech/               # system TTS (simplified, no model management)
│   ├── src/main/assets/          # generated character data + static assets
│   └── src/test/java/com/hanzi/learner/
├── data/                         # source datasets for asset generation
└── tools/                        # asset pipeline scripts (Python)
```

## WHERE TO LOOK
| Task | Location | Notes |
|---|---|---|
| Add/modify screens | `features/*/ui` | UI must stay free of repository impl/DAO imports |
| Change feature flow | `features/*/viewmodel` + `features/*/domain` | Respect architecture guardrails test |
| Dependency wiring | `app/AppContainer.kt`, `app/AppModules.kt`, `app/Dependencies.kt` | Modular composition root (CoreDataModule/PracticeModule/AdminModule) |
| Feature dependency interfaces | `app/Dependencies.kt` | `HomeFeatureDependencies`, `PracticeFeatureDependencies`, `AdminFeatureDependencies` |
| Data persistence | `data/local` + `data/repository` | Contracts first, impl second |
| Writing algorithm | `character_writer/match`, `character_writer/render` | Performance-sensitive paths |
| Asset pipeline | `tools/generate_assets.py`, `app/build.gradle.kts` task `generateHanziAssets` | Source-of-truth is `/data`, not generated assets |
| Speech/TTS | `speech/` | System TTS only; no model management |
| Theme/styling | `app/theme/Theme.kt`, `app/theme/Claymorphism.kt` | Claymorphism design system + SeniorTheme |
| Architecture policy | `app/src/test/.../architecture/ArchitectureGuardrailsTest.kt` | Encodes forbidden dependency directions |

## CODE MAP
| Symbol | Type | Location | Refs | Role |
|---|---|---|---:|---|
| `MainActivity` | class | `.../MainActivity.kt` | high | Android launcher entry |
| `AppNavGraph` | function | `.../app/AppNavGraph.kt` | high | app route composition (home/practice/review/admin) |
| `AppContainer` | class | `.../app/AppContainer.kt` | high | DI composition root, implements all feature dependency interfaces |
| `AppModules` | module classes | `.../app/AppModules.kt` | high | CoreDataModule + PracticeModule + AdminModule |
| `Dependencies` | interfaces | `.../app/Dependencies.kt` | high | HomeFeatureDependencies, PracticeFeatureDependencies, AdminFeatureDependencies, AppDependencies |
| `HanziLearnerTheme` | composable | `.../app/theme/Theme.kt` | high | default theme with clay color palette |
| `SeniorTheme` | composable | `.../app/theme/Theme.kt` | high | large-font theme for practice screen (elderly-friendly) |
| `claymorphism` | Modifier ext | `.../app/theme/Claymorphism.kt` | high | soft 3D clay-style modifier (shadow+border+rounded) |
| `HomeScreen` | composable | `.../features/home/ui/HomeScreen.kt` | medium | home screen with practice/review buttons |
| `PracticeScreen` | composable | `.../features/practice/ui/PracticeScreen.kt` | high | stroke practice UI with SeniorTheme + claymorphism |
| `PracticeViewModel` | class | `.../features/practice/viewmodel/PracticeViewModel.kt` | high | practice state orchestration |
| `AdminScreen` | composable | `.../features/admin/ui/AdminScreen.kt` | medium | admin tab host (5 tabs: 总览/字管理/学习数据/设置/备份) |
| `AdminTabRoutes` | route composables | `.../features/admin/ui/AdminTabRoutes.kt` | medium | admin tab route wiring (OverviewTabRoute, CharacterManagementTabRoute, etc.) |
| `AdminViewModelFactories` | class | `.../features/admin/ui/AdminViewModelFactories.kt` | medium | factory collection for all admin ViewModels |
| `ArchitectureGuardrailsTest` | test class | `.../architecture/ArchitectureGuardrailsTest.kt` | high | enforces layer boundaries |
| `TtsSpeakerContract` | interface | `.../speech/contract/TtsSpeakerContract.kt` | medium | TTS abstraction (speak, speakCharacterAndPhrase, stop, shutdown) |
| `SystemTtsSpeaker` | class | `.../speech/internal/SystemTtsSpeaker.kt` | medium | Android system TTS with Chinese locale auto-detection |
| `rememberTtsSpeaker` | composable | `.../speech/TtsSpeakerComposables.kt` | medium | Compose TTS lifecycle helper |

## MUST DO (ENFORCED)
- Before each coding session, query relevant library docs via Context7:
  1. `context7_resolve-library-id(libraryName="<lib>", query="<task description>")` → get library ID
  2. `context7_query-docs(libraryId="<id>", query="<specific question>")` → fetch docs
- Example: For Room database work, first call `context7_resolve-library-id(libraryName="room", query="Room database migration")`, then `context7_query-docs(libraryId="/androidx/room", query="how to write migration")`

## CONVENTIONS
- Prefer contract/port types at boundaries (`*Contract`, provider interfaces) over concrete implementations.
- Keep feature layering explicit: `ui -> viewmodel -> domain -> data contracts`.
- Repository implementations live in `data/repository` (and admin-specific repository adapters); UI/ViewModel layers consume abstractions.
- Time source indirection exists (`TimeProvider`); avoid direct `LocalDate.now()` usage outside provider.
- Generated character assets live under `app/src/main/assets/char_data` and are produced via pipeline.
- Practice screen uses `SeniorTheme` wrapper for elderly-friendly large typography.
- UI components use `claymorphism()` and `clayClickable()` modifiers for consistent clay-style appearance.
- Feature dependencies are injected via typed interfaces (`*FeatureDependencies`), not direct `AppContainer` access.

## ANTI-PATTERNS (THIS PROJECT)
- Do not import `AppContainer` directly in feature UI screens; use `*FeatureDependencies` interfaces.
- Do not import concrete `RepositoryImpl`/DAO types into UI/ViewModel layers.
- Do not import Compose APIs into `domain`/`application` layers.
- Do not manually patch generated files in `app/src/main/assets/char_data` as primary fix; patch source data/pipeline.
- Do not bypass feature boundaries by cross-importing feature UI internals.
- Do not add TTS model download/selection features — removed intentionally; system TTS only.

## PERFORMANCE
- **Baseline Profile**: `app/src/main/baseline-prof.txt` pre-compiles startup/practice hot paths via AOT at install time.
- **Async DI Initialization**: `HanziLearnerApplication` builds `AppContainer` on `Dispatchers.IO` to prevent main-thread blocking.
- **Serial I/O (PWR-02)**: Bulk operations use `Dispatchers.IO.limitedParallelism(1)` to prevent DB contention in `AdminCharacterViewModel`.
- **Smart Debouncing (PWR-03)**: Search inputs debounced 150ms; "Load More" actions remain immediate.
- **Manual Paging**: Admin character list (3000+ items) uses `displayCount` state for lazy loading (20-200 items at a time).
- **LRU Caching**: `AssetCharacterRepository` caches 40 most-recent character stroke data to avoid repeated file I/O.
- Large lists (e.g., character management with 3000+ chars) use manual paging to avoid memory pressure.
- See `features/admin/AGENTS.md` for character list paging details.
- Large lists (e.g., character management with 3000+ chars) use manual paging to avoid memory pressure.
- See `features/admin/AGENTS.md` for character list paging details.

## UNIQUE STYLES
- Manual DI with modular composition: `CoreDataModule`, `PracticeModule`, `AdminModule` behind API interfaces, assembled in `AppContainer`.
- Architecture is guarded with unit tests, not documentation-only guidance.
- Feature-first packaging with explicit `domain`, `ui`, `viewmodel` subpackages.
- Claymorphism design system: soft shadows, thick borders (3px), rounded corners (16-24dp), clay color palette.
- Dual theme: `HanziLearnerTheme` (default) + `SeniorTheme` (practice screen, large fonts for elderly).
- **Performance Patterns**: PWR-02 (serial I/O), PWR-03 (debouncing), PWR-07/08 (async init) — tag perf-sensitive code.
- Manual DI with modular composition: `CoreDataModule`, `PracticeModule`, `AdminModule` behind API interfaces, assembled in `AppContainer`.
- Architecture is guarded with unit tests, not documentation-only guidance.
- Feature-first packaging with explicit `domain`, `ui`, `viewmodel` subpackages.
- Claymorphism design system: soft shadows, thick borders (3px), rounded corners (16-24dp), clay color palette.
- Dual theme: `HanziLearnerTheme` (default) + `SeniorTheme` (practice screen, large fonts for elderly).

## COMMANDS
```bash
# Build/test/lint
./gradlew app:assembleRelease
./gradlew app:testDebugUnitTest
./gradlew app:lint
./gradlew app:check

# Asset pipeline
./gradlew app:generateHanziAssets
```

## NOTES
- `rg`/`python` may be unavailable in some local shells; rely on Gradle and Kotlin toolchain for core workflows.
- `ArchitectureGuardrailsTest` is the strongest source of truth for allowed dependencies.
- Speech module was intentionally simplified from multi-engine (SherpaOnnx + system) to system-TTS-only.
