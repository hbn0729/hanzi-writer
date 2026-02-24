# APP MODULE KNOWLEDGE

## OVERVIEW
Android application module `:app`; owns runtime code, assets, manifests, and app-level Gradle tasks.

## WHERE TO LOOK
| Task | Location | Notes |
|---|---|---|
| Android build config | `app/build.gradle.kts` | source of module plugins/dependencies/tasks |
| Launch config | `app/src/main/AndroidManifest.xml` | `MainActivity` is launcher |
| Runtime code | `app/src/main/java/com/hanzi/learner` | see deeper AGENTS in this tree |
| Runtime assets | `app/src/main/assets` | generated + static mixed |
| Unit tests | `app/src/test/java/com/hanzi/learner` | guardrails + feature tests |

## CONVENTIONS
- Keep module boundaries explicit: app wiring in `app/`, business logic in `features/` or domain modules.
- Asset generation is task-driven (`generateHanziAssets`), not manual copy workflows.

## ANTI-PATTERNS
- Do not hardcode alternate build commands when Gradle wrapper exists.
- Do not treat `app/src/main/assets/char_data` as hand-edited source.
