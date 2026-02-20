# Hanzi Learner（汉字学习应用）

一个基于间隔重复算法的汉字书写学习应用，帮助用户通过书写练习掌握汉字笔画和结构。

## 功能特性

### 核心学习功能
- **笔画书写练习**：在屏幕上跟随示范书写汉字，系统自动识别笔画准确性
- **笔画动画演示**：动态展示标准笔顺，帮助用户掌握正确书写顺序
- **间隔重复算法**：基于 SM-2 算法智能安排复习计划，优化记忆效果
- **语音朗读**：集成 TTS 引擎。每进入一个汉字的练习界面就会自动朗读汉字和词语发音。点击练习界面的朗读按钮也可以朗读发音。

### 学习管理
- **学习进度追踪**：记录每个汉字的书写正确率和练习次数
- **自定义词语**：支持为每个汉字配置常用词语，加深记忆
- **禁用字管理**：可暂时禁用已掌握的汉字，专注学习新字
- **数据备份/恢复**：支持学习数据导出导入，防止数据丢失

### 管理员功能
- **笔画数据管理**：处理汉字笔画和部首数据
- **学习数据统计**：查看整体学习进度和统计数据

## 技术栈

- **平台**：Android（minSdk 30, targetSdk 33）
- **编程语言**：Kotlin
- **UI 框架**：Jetpack Compose + Material3
- **架构模式**：MVVM + Clean Architecture
- **本地存储**：Room 数据库
- **依赖注入**：手动依赖容器（AppContainer）
- **构建工具**：Gradle + Kotlin DSL

## 项目结构

```
hanzi-learner/
├── app/src/main/java/com/hanzi/learner/
│   ├── MainActivity.kt                 # 应用入口
│   ├── app/                            # 应用级组件
│   │   ├── AppContainer.kt             # 依赖容器（DI）
│   │   ├── HanziLearnerApp.kt          # 应用根组件
│   │   ├── navigation/                 # 导航配置
│   │   └── theme/                      # 主题和样式
│   ├── character_writer/               # 汉字书写核心模块
│   │   ├── data/                       # 汉字数据层
│   │   ├── match/                      # 笔画匹配算法
│   │   ├── model/                      # 数据模型
│   │   ├── practice/                   # 书写练习组件
│   │   └── render/                     # 渲染引擎
│   ├── data/                           # 数据层
│   │   ├── BackupManager.kt            # 备份管理器
│   │   ├── local/                      # 本地数据库（Room）
│   │   │   ├── dao/                    # 数据访问对象
│   │   │   └── entity/                 # 实体定义
│   │   ├── model/                      # 数据模型
│   │   └── repository/                 # 仓库接口和实现
│   ├── features/                       # 功能模块
│   │   ├── home/                       # 首页
│   │   ├── practice/                   # 练习模块
│   │   │   ├── domain/                 # 用例和领域逻辑
│   │   │   ├── ui/                     # UI 组件
│   │   │   └── viewmodel/              # 视图模型
│   │   └── admin/                      # 管理员功能
│   │       ├── backup/                 # 备份处理
│   │       ├── domain/                 # 领域逻辑
│   │       ├── model/                  # 模型定义
│   │       ├── repository/             # 仓库
│   │       ├── ui/                     # UI 组件
│   │       └── viewmodel/              # 视图模型
│   └── speech/                         # 语音合成模块
├── app/src/main/assets/                # 应用资源
│   ├── char_data/                      # 单个汉字笔画数据
│   ├── char_index.json                 # 汉字索引
│   ├── lesson_chars.txt                # 课程汉字列表
│   └── phrases.json                    # 词语配置
├── data/                               # 原始数据，应用内直接导入
├── tools/                              # 工具脚本
│   └── generate_assets.py              # 资源生成工具
└── app/src/test/                       # 单元测试

```

## 模块职责说明

### 1. app 模块
负责应用级别的配置和初始化，包括主题、导航和依赖注入容器。

| 文件/目录 | 职责 |
|-----------|------|
| `AppContainer.kt` | 集中管理所有依赖的创建和生命周期 |
| `HanziLearnerApp.kt` | 根可组合函数，设置导航和全局状态 |
| `navigation/` | 定义应用导航图和路由 |
| `theme/` | 定义颜色、字体、形状等主题配置 |

### 2. character_writer 模块
核心汉字书写引擎，负责笔画渲染、书写识别和动画演示。

