# LEARNER PACKAGE KNOWLEDGE

## OVERVIEW
Primary runtime package; composes app wiring (`app/`), feature modules, persistence (`data/`), writer engine, and speech.

## STRUCTURE
```text
com/hanzi/learner/
├── app/               # composition root + DI modules + navigation + theme
├── features/          # home/practice/admin vertical slices
├── data/              # Room + repository contracts/impls
├── character_writer/  # stroke algorithm + rendering
└── speech/            # system TTS only (simplified)
```

## WHERE TO LOOK
| Task | Location | Notes |
|---|---|---|
| Change dependency graph | `app/AppContainer.kt`, `app/AppModules.kt`, `app/Dependencies.kt` | modular composition root + feature dependency interfaces |
| Enforce architecture constraints | `.../architecture/ArchitectureGuardrailsTest.kt` | enforceable policy, not optional |
| Feature integration | `app/AppNavGraph.kt` + feature screen/viewmodel | keep navigation logic in app layer |
| Theme/styling | `app/theme/Theme.kt`, `app/theme/Claymorphism.kt` | claymorphism + SeniorTheme (elderly-friendly) |

## CONVENTIONS
- Respect SOLID boundaries: UI depends on abstractions, domain isolated from Compose.
- New cross-cutting behavior should enter via contracts/ports before concrete adapters.

## ANTI-PATTERNS
- No direct `RepositoryImpl` imports in UI/ViewModel.
- No direct DAO/entity imports in feature UI.
- No new global singletons bypassing `AppContainer` composition.
