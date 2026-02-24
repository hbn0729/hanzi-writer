Hanzi Learner 性能优化计划 v5（最终版）
一、项目概况与性能画像
| 维度 | 现状 |
|------|------|
| 平台 | Android (minSdk 30, targetSdk 33) |
| 技术栈 | Kotlin 1.8.22 + Compose BOM 2023.06 + Room 2.5.2 + 手动 DI |
| 构建工具 | AGP 7.3.1, Gradle 7.4.2 |
| 数据规模 | 3000+ 汉字资产文件，每文件 ~2-5KB JSON；支持外部字库导入替换 |
| 核心热路径 | Canvas 笔画渲染（60fps）→ 手势拖拽采集 → 笔画匹配算法 → 状态刷新 |
| Release 优化 | R8/ProGuard 未启用，无 Baseline Profile |
性能瓶颈分布（按用户感知影响排序）：
书写练习流畅度  ████████████████████  最高（每帧 Path 分配 + 拖拽列表拷贝）
汉字切换速度    ████████████          中高（每次切换重新读取 assets + JSON 解析）
冷启动时间      ████████              中等（主线程同步初始化 DI + Room）
管理页列表滚动  ██████                中等（3000+ 条目主线程过滤 + Compose 重组）
APK 体积        ████                  较低（R8 未启用）
---
二、基线指标与验收标准
执行前必须在指定参考设备上采集基线数据。所有验收标准以相对改善率为主。
参考设备规格：选定一台中低端代表性设备（如 Redmi Note 系列或 Pixel 4a），记录型号、Android 版本、可用内存。所有测量在相同条件下进行（室温、电量 > 50%、后台清空）。每项测量 5 次取中位数。
| 指标 | 测量方式 | 基线值（待采集） | 验收标准 |
|------|---------|----------------|----------|
| 启动耗时 | adb shell am start -S -W 的 TotalTime（接近 Activity 启动完成时间，非严格首帧） | — | 较基线下降 ≥ 20% |
| 冷启动 TTI | 自定义埋点：从 Application.onCreate 到首页数据加载完成并可点击 | — | 较基线下降 ≥ 15% |
| 练习页帧耗时 | Profiler → Frame Timeline，书写期间 P95 | — | 较基线下降 ≥ 30% |
| draw 阶段内存分配 | Memory Profiler → Allocation Tracking，draw 循环内 | — | 较基线下降 ≥ 95% |
| 汉字切换延迟 | 从 completeCharacter 到下一汉字渲染完成 | — | 缓存命中时较基线下降 ≥ 80% |
| 管理页搜索响应 | 输入字符到列表更新完成 | — | 较基线下降 ≥ 50% |
| APK 大小 (release) | ./gradlew app:assembleRelease | — | R8 启用后缩小 ≥ 25% |
| Compose 重组次数 | Layout Inspector → Recomposition counts | — | 见各 OPT 项具体验收条件 |
TTI 埋点方案（OPT-05 配套）：
// Application 中记录启动时刻
val startTime = SystemClock.elapsedRealtime()
// 首页 ViewModel 数据加载完成后记录
val tti = SystemClock.elapsedRealtime() - (application as App).startTime
Log.d("Perf", "TTI: ${tti}ms")
采集方式备注：
- debug build 采集帧数据与内存分配（Profiler overhead 已知，但前后对比时一致即可）
- release build 采集启动时间与 APK 大小
- 每阶段完成后在同一设备上复测，与基线对比
- 每次记录设备型号、OS 版本、电量、温度
---
三、优化项目清单
共 14 项，按 P0-P4 五级优先级排列。
---
P0 — 关键帧率缺陷（直接导致书写卡顿）
OPT-01 HanziCanvas 绘制循环消除对象分配
位置: character_writer/render/HanziCanvas.kt:80-104
问题: drawIntoCanvas {} 内部每帧创建：
- 轮廓绘制：N 个 Path(raw) 拷贝 + transform(matrix)（第 84-86 行）
- 已完成笔画：最多 N 个 Path(raw) 拷贝（第 92-94 行）
- 动画笔画：buildPolylinePath 创建新 Path（第 100 行）
- extractPathSegment 创建新 PathMeasure + Path（第 120-127 行）
总计每帧 2N + 3 个对象分配（N = 笔画数 5-15），60fps 下造成高频 GC pressure。
方案: 将 Canvas 替换为 Modifier.drawWithCache
drawWithCache 的 cache block 在 size 或声明的 key 变化时自动重算，draw block 每帧执行但只使用已缓存对象：
cache block（仅在 size/character/animatedStrokeIndex 变化时执行）:
  - 构建 Positioner、Matrix
  - 预计算 transformedPaths = rawPaths.map { Path(raw).apply { transform(matrix) } }
  - 为当前 animatedStrokeIndex 构建 medianPath + PathMeasure
  - 创建 reusableOutputPath
