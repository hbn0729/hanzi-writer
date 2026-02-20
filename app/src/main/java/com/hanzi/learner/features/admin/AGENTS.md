# ADMIN FEATURE KNOWLEDGE

## OVERVIEW
Administrative workflows: dashboard metrics, character/phrase management, backup import/export, settings.

## STRUCTURE
```text
admin/
├── ui/                 # tab routes + tab composables
├── viewmodel/          # tab-specific state managers
├── domain/             # loader/use-case orchestration
├── repository/         # admin adapters over shared contracts
└── backup/             # parsing/writing/zip extraction helpers
```

## WHERE TO LOOK
| Task | Location | Notes |
|---|---|---|
| Tab navigation | `ui/AdminTabRoutes.kt` | high churn integration point |
| Main shell screen | `ui/AdminScreen.kt` | tab host and wiring |
| Character management (字管理) | `ui/tabs/CharacterManagementTab.kt` | supports paging for large character lists (3000+ chars) |
| Backup business flow | `viewmodel/AdminBackupViewModel.kt`, `repository/backup/*` | enforce port segregation |
| Import/export parsing | `backup/StrokeDatasetParser.kt`, `StrokeDatasetWriter.kt` | data contract-sensitive |

## PERFORMANCE
- Character list (`CharacterManagementTab`) uses manual paging (pageSize=200) to handle 3000+ characters without loading all into memory.
- Filtering happens in-memory but only visible items are rendered via `visibleItems` and `totalCount`.
- "Load more" button appears when totalCount > displayCount; resets on filter/search changes.

## CONVENTIONS
- Keep backup operations behind segregated backup ports (guardrail-enforced).
- Keep mapper logic in `repository` adapters, not in UI/ViewModel.
- Admin tabs should stay independently testable.

## ANTI-PATTERNS
- Do not re-centralize admin data access into a single monolithic repository class.
- Do not place zip/file parsing in composables.
- Do not bypass domain loaders when adding new admin tabs.
