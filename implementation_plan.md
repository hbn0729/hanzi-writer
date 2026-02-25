# 将「进入页面自动朗读」改为管理界面可配置开关

## 背景

当前 [PracticeScreen.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/practice/ui/PracticeScreen.kt) 的 [PracticeFeedbackEffects](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/practice/ui/PracticeScreen.kt#103-135) 中，每次 `currentChar` 变化（即切换到新汉字）时，都会**无条件**调用 `speaker.speakCharacterAndPhrase()` 自动朗读。该行为是硬编码的，用户无法关闭。

本变更将其改为一个持久化的 Boolean 设置项 `autoReadAloud`，用户可在管理界面 → 设置 tab 中启用/关闭。

---

## 用户审核事项

> [!IMPORTANT]
> **数据库迁移 (v5 → v6)**：`app_settings` 表新增 `autoReadAloud` 列，默认值为 `true`（保持与当前行为一致）。已有用户升级时不会感知行为变化。

> [!NOTE]
> **手动点按喇叭图标**不受此开关影响 — 即使关闭自动朗读，用户仍可随时手动点击朗读。

> [!IMPORTANT]
> **语义确认（已确认）**：
> 1. 设置在 session 启动时快照；session 中途改设置，下次 session 生效。
> 2. `Practice` 与 `Review` 路径纳入同一开关语义（均受 `autoReadAloud` 控制）。

---

## 变更摘要

| 层 | 文件 | 变更 |
|---|---|---|
| Data Entity | [AppSettingsEntity.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/data/local/entity/AppSettingsEntity.kt) | 新增 `autoReadAloud: Boolean = true` |
| Data Model | [AppSettings.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/data/model/AppSettings.kt) | 新增 `autoReadAloud: Boolean = true` |
| Data DAO | [AppSettingsDao.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/data/local/dao/AppSettingsDao.kt) | 无变更（Upsert 通用） |
| Data Repository | [AppSettingsRepository.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/data/repository/AppSettingsRepository.kt) | mapper 加 `autoReadAloud` |
| Data Contract | [AppSettingsRepositoryContract.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/data/repository/AppSettingsRepositoryContract.kt) | 无变更（接口不变） |
| Database | [AppDatabase.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/data/local/AppDatabase.kt) | version 6 + MIGRATION_5_6 |
| Database Schema | `app/schemas/com.hanzi.learner.data.local.AppDatabase/6.json` | 生成并提交 v6 schema 快照 |
| Migration Test | `app/src/androidTest/java/com/hanzi/learner/db/AppDatabaseMigrationTest.kt` | 新增 `5 → 6` 迁移回归测试 |
| Build/Test | [app/build.gradle.kts](file:///e:/project/hanzi-learner/app/build.gradle.kts) | 补充 `androidTestImplementation("androidx.room:room-testing:2.6.1")`（需确认是否已存在） |
| Admin Model | [AdminModels.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/admin/model/AdminModels.kt) | [AdminSettings](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/admin/model/AdminModels.kt#3-8) 新增 `autoReadAloud` |
| Admin Mapper | [AdminRepositoryMappers.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/admin/repository/AdminRepositoryMappers.kt) | mapper 加 `autoReadAloud` |
| Admin SettingsTab | [SettingsTab.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/admin/ui/tabs/SettingsTab.kt) | 新增 Switch 行 |
| Admin Tests | `app/src/test/java/com/hanzi/learner/features/admin/...` | 新增 mapper + viewmodel 回归测试 |
| Backup | [BackupSerializer.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/data/BackupSerializer.kt) | encode/decode 加 `autoReadAloud` |
| Backup Tests | [BackupSerializerTest.kt](file:///e:/project/hanzi-learner/app/src/test/java/com/hanzi/learner/db/BackupSerializerTest.kt) | 增补 encode/decode 兼容性用例 |
| Practice Domain | [PracticeSessionState.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/practice/domain/PracticeSessionState.kt) | 新增 `autoReadAloud: Boolean = true` |
| Practice Domain | [CurrentCharacterLoader.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/practice/domain/CurrentCharacterLoader.kt) | `load()` 方法增加 `autoReadAloud` 参数，返回值包含该字段 |
| Practice Domain | [PracticeSessionOrchestrator.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/practice/domain/PracticeSessionOrchestrator.kt) | `loadCurrentChar()` 传递 `settings.autoReadAloud` 到 `CurrentCharacterLoader` |
| Practice ViewModel | [PracticeViewModel.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/practice/viewmodel/PracticeViewModel.kt) | [PracticeUiState](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/practice/viewmodel/PracticeViewModel.kt#90-104) 新增 `autoReadAloud` + [applyState](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/practice/viewmodel/PracticeViewModel.kt#179-197) 映射 |
| Practice UI | [PracticeScreen.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/practice/ui/PracticeScreen.kt) | [PracticeFeedbackEffects](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/practice/ui/PracticeScreen.kt#103-135) 条件判断 |
| Practice UI Tests | `app/src/test/java/com/hanzi/learner/features/practice/ui/...` | 覆盖自动朗读触发条件（开/关） |

---

## 具体变更

### Data Layer

#### [MODIFY] [AppSettingsEntity.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/data/local/entity/AppSettingsEntity.kt)

新增字段 `autoReadAloud: Boolean = true`。

```diff
 @Entity(tableName = "app_settings")
 data class AppSettingsEntity(
     @PrimaryKey val id: Int = 1,
     val duePickLimit: Int = 50,
     val hintAfterMisses: Int = 2,
     val useExternalDataset: Boolean = false,
+    val autoReadAloud: Boolean = true,
 )
```

---

#### [MODIFY] [AppSettings.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/data/model/AppSettings.kt)

新增字段 `autoReadAloud: Boolean = true`。

```diff
 data class AppSettings(
     val duePickLimit: Int = 50,
     val hintAfterMisses: Int = 2,
     val useExternalDataset: Boolean = false,
+    val autoReadAloud: Boolean = true,
 )
```

---

#### [MODIFY] [AppSettingsRepository.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/data/repository/AppSettingsRepository.kt)

[toData()](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/data/repository/AppSettingsRepository.kt#19-24) 和 [toEntity()](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/data/repository/AppSettingsRepository.kt#25-29) mapper 中补充 `autoReadAloud` 字段映射。

---

#### [MODIFY] [AppDatabase.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/data/local/AppDatabase.kt)

- 版本号 `5 → 6`
- 新增 `MIGRATION_5_6`：`ALTER TABLE app_settings ADD COLUMN autoReadAloud INTEGER NOT NULL DEFAULT 1`
- `.addMigrations()` 追加 `MIGRATION_5_6`
- 生成并提交 `app/schemas/com.hanzi.learner.data.local.AppDatabase/6.json`

#### [ADD] Migration 回归测试（强制）

- 新增 `app/src/androidTest/java/com/hanzi/learner/db/AppDatabaseMigrationTest.kt`
- 使用 `MigrationTestHelper` + `MIGRATION_5_6` 执行 `runMigrationsAndValidate(..., 6, true, MIGRATION_5_6)`
- 断言 `app_settings.autoReadAloud` 列存在，且历史数据迁移后默认值为 `1`
- 在 [app/build.gradle.kts](file:///e:/project/hanzi-learner/app/build.gradle.kts) 增加 `androidTestImplementation("androidx.room:room-testing:2.6.1")`

> [!NOTE]
> **依赖版本确认**：实施前需检查 `build.gradle.kts` 中是否已存在 `room-testing` 依赖。若已存在，确认版本是否需要更新（当前 Room 最新稳定版为 2.6.x，建议与项目 Room 版本保持一致）。

---

### Admin Layer

#### [MODIFY] [AdminModels.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/admin/model/AdminModels.kt)

```diff
 data class AdminSettings(
     val duePickLimit: Int = 50,
     val hintAfterMisses: Int = 2,
     val useExternalDataset: Boolean = false,
+    val autoReadAloud: Boolean = true,
 )
```

---

#### [MODIFY] [AdminRepositoryMappers.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/admin/repository/AdminRepositoryMappers.kt)

[toAdminSettings()](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/admin/repository/AdminRepositoryMappers.kt#14-19) 和 [toEntity()](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/admin/repository/AdminRepositoryMappers.kt#20-26) mapper 中补充 `autoReadAloud` 字段映射。

---

#### [MODIFY] [SettingsTab.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/admin/ui/tabs/SettingsTab.kt)

在「使用外部字库」开关下方新增一行 Switch，仿照已有 `useExternalDataset` 开关的 UI 模式：

```kotlin
Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
) {
    Column(modifier = Modifier.weight(1f)) {
        Text(text = "自动朗读", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "进入练习时自动朗读汉字和词语",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
    Switch(
        checked = s.autoReadAloud,
        onCheckedChange = { onUpdateSettings(s.copy(autoReadAloud = it)) },
    )
}
```

> [!NOTE]
> **设置变更即时反馈**：建议在切换开关后显示即时反馈（如 Snackbar），提示用户"设置将在下次练习时生效"，避免用户困惑为何当前 session 行为未改变。可参考以下实现：
> ```kotlin
> // 在 SettingsTab 中添加 SnackbarHostState
> val snackbarHostState = remember { SnackbarHostState() }
> 
> // 在 onCheckedChange 中触发 Snackbar
> onCheckedChange = { 
>     onUpdateSettings(s.copy(autoReadAloud = it))
>     scope.launch {
>         snackbarHostState.showSnackbar("设置将在下次练习时生效")
>     }
> }
> ```

---

### Backup Layer

#### [MODIFY] [BackupSerializer.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/data/BackupSerializer.kt)

- **encode**: `.put("autoReadAloud", settings.autoReadAloud)`
- **decode**: `autoReadAloud = settingsObj.optBoolean("autoReadAloud", true)`

> [!NOTE]
> 使用 `optBoolean(..., true)` 保证旧版本备份文件（没有此字段）导入后默认启用自动朗读，不会出现兼容性问题。

#### [MODIFY] [BackupSerializerTest.kt](file:///e:/project/hanzi-learner/app/src/test/java/com/hanzi/learner/db/BackupSerializerTest.kt)

新增以下用例（在保留现有版本校验测试基础上）：

1. `encode_includesAutoReadAloudFieldWhenSettingsPresent`
2. `decode_missingAutoReadAloud_defaultsToTrue`
3. `decode_autoReadAloudFalse_keepsFalse`

---

### Practice Layer

#### [MODIFY] [PracticeSessionState.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/practice/domain/PracticeSessionState.kt)

在 `PracticeSessionState` 中新增 `autoReadAloud: Boolean = true`。

```diff
 data class PracticeSessionState(
     val isSessionComplete: Boolean = false,
     val allDisabled: Boolean = false,
     val noReviewsDue: Boolean = false,
     val currentCharacter: CharacterData? = null,
     val currentItem: CharIndexItem? = null,
     val currentPhrase: String = "",
     val strokeIndex: Int = 0,
     val completedStrokeCount: Int = 0,
     val mistakesOnStroke: Int = 0,
     val hintAfterMisses: Int = 2,
+    val autoReadAloud: Boolean = true,
     val windowItems: List<CharIndexItem> = emptyList(),
 )
```

---

#### [MODIFY] [CurrentCharacterLoader.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/practice/domain/CurrentCharacterLoader.kt)

修改 `load()` 方法签名，增加 `autoReadAloud` 参数，并在返回的 `PracticeSessionState` 中包含该字段。

```diff
 class CurrentCharacterLoader(
     private val phraseProvider: PracticePhraseProvider,
 ) {
     suspend fun load(
         repo: CharacterRepository?,
         windowManager: PracticeWindowManager,
         hintAfterMisses: Int,
+        autoReadAloud: Boolean = true,
     ): PracticeSessionState {
         // ... existing logic ...
         return PracticeSessionState(
             currentCharacter = loaded,
             currentItem = item,
             currentPhrase = phrase,
             hintAfterMisses = hintAfterMisses,
+            autoReadAloud = autoReadAloud,
             windowItems = windowManager.windowItems,
         )
     }
 }
```

---

#### [MODIFY] [PracticeSessionOrchestrator.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/practice/domain/PracticeSessionOrchestrator.kt)

修改 `loadCurrentChar()` 方法，将 `settings.autoReadAloud` 传递到 `CurrentCharacterLoader.load()`。

```diff
 private suspend fun loadCurrentChar(): PracticeSessionState {
     return stateLoader.load(
         repo = charRepo,
         windowManager = sessionManager,
         hintAfterMisses = settings.hintAfterMisses,
+        autoReadAloud = settings.autoReadAloud,
     )
 }
```

---

#### [MODIFY] [PracticeViewModel.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/practice/viewmodel/PracticeViewModel.kt)

- [PracticeUiState](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/practice/viewmodel/PracticeViewModel.kt#90-104) 新增 `autoReadAloud: Boolean = true`
- [applyState()](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/practice/viewmodel/PracticeViewModel.kt#179-197) 中映射 `autoReadAloud = state.autoReadAloud`

---

#### [MODIFY] [PracticeScreen.kt](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/practice/ui/PracticeScreen.kt)

1. [PracticeFeedbackEffects](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/practice/ui/PracticeScreen.kt#103-135) 新增参数 `autoReadAloud: Boolean`
2. 在调用处传入 `uiState.autoReadAloud`
3. 自动朗读 `LaunchedEffect(currentChar)` 中添加条件判断：
4. `Practice` 与 `Review` 路径共用同一 `PracticeScreen`，因此二者自动朗读语义保持一致（均受该开关控制）

```diff
 LaunchedEffect(currentChar) {
-    if (!currentChar.isNullOrEmpty()) {
+    if (!currentChar.isNullOrEmpty() && autoReadAloud) {
         speaker.speakCharacterAndPhrase(currentChar, currentPhrase)
     }
 }
```

> [!IMPORTANT]
> **手动点击喇叭按钮**（[PracticeTopBar](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/practice/ui/PracticeScreen.kt#289-398) 的 `onSpeak` 回调，L231-234）**不受影响**，用户随时可手动触发朗读。

#### [ADD] Practice UI 条件触发测试

- 新增 `app/src/test/java/com/hanzi/learner/features/practice/ui/PracticeFeedbackEffectsTest.kt`
- 由于 `PracticeFeedbackEffects` 是 `@Composable` 函数，建议将自动朗读触发条件逻辑提取为纯函数便于单测：

```kotlin
// 在 PracticeScreen.kt 中提取为顶层函数
fun shouldAutoReadAloud(
    currentChar: String?,
    autoReadAloud: Boolean,
): Boolean = !currentChar.isNullOrEmpty() && autoReadAloud
```

- 测试覆盖：
  - `shouldAutoReadAloud(char="一", autoReadAloud=true) → true`
  - `shouldAutoReadAloud(char="一", autoReadAloud=false) → false`
  - `shouldAutoReadAloud(char=null, autoReadAloud=true) → false`
  - `shouldAutoReadAloud(char="", autoReadAloud=true) → false`
  - 手动 `onSpeak` 回调始终可触发朗读（不受 `autoReadAloud` 影响）

> [!NOTE]
> **TTS Mock 方案**：若需对 `PracticeFeedbackEffects` 进行 Compose UI 测试，需 mock `TtsSpeakerContract`。建议方案：
> 1. 创建 `FakeTtsSpeaker` 实现 `TtsSpeakerContract`，记录 `speakCharacterAndPhrase` 调用次数和参数
> 2. 在测试中使用 `composeTestRule.setContent { PracticeFeedbackEffects(..., speaker = fakeSpeaker) }`
> 3. 验证 `fakeSpeaker.callCount` 在 `autoReadAloud=true` 时为 1，在 `autoReadAloud=false` 时为 0

---

#### [MODIFY] PracticeSessionOrchestratorTest.kt

在现有测试基础上，新增验证 `autoReadAloud` 正确传递到 `PracticeSessionState` 的测试：

```kotlin
@Test
fun startSession_withAutoReadAloudFalse_propagatesToState() = runTest {
    val settings = AppSettings(autoReadAloud = false)
    val orchestrator = createOrchestrator(
        index = listOf(item("一")),
        characters = mapOf("一" to characterData("一")),
        dueChars = listOf("一"),
        settings = settings,
    )

    val session = orchestrator.create(reviewOnly = false)
    val state = session.startSession()

    assertEquals(false, state.autoReadAloud)
}

@Test
fun startSession_withAutoReadAloudTrue_propagatesToState() = runTest {
    val settings = AppSettings(autoReadAloud = true)
    val orchestrator = createOrchestrator(
        index = listOf(item("一")),
        characters = mapOf("一" to characterData("一")),
        dueChars = listOf("一"),
        settings = settings,
    )

    val session = orchestrator.create(reviewOnly = false)
    val state = session.startSession()

    assertEquals(true, state.autoReadAloud)
}
```

---

#### [MODIFY] PracticeViewModelTest.kt

在现有测试基础上，新增验证 `autoReadAloud` 正确映射到 `PracticeUiState` 的测试：

```kotlin
@Test
fun loadPracticeSession_propagatesAutoReadAloudToUiState() = runTest {
    // 需要修改 FakeAppSettingsRepository 以支持自定义 settings
    val customSettingsRepo = object : AppSettingsRepositoryContract {
        override suspend fun getSettings(): AppSettings = AppSettings(
            duePickLimit = 50,
            hintAfterMisses = 3,
            autoReadAloud = false,
        )
        override suspend fun updateSettings(settings: AppSettings) {}
    }

    // 使用 customSettingsRepo 创建 orchestrator 和 viewModel
    // ... 

    viewModel.onAction(PracticeAction.Start)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.uiState.test {
        val state = awaitItem()
        assertEquals(false, state.autoReadAloud)
        assertEquals(3, state.hintAfterMisses)
    }
}
```

---

### Admin Tests

#### [ADD] Admin 设置链路测试

- 新增 `AdminRepositoryMappersTest`：验证 `AppSettingsEntity <-> AdminSettings` 的 `autoReadAloud` 双向映射
- 新增 `AdminSettingsViewModelTest`：验证 `updateSettings()` 后 state 与 repository 参数包含 `autoReadAloud`

## 已识别的隐患与对策

| 隐患 | 风险 | 对策 |
|---|---|---|
| 迁移仅靠手工验证 | 升级路径在真实设备崩溃 | 增加 `5 → 6` MigrationTest + schema `6.json` |
| 备份向后兼容 | 旧备份无 `autoReadAloud` 字段 | `optBoolean("autoReadAloud", true)` 默认 `true` |
| 备份向前兼容 | 新备份导入旧版应用 | JSON 使用 `opt*` 读取，旧版会忽略未知字段 |
| 数据库升级 | 旧版用户升级时表缺列 | `MIGRATION_5_6` 带 `DEFAULT 1` |
| 设置读取时机 | 设置在 session 开始时快照 | 与 `hintAfterMisses` 一致，session 中途改设置下次生效 |
| 手动朗读被误拦 | 用户关闭自动朗读后也无法手动朗读 | 仅拦截自动触发条件，不影响手动 `onSpeak` |
| Review 语义漂移 | Practice 与 Review 行为不一致 | 复用同一 `PracticeScreen` 逻辑，并在手测中覆盖 review 路径 |
| Practice 依赖 Data 设置 | [PracticeFeatureDependencies](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/app/Dependencies.kt#33-38) 改动 | 不需要改——设置通过 [PracticeSessionOrchestrator](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/practice/domain/PracticeSessionOrchestrator.kt#11-137) → `PracticeSessionState` 传递，与 `hintAfterMisses` 路径一致 |
| 设置变更无即时反馈 | 用户切换开关后困惑为何当前 session 行为未变 | 在 SettingsTab 中添加 Snackbar 提示"设置将在下次练习时生效" |

---

## 验证方案

### 自动化测试

运行命令（单测）：
```bash
./gradlew app:testDebugUnitTest
```

运行命令（迁移测试，需设备/模拟器）：
```bash
./gradlew app:connectedDebugAndroidTest
```

需要通过的关键测试：
- [ArchitectureGuardrailsTest](file:///e:/project/hanzi-learner/app/src/test/java/com/hanzi/learner/architecture/ArchitectureGuardrailsTest.kt#12-305) — 确保新代码不违反层级约束
- `AppDatabaseMigrationTest`（新增）— 确保 `5 → 6` 迁移可执行且 `autoReadAloud` 默认值正确
- `BackupSerializerTest` — 覆盖 `encode` 字段写入、缺省字段默认值、显式 false 保持
- `PracticeSessionOrchestratorTest` — 确保 `autoReadAloud` 正确传播到 `PracticeSessionState`
- `PracticeViewModelTest` — 确保 `autoReadAloud` 正确出现在 [PracticeUiState](file:///e:/project/hanzi-learner/app/src/main/java/com/hanzi/learner/features/practice/viewmodel/PracticeViewModel.kt#90-104)
- `PracticeFeedbackEffectsTest`（新增）— 确保自动朗读触发条件正确
- `AdminRepositoryMappersTest` / `AdminSettingsViewModelTest`（新增）— 确保 admin 设置链路不丢字段

### 手动验证

请在真机或模拟器上执行以下步骤：

1. **升级安装**：在已有数据的设备上安装新版 APK，确认应用启动无崩溃（数据库迁移正确）
2. **默认行为不变**：进入练习界面，确认自动朗读仍然生效（默认 `true`）
3. **关闭自动朗读**：
   - 进入管理界面 → 设置 tab
   - 找到「自动朗读」开关并关闭
   - 返回练习界面，确认进入时**不**自动朗读
   - 确认手动点击喇叭图标**仍然可以朗读**
4. **重新启用**：再次开启开关，确认自动朗读恢复
5. **备份兼容**：导出备份 → 关闭应用 → 重新导入 → 确认设置保持
6. **Review 路径一致性**：
   - 从首页进入"复习"路径
   - 关闭开关时确认不自动朗读，开启后确认自动朗读恢复
   - 手动点按喇叭在两种状态下都可朗读
7. **Session 中途切换设置**：
   - 进入练习界面，开始一个 session
   - 不退出 session，切换到管理界面关闭「自动朗读」
   - 返回当前 session，确认行为不变（仍自动朗读）
   - 退出 session 后重新进入，确认新设置生效（不自动朗读）