onDrawBehind（每帧执行，零分配）:
  - 轮廓/已完成笔画：直接绘制 transformedPaths[i]
  - 动画笔画：reusableOutputPath.reset()
    → cachedPathMeasure.getSegment(0, stop, reusableOutputPath, true)
    → drawPath(reusableOutputPath, ...)
关键细节:
- animatedStrokeProgress 每帧变化，不能作为 cache key，只在 draw block 中读取
- animatedStrokeIndex 变化频率低（每笔一次），作为 cache key 触发 medianPath 重算
- completedStrokeCount 变化频率低，不影响 cache（只决定绘制 transformedPaths 的范围）
- Paint 对象已经在外部 remember 中缓存，无需重复处理
关于缓存分层的取舍: animatedStrokeIndex 作为 cache key 意味着每次换笔会重算 N 个 transformedPaths。但换笔频率极低（每个汉字仅 5-15 次），而当前代码是每帧重算（60fps），收益比 = 消除 N×60/s 持续分配 vs 多出 N×~10次/字 的一次性重算。拆分为两级缓存需要在 drawWithCache 外部引入额外 remember + mutable state 协调，增加复杂度，实际收益可忽略。如实测发现换笔卡顿再考虑拆分。
影响范围: 仅修改 HanziCanvas.kt，不改公开接口
验收: draw 阶段内存分配较基线下降 ≥ 95%
---
OPT-02 TraceOverlay 手势路径消除 O(n^2) 拷贝
位置: character_writer/practice/TraceOverlay.kt:94-101
问题: onDrag 回调中 userStrokeCanvasPoints = userStrokeCanvasPoints + pos，每次追加都创建新 List。一笔手势约 100-300 个拖拽事件，总内存分配 O(n^2)。userStrokeHanziPoints 同理。
方案: 改用 mutableStateListOf
val userStrokeCanvasPoints = remember { mutableStateListOf<Offset>() }
val userStrokeHanziPoints = remember { mutableStateListOf<Point>() }
// onDragStart: clear + add（O(1)）
// onDrag: add（O(1) amortized）
// onDragEnd: toList() 一次快照给 matcher，然后 clear
// onDragCancel: clear
Canvas 绘制中的 Path 也应复用（第 118-123 行）：
val userPath = remember { Path() }
// draw block 中: userPath.reset() → moveTo → lineTo → drawPath
影响范围: 仅修改 TraceOverlay.kt
验收: 书写一个 10 画汉字，onDrag 回调期间 List 分配较基线下降 ≥ 95%
---
OPT-03 StrokeMatcher 笔画数据预计算
位置: character_writer/match/StrokeMatcher.kt:76, character_writer/model/CharacterData.kt
问题: DefaultStrokeMatcher.matches() 每次调用都执行 character.medians.mapIndexed { ... Stroke(...) }，重新构建所有笔画的 Stroke 列表。
方案: 在 CharacterData 中延迟预计算
data class CharacterData(
    val char: String,
    val strokes: List<String>,
    val medians: List<List<Point>>,
) {
    val strokeObjects: List<Stroke> by lazy {
        medians.mapIndexed { index, pts -> Stroke(points = pts, strokeNum = index) }
    }
}
DefaultStrokeMatcher.matches() 改用 character.strokeObjects。
附加: Geometry.kt 中 outlineCurve 的 remaining 列表（第 126 行 ArrayList.removeAt(0) 是 O(n)）改用 index 游标遍历原列表，避免元素移动。
影响范围: CharacterData.kt, StrokeMatcher.kt, Geometry.kt
验收: matches() 单次调用耗时较基线下降（可用 unit test 对比）
---
P1 — 数据加载与启动优化
OPT-04 Provider 级汉字数据缓存（含显式失效）
位置: app/DefaultCharacterRepositoryProvider.kt:11, character_writer/data/AssetCharacterRepository.kt
问题链路:
1. DefaultCharacterRepositoryProvider.get() 每次创建新 Repository 实例
2. PracticeSessionOrchestrator 和 AdminIndexRepositoryImpl 各自调用 get() 得到独立实例
3. 缓存放在 Repository 内部 → 跨会话/跨模块无法复用
陈旧数据风险: StrokeDatasetImportService.kt:49 执行 datasetDir.deleteRecursively() + 重建目录 + 设置 useExternalDataset = true。如果 Provider 按 boolean key 缓存，导入后相同 true key 命中旧 repo，其 LruCache 持有已被删除的数据。
失效策略关键约束: AdminDataChangedNotifier 不能用作缓存失效触发——它只递增一个 UI 版本号（AdminDataChangedNotifier.kt:13），触发 AdminScreen 的 LaunchedEffect 重新调用 indexDataLoader.load()，但 indexDataLoader 内部仍经过 characterRepositoryProvider.get(true) 取到旧缓存实例。notifier 不触达 Provider/Repository 缓存层。
方案: 两层缓存 + 独立缓存控制接口（接口隔离）
接口设计:
CharacterRepositoryProvider 保持不变，新增独立的缓存控制接口，避免将缓存控制泄漏到 feature 公共契约：
// 新文件: features/common/ports/CharacterCacheController.kt
interface CharacterCacheController {
    fun invalidate()  // 清除 Provider 和 Repository 层缓存
}
CharacterRepositoryProvider 不变（现有测试桩无需修改）:
interface CharacterRepositoryProvider {
    fun get(useExternalDataset: Boolean): CharacterRepository
}
第一层 — Provider 缓存 Repository 实例（实现双接口）:
internal class DefaultCharacterRepositoryProvider(
    private val context: Context,
    private val factory: CharacterRepositoryFactory,
) : CharacterRepositoryProvider, CharacterCacheController {
    private var cachedRepo: CharacterRepository? = null
    private var cachedUseExternal: Boolean? = null
    override fun get(useExternalDataset: Boolean): CharacterRepository {
        if (useExternalDataset == cachedUseExternal) {
            cachedRepo?.let { return it }
        }
        return factory.create(context, useExternalDataset).also {
            cachedRepo = it
            cachedUseExternal = useExternalDataset
        }
    }
    override fun invalidate() {
        cachedRepo = null
        cachedUseExternal = null
    }
}
第二层 — Repository 内缓存:
class AssetCharacterRepository(private val context: Context) : CharacterRepository {
    private var indexCache: List<CharIndexItem>? = null
    private val characterCache = LruCache<String, CharacterData>(40)
    override suspend fun loadIndex(): List<CharIndexItem> {
        indexCache?.let { return it }
        return withContext(Dispatchers.IO) {
            val json = context.assets.open("char_index.json").bufferedReader().use { it.readText() }
            CharacterJsonParser.parseIndex(json).also { indexCache = it }
        }
    }
    override suspend fun loadCharacter(item: CharIndexItem): CharacterData {
        characterCache.get(item.char)?.let { return it }
        return withContext(Dispatchers.IO) {
            val json = context.assets.open(item.file).bufferedReader().use { it.readText() }
            CharacterJsonParser.parseCharacter(item, json).also { characterCache.put(item.char, it) }
        }
    }
}
FileCharacterRepository 同理。
失效触发点（唯一必选路径）: AdminStrokeImportPortAdapter 构造时注入 CharacterCacheController（非 CharacterRepositoryProvider），importStrokes() 成功后调用 cacheController.invalidate()：
class AdminStrokeImportPortAdapter(
    // ... existing params ...
    private val cacheController: CharacterCacheController,  // 新增：仅缓存控制接口
) : StrokeImportPort {
    override suspend fun importStrokes(uri: Uri): StrokeImportResult {
        val result = strokeDatasetImportService.import(uri)
        if (result.switchedToExternalDataset) {
            cacheController.invalidate()  // 强制失效
        }
        return result
    }
}
需同步修改 AppModules.kt 中 AdminModule 的构造，将 DefaultCharacterRepositoryProvider（向上转型为 CharacterCacheController）传入 adapter。
测试桩影响: 无。CharacterRepositoryProvider 接口未变，现有 3 个测试文件（HomeViewModelTest.kt:86, PracticeSessionOrchestratorTest.kt:92, PracticeViewModelTest.kt:57）中的匿名实现无需修改。
影响范围: 新增 CharacterCacheController.kt 接口, DefaultCharacterRepositoryProvider.kt（实现双接口）, AssetCharacterRepository.kt, FileCharacterRepository.kt, AdminStrokeImportPortAdapter.kt, AdminBackupPortAdapters.kt, AppModules.kt（AdminModule 构造）
验收: 连续切换 5 个汉字后回到第 1 个，loadCharacter 耗时较首次下降 ≥ 80%；导入字库后立即进入练习，使用的是新数据
---
OPT-05 应用启动路径改造
位置: MainActivity.kt:15, AppModules.kt:108-231
问题: AppContainer 在 MainActivity.onCreate 主线程同步初始化。CoreDataModule → AppDatabase.getInstance() → Room 构建 + schema 验证 + migration（磁盘 I/O）全部阻塞主线程。
注意: 单纯在 Application.onCreate 中 IO 线程预热数据库不够——AppDatabase.getInstance() 使用 synchronized(this)，预热未完成时主线程仍阻塞在锁上。
方案: 容器 lazy 化 + 首屏 loading gate
// HanziLearnerApplication.kt
class HanziLearnerApplication : Application() {
    val startTimestamp = SystemClock.elapsedRealtime()  // TTI 基准
    val containerDeferred: Deferred<AppContainer> by lazy {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).async {
            AppContainer(applicationContext)
        }
    }
    override fun onCreate() {
        super.onCreate()
        containerDeferred  // 触发 lazy 开始后台构建
    }
}
// MainActivity.kt
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
        val container by produceState<AppContainer?>(null) {
            value = (application as HanziLearnerApplication).containerDeferred.await()
        }
        HanziLearnerTheme {
            if (container != null) {
                HanziLearnerApp(appDeps = container!!)
            } else {
                // Splash / loading indicator（不会长期停留）
            }
        }
    }
}
AdminModule 额外 lazy 化:
// AppContainer 中
private val adminModule: AdminModuleApi by lazy { AdminModule(...) }
// admin 相关 override 属性改为 get() = adminModule.xxx
双指标验收:
| 指标 | 含义 | 测量方式 |
|------|------|---------|
| 启动耗时 | 用户看到画面（含 loading） | adb shell am start -S -W TotalTime |
| TTI | 首页数据加载完成可点击 | 自定义埋点：Application.startTimestamp 到 HomeViewModel 数据 ready |
第一个指标改善来自 loading gate 提前显示画面，第二个指标反映真实初始化效率。两者都应较基线改善。
影响范围: 新增 HanziLearnerApplication.kt，修改 MainActivity.kt, AppContainer.kt, AndroidManifest.xml
验收: 启动耗时较基线下降 ≥ 20%；TTI 较基线下降 ≥ 15%
---
OPT-06 Room 数据库添加查询索引
位置: data/local/entity/HanziProgressEntity.kt:6-14
问题: DAO 中按 nextDueDay、lastStudiedDay、wrongCount 排序/过滤的查询无索引。
方案:
@Entity(
    tableName = "hanzi_progress",
    indices = [
        Index(value = ["nextDueDay"]),
        Index(value = ["lastStudiedDay"]),
        Index(value = ["wrongCount"]),
    ]
)
data class HanziProgressEntity(...)
MIGRATION_4_5:
private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS index_hanzi_progress_nextDueDay ON hanzi_progress(nextDueDay)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_hanzi_progress_lastStudiedDay ON hanzi_progress(lastStudiedDay)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_hanzi_progress_wrongCount ON hanzi_progress(wrongCount)")
    }
}
影响范围: HanziProgressEntity.kt, AppDatabase.kt（version 4→5）
验收: EXPLAIN QUERY PLAN 确认索引被使用
---
P2 — Compose 重组效率
OPT-07 数据模型稳定性保证
位置: character_writer/model/CharacterData.kt, character_writer/data/CharIndexItem.kt
问题: 包含 List<String> / List<List<Point>> 的 data class 被 Compose 编译器判定为不稳定参数，导致无条件重组。
推荐方案 — kotlinx-collections-immutable:
// build.gradle.kts
implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.5")
// CharacterData.kt
data class CharacterData(
    val char: String,
    val strokes: ImmutableList<String>,
    val medians: ImmutableList<ImmutableList<Point>>,
)
// CharIndexItem.kt — 同理将 List 替换为 ImmutableList
// 解析层输出改为 .toImmutableList()
Point 是纯 val 原始类型 data class，Compose 编译器已自动判定稳定，无需处理。
为何不用 @Immutable: @Immutable 只是对编译器的"承诺"，无编译时约束。ImmutableList 在类型系统层面保证不可变。
影响范围: 添加依赖，修改 CharacterData.kt, CharIndexItem.kt, CharacterJsonParser.kt, JsonExtensions.kt
受影响测试文件清单（需将构造处 emptyList()/listOf() 改为 persistentListOf()/toPersistentList()）:
- HomeViewModelTest.kt:26 — item() 构造 CharIndexItem（pinyin, phrases 参数）
- PracticeViewModelTest.kt:106,115 — createTestItem() 构造 CharIndexItem + createTestCharacterData() 构造 CharacterData
- PracticeSessionOrchestratorTest.kt:22,33 — item() 构造 CharIndexItem + characterData() 构造 CharacterData
- PracticeSessionManagerTest.kt:10 — item() 构造 CharIndexItem（pinyin, phrases 参数）
- PickNextPracticeItemUseCaseTest.kt:22 — item() 构造 CharIndexItem（pinyin, phrases 参数）
共 5 个测试文件，均为构造函数参数适配，每处改动量 ≤ 3 行。
验收: flash 状态变化时 HanziCanvas 不触发重组（Layout Inspector 验证）；管理页列表中 character 参数不变的 item 不重组
---
OPT-08 PracticeUiState 重组范围精细化
位置: features/practice/viewmodel/PracticeViewModel.kt:90-104, features/practice/ui/PracticeScreen.kt:322-357
问题: PracticeUiState 包含 13 个字段，任何字段变更都触发整棵树的重组检查。
关键澄清: flashColor 是 TraceCanvas 组件的 claymorphism 的 backgroundColor 参数（PracticeScreen.kt:333），因此 flash 变化时 TraceCanvas 的外层 Box 需要重组来更新背景色。但其内部的 HanziTraceOverlay → HanziCanvas 不应因外层背景色变化而重组。
方案: 将 flashState 拆为独立 StateFlow
class PracticeViewModel(...) : ViewModel() {
    private val _uiState = MutableStateFlow(PracticeUiState())  // 不含 flashColorState
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()
    private val _flashState = MutableStateFlow(FlashState.None)
    val flashState: StateFlow<FlashState> = _flashState.asStateFlow()
}
UI 层分别 collect：
val uiState by viewModel.uiState.collectAsState()
val flashState by viewModel.flashState.collectAsState()
配合 OPT-07 的稳定性标注，flash 变化时：
- TraceCanvas 外层 Box 重组（背景色变化，正确行为）
- 内部 HanziTraceOverlay 参数（character, strokeIndex 等）未变 → 跳过重组
- 内部 HanziCanvas 参数未变 → 跳过重组
影响范围: PracticeViewModel.kt, PracticeScreen.kt
验收: flash 变化时 HanziTraceOverlay 和 HanziCanvas 不触发重组（Layout Inspector 验证），仅 TraceCanvas 外层 Box 重组
---
OPT-09 管理页过滤逻辑下沉至 ViewModel + 后台单遍历
位置: features/admin/ui/tabs/CharacterManagementTab.kt:88-96, features/admin/viewmodel/AdminCharacterViewModel.kt
问题:
- totalCount 和 visibleItems 分别遍历 3000+ 条目，matchesFilter 执行两遍
- remember 的 key 包含不稳定集合类型，引用变化触发重算
- 过滤计算在 composition 中执行（主线程）
方案: ViewModel 层过滤 + Dispatchers.Default + 单遍历实现
// ViewModel 中
private val _searchText = MutableStateFlow("")
private val _filterMode = MutableStateFlow(CharFilterMode.ALL)
private val _displayCount = MutableStateFlow(20)
data class FilteredCharacterResult(
    val totalCount: Int = 0,
    val visibleItems: List<CharIndexItem> = emptyList(),
)
val filteredResult: StateFlow<FilteredCharacterResult> = combine(
    _searchText, _filterMode, _displayCount, rawDataFlow
) { search, filter, count, data ->
    Params(search, filter, count, data)
}
.mapLatest { params ->
    withContext(Dispatchers.Default) {  // ← 显式后台线程
        // 单遍历实现：一次 filter 同时得到 total 和 visible
        val visible = ArrayList<CharIndexItem>(minOf(params.count, 200))
        var total = 0
        for (item in params.data.indexItems) {
            if (!matchesFilter(item, params.search, params.filter, ...)) continue
            total++
            if (visible.size < params.count) {
                visible.add(item)
            }
            // total++ 继续计数，但不再添加到 visible
        }
        FilteredCharacterResult(totalCount = total, visibleItems = visible)
    }
}
.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FilteredCharacterResult())
关键点:
- mapLatest + withContext(Dispatchers.Default) 确保过滤在后台线程执行，不阻塞主线程
- 单次 for 循环同时完成计数和取可见子集，避免双遍历
- UI 层 CharacterManagementTab 改为消费 filteredResult StateFlow，移除内部 remember 过滤逻辑
影响范围: AdminCharacterViewModel.kt, CharacterManagementTab.kt
验收: 搜索输入时主线程帧耗时较基线下降 ≥ 50%
---
P3 — 构建与包体积
OPT-10 启用 R8 代码压缩与资源收缩
位置: app/build.gradle.kts:25-28
方案:
release {
    isMinifyEnabled = true
    isShrinkResources = true
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
}
proguard-rules.pro:
# Room entities & DAOs
-keep class com.hanzi.learner.data.local.entity.** { *; }
-keep class com.hanzi.learner.data.local.dao.** { *; }
# TTS callback
-keep class * extends android.speech.tts.UtteranceProgressListener { *; }
影响范围: app/build.gradle.kts, 新增 app/proguard-rules.pro
验收: APK 大小较基线缩小 ≥ 25%，全功能回归通过
---
OPT-11 添加 Baseline Profile
方案: 添加 :baselineprofile 模块，编写 CUJ（冷启动/练习/管理），生成 baseline-prof.txt。
影响范围: 新增 :baselineprofile 模块
验收: 启动耗时较基线下降 ≥ 15%
---
P4 — 其他改进
OPT-12 索引 JSON 流式解析
位置: character_writer/data/CharacterJsonParser.kt, AssetCharacterRepository.kt:12
问题: char_index.json（3000+ 条目）先 readText() 为 String 再 JSONArray 构建 DOM 树，峰值内存 = 原始字符串 + DOM 树。
方案: android.util.JsonReader 流式解析。
注意: 若 OPT-04 的索引缓存已生效（仅首次解析），此项收益降低。可视 OPT-04 效果决定是否执行。
影响范围: CharacterJsonParser.kt, AssetCharacterRepository.kt
验收: 索引加载期间内存峰值较基线下降 ≥ 40%
---
OPT-13 Claymorphism overdraw 优化
位置: app/theme/Claymorphism.kt:37-44
问题: .shadow() + .background() + .border() 产生 3 层 overdraw。
方案: 合并为 graphicsLayer（shadow）+ drawBehind（background + border 一次绘制）。
附带清理: clayClickable（第 49 行）从 Modifier.composed {} 改为 @Composable 扩展函数。当前仅 PracticeScreen.kt:378 使用一处，ROI 低，但既然修改同文件一并处理。
影响范围: Claymorphism.kt
验收: Debug GPU overdraw 中 claymorphism 组件减少 1-2 层
---
OPT-14 代码清理杂项
| 项目 | 位置 | 内容 |
|------|------|------|
| 重复 import | AnimatedHanziCanvas.kt:10 | mutableStateOf 导入了两次 |
| exportSchema | AppDatabase.kt:29 | 改为 true，配置 KSP room.schemaLocation |
---
四、实施路线
第一阶段（Canvas 热循环）—— 低风险、高收益
├── OPT-01  HanziCanvas drawWithCache
├── OPT-02  TraceOverlay mutableStateList
└── OPT-03  StrokeMatcher Stroke 预计算
    预计 1-2 天
