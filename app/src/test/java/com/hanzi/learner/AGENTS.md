# TEST PACKAGE KNOWLEDGE

## OVERVIEW
Unit tests enforce behavior and architecture boundaries; includes guardrail tests that define allowed dependency directions.

## WHERE TO LOOK
| Task | Location | Notes |
|---|---|---|
| Architecture policy | `architecture/ArchitectureGuardrailsTest.kt` | first stop for layering constraints |
| Practice behavior | `feature/practice/*` | session/orchestrator/viewmodel tests |
| Data behavior | `db/*` | repository + backup serializer tests |
| UI navigation behavior | `ui/navigation/*` | app-level navigation contracts |

## CONVENTIONS
- Add/adjust guardrail assertions when introducing new architectural boundaries.
- Keep tests package-aligned with production responsibilities.
- Prefer deterministic tests with explicit collaborators.

## ANTI-PATTERNS
- Do not weaken or delete architecture tests to land feature changes.
- Do not rely on broad integration assertions when unit boundaries suffice.
