# APP MODULE KNOWLEDGE

## OVERVIEW
Application composition root. Manual DI with modular composition, navigation graph, and theming.

## STRUCTURE
```text
app/
├── AppContainer.kt           # DI composition root (implements all feature deps)
├── AppModules.kt             # CoreDataModule + PracticeModule + AdminModule
├── Dependencies.kt           # Feature dependency interfaces
├── AppNavGraph.kt            # Navigation graph (home/practice/review/admin)
├── HanziLearnerApp.kt        # Root composable with NavHost
└── theme/                    # HanziLearnerTheme + SeniorTheme + claymorphism
```

## WHERE TO LOOK
| Task | Location | Notes |
|---|---|---|
| Add feature dependency | `AppContainer.kt` + `AppModules.kt` | Extend interface in `Dependencies.kt`, implement in modules |
| DI lifecycle | `AppContainer.kt` | AdminModule uses `by lazy` for deferred initialization |
| Add route | `AppNavGraph.kt` | Feature screens receive deps via typed interfaces |
| Theme changes | `theme/Theme.kt`, `theme/Claymorphism.kt` | Claymorphism design system + dual themes |

## PERFORMANCE (PWR-07/08)
- **Async DI Init**: `HanziLearnerApplication` builds `AppContainer` on `Dispatchers.IO` via `Deferred<AppContainer>` to prevent cold-start blocking.
- **Lazy AdminModule**: Heavy admin repositories only initialized when user enters admin section.
- **StrictMode (Debug)**: Detects disk I/O and resource leaks on main thread.

## CONVENTIONS
- Feature dependencies are interface-based; never expose concrete implementations to UI.
- Modules encapsulate their construction logic; `AppContainer` only wires them together.

## ANTI-PATTERNS
- Do not add direct `AppContainer` references in feature UI; use `*FeatureDependencies` interfaces.
- Do not bypass modular boundaries by cross-importing module internals.