第二阶段（数据层缓存 + 模型稳定性）—— 低-中风险
├── OPT-04  Provider 级缓存 + LruCache + 独立 CacheController 接口
├── OPT-07  kotlinx-collections-immutable
│   └── 含 5 个测试文件适配
└── OPT-09  过滤逻辑下沉 ViewModel + Dispatchers.Default
    预计 1-2 天
第三阶段（启动 + 构建）—— 中风险（需回归测试）
├── OPT-05  启动路径改造 (Deferred container + loading gate + TTI 埋点)
├── OPT-10  R8 启用
└── OPT-06  数据库索引
    预计 1-2 天
第四阶段（精细调优）—— 低-中风险
├── OPT-08  UiState flash 拆分
├── OPT-11  Baseline Profile
├── OPT-13  Claymorphism overdraw + clayClickable
└── OPT-12  JSON 流式解析（视 OPT-04 效果决定）
    预计 1-2 天
第五阶段（清理）—— 低风险
└── OPT-14  杂项
    预计 < 0.5 天
预计总工作量: 5-8 天
---
五、范围外事项（独立跟踪）
| 事项 | 原因 |
|------|------|
| 工具链现代化 (AGP 7→8, Gradle 7→8, Kotlin 1.8→2.0, Compose BOM 升级) | 涉及 JDK 17 迁移 → Gradle 8 → AGP 8 namespace 强制 → K2 编译器。风险高、依赖链长，属于独立迁移项目 |
| TTS 生命周期 | 当前 rememberTtsSpeaker 已正确：remember(context) + DisposableEffect 管理。无性能问题 |
| Paging 3 集成 | 管理页已有手动分页（pageSize=20），当前可工作 |
---
六、每阶段验证清单
[ ] 采集/更新基线数据（首次 or 阶段完成后）
[ ] 记录测量环境：设备型号/OS 版本/电量/温度
[ ] 单元测试通过：./gradlew app:testDebugUnitTest
[ ] 架构守卫通过：ArchitectureGuardrailsTest 无新增违规
[ ] 功能回归：
    [ ] 练习流程（新学 + 复习 + 全部完成/禁用场景）
    [ ] 管理流程（搜索/筛选/禁用/启用/备份导入导出）
    [ ] 字库导入后练习使用新数据（OPT-04 失效策略验证）
