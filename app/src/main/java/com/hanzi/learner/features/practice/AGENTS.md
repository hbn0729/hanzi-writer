# PRACTICE FEATURE KNOWLEDGE

## OVERVIEW
Core learning loop: choose next character, run stroke session, evaluate match/miss, persist progress.

## WHERE TO LOOK
| Task | Location | Notes |
|---|---|---|
| Main UI | `ui/PracticeScreen.kt` | largest UI hotspot; prefer decomposition |
| Session state machine | `viewmodel/PracticeViewModel.kt` | event handling + state updates |
| Session orchestration | `domain/PracticeSessionOrchestrator.kt` | high logical complexity |
| Selection/completion use cases | `domain/PickNextPracticeItemUseCase.kt`, `CompletePracticeCharacterUseCase.kt` | core business rules |

## CONVENTIONS
- Maintain dependency inversion: ViewModel depends on `PracticeSessionEngine` abstraction, not concrete orchestrator internals.
- Keep stroke-match handling deterministic and testable in domain-level collaborators.
- Prefer small stateless composables for UI fragments extracted from `PracticeScreen`.

## ANTI-PATTERNS
- Do not introduce direct repository/DAO access in `ui`.
- Do not couple `PracticeViewModel` back to concrete orchestration types forbidden by guardrail tests.
- Do not mix rendering concerns with spacing-repetition policy rules.
