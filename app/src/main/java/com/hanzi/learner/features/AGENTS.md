# FEATURES PACKAGE KNOWLEDGE

## OVERVIEW
Feature-first organization. Each feature owns `ui`, `viewmodel`, and `domain` subpackages with explicit boundaries.

## WHERE TO LOOK
| Task | Location | Notes |
|---|---|---|
| Home flow | `home/ui`, `home/viewmodel`, `home/domain` | lightweight entry slice |
| Practice flow | `practice/*` | core user loop, highest complexity |
| Admin flow | `admin/*` | backup/import/statistics toolchain |
| Shared feature ports/helpers | `common/ports`, `common/extensions` | keep minimal and generic |

## CONVENTIONS
- Keep SRP by isolating use cases in `domain` and event/state shaping in `viewmodel`.
- `ui` should render state + emit intents only.
- Shared code goes to `common/*` only when truly cross-feature.

## ANTI-PATTERNS
- Do not import one feature's UI internals into another feature.
- Do not leak persistence details into `ui` or `domain`.
- Do not move orchestration logic into composables.
