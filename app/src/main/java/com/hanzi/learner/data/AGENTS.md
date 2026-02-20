# DATA PACKAGE KNOWLEDGE

## OVERVIEW
Persistence and data contracts: Room schema/DAO, repository contracts and implementations, backup serialization utilities.

## WHERE TO LOOK
| Task | Location | Notes |
|---|---|---|
| Room setup | `local/AppDatabase.kt`, `local/entity/*`, `local/dao/*` | schema + queries |
| Contracts | `repository/*Contract.kt` | boundary API for upper layers |
| Implementations | `repository/*.kt` (non-contract) | adapter logic over Room/models |
| Backup payloads | `BackupSerializer.kt`, `model/Backup*` | serialization compatibility |

## CONVENTIONS
- Contract-first design: expose domain/data models, not Room entity internals.
- Keep repository implementations minimal and side-effect focused.
- Time-dependent logic must consume `TimeProvider`.

## ANTI-PATTERNS
- Do not expose `Entity` types from repository contracts.
- Do not import feature-layer packages into data layer.
- Do not use `LocalDate.now()` outside provider abstraction.