| 子模块 | 职责 |
|--------|------|
| `data/` | 加载和管理汉字笔画数据（JSON 格式） |
| `match/` | 笔画匹配算法，评估用户书写准确性 |
| `model/` | 笔画、点等基础数据结构 |
| `practice/` | 书写练习的交互组件 |
| `render/` | 使用 Canvas 渲染汉字和笔画动画 |

### 3. data 模块
数据持久化层，使用 Room 数据库存储学习进度和应用设置。

| 子模块 | 职责 |
|--------|------|
| `local/` | Room 数据库定义、DAO 接口、实体类 |
| `repository/` | 仓库模式实现，封装数据源访问 |
| `BackupManager.kt` | 处理数据备份和恢复逻辑 |

### 4. features 模块
按功能划分的业务模块，每个功能模块内部采用 Clean Architecture。

| 功能模块 | 职责 |
|----------|------|
| `home/` | 首页展示，学习概览和快捷入口 |
| `practice/` | 练习会话管理，用例编排，UI 交互 |
| `admin/` | 数据管理、备份操作、学习统计 |

每个功能模块内部结构：
- `domain/` - 用例（UseCase）、领域服务、业务逻辑
- `ui/` - Compose UI 组件
- `viewmodel/` - 状态管理和业务逻辑编排

### 5. speech 模块
语音合成功能，封装 Android TTS API。

| 文件 | 职责 |
|------|------|
| `TtsSpeaker.kt` | TTS 引擎封装和管理 |
| `TtsSpeakerContract.kt` | 接口定义，便于测试和替换 |
| `TtsSpeakerComposables.kt` | Compose 集成组件 |

## 如何扩展功能

### 添加新的练习模式

1. **在 `features/practice/domain/` 创建用例**
   - 定义新练习模式的业务逻辑
   - 实现练习项选择、难度计算等逻辑

2. **在 `features/practice/ui/` 创建 UI 组件**
   - 使用 Jetpack Compose 构建界面
   - 遵循现有的状态管理模式

3. **更新导航配置**
   - 在 `app/navigation/` 添加新路由
   - 在 `AppContainer.kt` 添加依赖

### 添加新的数据源

1. **定义数据模型**
   - 在 `character_writer/model/` 或 `data/model/` 创建数据类

2. **实现数据访问**
   - 在 `character_writer/data/` 创建新的 Repository 实现
   - 遵循 `CharacterRepository` 接口模式

3. **更新 Repository 选择器**
   - 修改 `CharacterRepositorySelector.kt` 以支持新数据源

### 扩展间隔重复算法

1. **修改算法参数**
   - 在 `data/repository/` 中调整 SM-2 算法实现

2. **添加新的复习策略**
   - 在 `features/practice/domain/` 创建新的用例
   - 实现自定义的复习调度逻辑

### 添加新功能模块

1. **创建模块目录结构**
   ```
   features/new_feature/
   ├── domain/
   ├── ui/
   └── viewmodel/
   ```

2. **遵循 Clean Architecture 分层**
   - Domain 层不依赖其他层
   - ViewModel 依赖 Domain 层
   - UI 层依赖 ViewModel

3. **注册依赖**
   - 在 `AppContainer.kt` 中添加新模块的依赖初始化

### 自定义主题和样式

1. **修改颜色方案**
   - 在 `app/theme/Color.kt` 中定义新颜色

2. **更新主题配置**
   - 在 `app/theme/Theme.kt` 中应用新颜色方案

3. **自定义组件样式**
   - 在 `app/theme/Type.kt` 中定义字体样式

### 添加单元测试

1. **在 `app/src/test/` 创建测试类**
   - 遵循现有测试的命名规范：`XxxTest.kt`

2. **架构测试**
   - 在 `architecture/ArchitectureGuardrailsTest.kt` 中添加架构约束

## 数据资源说明

### 汉字数据来源
- 使用 Makemeahanzi 项目的开源数据
- 内置初始笔画数据以 JSON 格式存储在 `app/src/main/assets/char_data/`
- 每个汉字一个文件，命名格式：`u{codepoint}.json`

### 资源生成
使用 `tools/generate_assets.py` 脚本处理原始数据：
```bash
./gradlew generateHanziAssets
```

### 词语表格式
键之间不要包含空格
```json
{
  "字": ["词语1", "词语2"], ...
}
```

## 构建和运行

### 环境要求
- Android Studio Hedgehog 或更新版本
- JDK 11 或更高
- Android SDK 33

### 构建命令
```bash
# 调试构建
./gradlew assembleDebug
# 发行构建
./gradlew assemblerelease
# 运行测试
./gradlew test

# 生成资源
./gradlew generateHanziAssets
```
