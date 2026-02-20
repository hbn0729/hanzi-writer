# SOURCE DATA KNOWLEDGE

## OVERVIEW
Authoritative raw datasets for phrase and stroke sources. Upstream for generated app assets.

## WHERE TO LOOK
| Task | Location | Notes |
|---|---|---|
| Phrase source | `词语表-直接导入即可.json` | upstream phrase definitions |
| Bulk stroke/dictionary source | `笔画和词库数据-直接导入即可.zip` | input consumed by pipeline |

## CONVENTIONS
- Treat `/data` as source-of-truth for data fixes.
- Re-run `./gradlew app:generateHanziAssets` after source changes.

## ANTI-PATTERNS
- Do not fix data issues only in generated assets under `app/src/main/assets`.
- Do not rename source files without updating pipeline assumptions.
