# LEARNER PACKAGE KNOWLEDGE

## OVERVIEW
Primary runtime package; composes app wiring (`app/`), feature modules, persistence (`data/`), writer engine, and speech.

## STRUCTURE
```text
com/hanzi/learner/
├── app/               # composition root + navigation
├── features/          # home/practice/admin vertical slices
├── data/              # Room + repository contracts/impls
├── character_writer/  # stroke algorithm + rendering
└── speech/            # local TTS abstractions + impls
```

## WHERE TO LOOK
| Task | Location | Notes |
|---|---|---|
| Change dependency graph | `app/AppContainer.kt`, `app/AppModules.kt` | most coupled code in project |
| Enforce architecture constraints | `.../architecture/ArchitectureGuardrailsTest.kt` | enforceable policy, not optional |
| Feature integration | `app/AppNavGraph.kt` + feature screen/viewmodel | keep navigation logic in app layer |

## CONVENTIONS
- Respect SOLID boundaries: UI depends on abstractions, domain isolated from Compose.
- New cross-cutting behavior should enter via contracts/ports before concrete adapters.

## ANTI-PATTERNS
- No direct `RepositoryImpl` imports in UI/ViewModel.
- No direct DAO/entity imports in feature UI.
- No new global singletons bypassing `AppContainer` composition.
