# PROJECT KNOWLEDGE BASE

**Generated:** 2026-02-20T21:04:53+08:00  
**Commit:** e5e0be7  
**Branch:** main

## OVERVIEW
Android Hanzi learning app (Kotlin + Compose + Room + manual DI). Core domain is stroke-writing practice with local assets and a custom Python asset-generation pipeline.

## STRUCTURE
```text
./
├── app/                          # Android application module (:app)
│   ├── src/main/java/com/hanzi/learner/
│   │   ├── app/                  # app wiring, DI container, navigation
│   │   ├── features/             # feature modules (home/practice/admin)
│   │   ├── data/                 # Room + repositories + models
│   │   ├── character_writer/     # stroke rendering/matching engine
│   │   └── speech/               # TTS contracts + implementations
│   ├── src/main/assets/          # generated character data + static assets
│   └── src/test/java/com/hanzi/learner/
├── data/                         # source datasets for asset generation
└── tools/                        # asset pipeline scripts (Python)
```

## WHERE TO LOOK
| Task | Location | Notes |
|---|---|---|
| Add/modify screens | `app/src/main/java/com/hanzi/learner/features/*/ui` | UI must stay free of repository impl/DAO imports |
| Change feature flow | `features/*/viewmodel` + `features/*/domain` | Respect architecture guardrails test |
| Dependency wiring | `app/src/main/java/com/hanzi/learner/app/AppContainer.kt`, `AppModules.kt` | Single composition root |
| Data persistence | `app/src/main/java/com/hanzi/learner/data/local` + `data/repository` | Contracts first, impl second |
| Writing algorithm | `character_writer/match`, `character_writer/render` | Performance-sensitive paths |
| Asset pipeline | `tools/generate_assets.py`, `app/build.gradle.kts` task `generateHanziAssets` | Source-of-truth is `/data`, not generated assets |
| Speech/TTS | `speech/`, `speech/internal` | TTS contracts + implementations, fallback support |
| Architecture policy | `app/src/test/java/com/hanzi/learner/architecture/ArchitectureGuardrailsTest.kt` | Encodes forbidden dependency directions |

## CODE MAP
| Symbol | Type | Location | Refs | Role |
|---|---|---|---:|---|
| `MainActivity` | class | `.../MainActivity.kt` | high | Android launcher entry |
| `AppNavGraph` | function set | `.../app/AppNavGraph.kt` | high | app route composition |
| `AppModules` | object/class cluster | `.../app/AppModules.kt` | high | module wiring + factories |
| `PracticeViewModel` | class | `.../features/practice/viewmodel/PracticeViewModel.kt` | high | practice state orchestration |
| `AdminScreen` | composable | `.../features/admin/ui/AdminScreen.kt` | medium | admin feature entry UI |
| `ArchitectureGuardrailsTest` | test class | `.../architecture/ArchitectureGuardrailsTest.kt` | high | enforces layer boundaries |
| `TtsSpeakerContract` | interface | `.../speech/contract/TtsSpeakerContract.kt` | medium | TTS abstraction |
| `SpeechModule` | object | `.../speech/SpeechModule.kt` | medium | TTS factory |
| `FallbackTtsSpeaker` | class | `.../speech/internal/FallbackTtsSpeaker.kt` | medium | system TTS with fallback |
| `SystemTtsSpeaker` | class | `.../speech/internal/SystemTtsSpeaker.kt` | medium | Android system TTS |

## MUST DO(ENFORCED)
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

## ANTI-PATTERNS (THIS PROJECT)
- Do not import `AppContainer` directly in feature UI screens.
- Do not import concrete `RepositoryImpl`/DAO types into UI/ViewModel layers.
- Do not import Compose APIs into `domain`/`application` layers.
- Do not manually patch generated files in `app/src/main/assets/char_data` as primary fix; patch source data/pipeline.
- Do not bypass feature boundaries by cross-importing feature UI internals.

## PERFORMANCE
- Large lists (e.g., character management with 3000+ chars) use manual paging to avoid memory pressure.
- See `features/admin/AGENTS.md` for character list paging details.

## UNIQUE STYLES
- Manual DI composition root (`AppContainer`, `AppModules`) instead of framework DI.
- Architecture is guarded with unit tests, not documentation-only guidance.
- Feature-first packaging with explicit `domain`, `ui`, `viewmodel` subpackages.

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

## SPEECH MODULE

### Overview
TTS module with automatic fallback: tries system TTS first (Chinese support), falls back to built-in SherpaOnnx if unavailable.

### Files
| File | Role |
|------|------|
| `speech/SpeechModule.kt` | Factory for creating TTS speakers |
| `speech/TtsSpeakerComposables.kt` | Compose integration (`rememberTtsSpeaker`) |
| `speech/contract/TtsSpeakerContract.kt` | High-level TTS interface |
| `speech/contract/TtsEngineContract.kt` | Low-level TTS engine interface |
| `speech/internal/SystemTtsSpeaker.kt` | Android system TTS adapter |
| `speech/internal/FallbackTtsSpeaker.kt` | Smart fallback wrapper |
| `speech/internal/SherpaOnnxTtsSpeaker.kt` | Built-in SherpaOnnx TTS |

### Usage
```kotlin
@Composable
fun MyScreen(context: Context) {
    val speaker = rememberTtsSpeaker(context)  // auto-fallback
    speaker.speakCharacterAndPhrase("汉", "汉字")
}
```

### Fallback Logic
1. App starts → `FallbackTtsSpeaker` initializes
2. Try system TTS (max 3s timeout)
3. If system TTS has Chinese support → use it
4. Else → use built-in SherpaOnnx TTS