[ ] 性能指标对比：与基线对比，确认改善率达标
[ ] 无新增 Lint 告警：./gradlew app:lint
---
七、Review 修正追溯
| 版本 | 来源 | 修正内容 |
|------|------|---------|
| v1→v2 | 第一轮 review (6 findings) | 删除 clayClickable 热路径虚高；删除依赖升级（独立项目）；删除 TTS（已正确）；缓存提升 Provider 级；改推荐 kotlinx-immutable；改用 drawWithCache |
| v2→v3 | 第二轮 review (5 findings) | OPT-04 补充 invalidate() + 导入触发点；OPT-09 补充 Dispatchers.Default；OPT-08 修正验收（flash 应触发 Box 重组但不触发 HanziCanvas）；OPT-05 从 best-effort 预热改为 Deferred container + loading gate；验收标准全部改为相对改善率 |
| v3→v4 | 第三轮 review (5 findings) | OPT-04 删除 notifier 备选路径，收敛为唯一显式 invalidate() 调用，补充 3 个测试桩修改成本；OPT-05 增加 TTI 双指标避免 loading gate 美化；OPT-09 落地单遍历实现代码替换双遍历示例；OPT-07/OPT-08 验收条件精确化为"flash 变化不触发 HanziCanvas 重组"；全局验收表增加测量环境记录要求 |
| v4→v5 | 第四轮 review (5 findings) | OPT-01 补充缓存分层取舍说明（单级 cache key 收益充分，拆分复杂度不值得）；OPT-04 将 invalidate() 从 CharacterRepositoryProvider 拆分为独立 CharacterCacheController 接口（ISP），消除 3 个测试桩修改；OPT-07 补充完整测试文件影响清单（5 个文件，非原文 3 个）；条目总数修正 15→14；指标名"冷启动首帧时间"修正为"启动耗时"（TotalTime 语义对齐） |