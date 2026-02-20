# CHARACTER WRITER KNOWLEDGE

## OVERVIEW
Stroke-writing engine with data loading, geometric matching, and Compose canvas rendering.

## WHERE TO LOOK
| Task | Location | Notes |
|---|---|---|
| Stroke match math | `match/StrokeMatcher.kt`, `match/Geometry.kt` | accuracy + tolerance logic |
| Rendering | `render/HanziCanvas.kt`, `AnimatedHanziCanvas.kt` | performance-sensitive drawing |
| Character asset access | `data/CharacterRepository*.kt` | source selection and parsing |
| User tracing overlay | `practice/TraceOverlay.kt` | pointer/stroke interaction |

## CONVENTIONS
- Keep algorithmic logic pure and unit-test friendly.
- Separate geometric validation from UI rendering concerns.
- Repository selection should stay behind `CharacterRepository` abstractions.

## ANTI-PATTERNS
- Do not embed Compose state logic into matching algorithms.
- Do not bypass repository selectors with ad-hoc file I/O in feature code.
- Do not couple render components to admin-only import workflows.
