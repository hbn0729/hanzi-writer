# PRACTICE FEATURE KNOWLEDGE
Core learning loop: choose next character, run stroke session, evaluate match/miss, persist progress. Uses SeniorTheme (large fonts) + claymorphism UI.
## WHERE TO LOOK
| Task | Location | Notes |
|---|---|---|
| Main UI | `ui/PracticeScreen.kt` | SeniorTheme-wrapped; decomposed into CharacterHeader, TraceCanvas, ExitButtonRow |
| Session state machine | `viewmodel/PracticeViewModel.kt` | event handling + state updates |
| Session orchestration | `domain/PracticeSessionOrchestrator.kt` | high logical complexity |
| Selection/completion use cases | `domain/PickNextPracticeItemUseCase.kt`, `CompletePracticeCharacterUseCase.kt` | core business rules |
## CONVENTIONS
- Maintain dependency inversion: ViewModel depends on `PracticeSessionEngine` abstraction, not concrete orchestrator internals.
- Keep stroke-match handling deterministic and testable in domain-level collaborators.
- Prefer small stateless composables for UI fragments extracted from `PracticeScreen`.
- TTS is consumed via `rememberTtsSpeaker(context)` — no ViewModel needed for speech.
- Practice screen is wrapped in `SeniorTheme` for elderly-friendly large typography.
- UI elements use `claymorphism()` and `clayClickable()` modifiers for consistent clay-style appearance.
- **Auto-read-aloud**: `PracticeUiState.autoReadAloud` controls automatic TTS on character change; manual speaker button always works regardless of this setting.
## ANTI-PATTERNS
- Do not introduce direct repository/DAO access in `ui`.
- Do not couple `PracticeViewModel` back to concrete orchestration types forbidden by guardrail tests.
- Do not mix rendering concerns with spacing-repetition policy rules.
- Do not add TTS model selection UI — removed intentionally; system TTS only.
