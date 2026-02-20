# TOOLS WORKFLOW KNOWLEDGE

## OVERVIEW
Python tooling for asset generation; converts source datasets into app-consumable assets.

## WHERE TO LOOK
| Task | Location | Notes |
|---|---|---|
| Generation entry | `generate_assets.py` | parses lesson chars, dictionary, graphics, phrases |
| Invocation point | `app/build.gradle.kts` task `generateHanziAssets` | canonical execution path |

## CONVENTIONS
- Validate data shape at generation time (phrase length, missing character data).
- Keep output deterministic: stable sort by codepoint for generated index.

## ANTI-PATTERNS
- Do not manually patch generated `char_data` as a primary fix.
- Do not change CLI argument contract without updating Gradle task wiring.
