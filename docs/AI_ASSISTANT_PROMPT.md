# ProjectU - AI 助手协作提示词

> 📋 **用途**: 当你需要新的 AI 助手帮助开发 ProjectU 时，将本文档内容作为对话开头的上下文提供给 AI。

---

## 🎯 项目概述

**项目名称**: ProjectU  
**项目类型**: Kotlin Multiplatform 跨平台 Pixiv 客户端  
**支持平台**: Android (API 24+) 和 Desktop (Windows/Mac/Linux)  
**技术栈**: Kotlin 2.2.20 + Compose Multiplatform 1.9.1 + Clean Architecture + MVI

---

## 📂 项目结构

```
ProjectU/
├── composeApp/              # UI 层 (Presentation Layer)
│   ├── src/
│   │   ├── commonMain/      # 跨平台 UI 代码
│   │   │   ├── kotlin/com/projectu/
│   │   │   │   ├── App.kt   # 应用入口
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screens/        # 页面 (MVI 架构)
│   │   │   │   │   ├── components/     # 可复用组件
│   │   │   │   │   ├── theme/          # Material 3 主题
│   │   │   │   │   └── localization/   # 多语言管理
│   │   │   │   └── di/      # Koin DI 配置
│   │   │   └── composeResources/       # 多语言资源
│   │   ├── androidMain/     # Android 平台特定
│   │   └── desktopMain/     # Desktop 平台特定
│   └── build.gradle.kts
│
├── shared/                  # 业务逻辑层 (Domain + Data)
│   ├── src/
│   │   ├── commonMain/
│   │   │   ├── kotlin/com/projectu/shared/
│   │   │   │   ├── domain/      # 领域层
│   │   │   │   │   ├── model/       # 领域模型
│   │   │   │   │   ├── repository/  # Repository 接口
│   │   │   │   │   └── usecase/     # 业务用例
│   │   │   │   ├── data/        # 数据层
│   │   │   │   │   ├── repository/  # Repository 实现
│   │   │   │   │   ├── remote/      # Pixiv Web API
│   │   │   │   │   │   ├── api/         # API 接口
│   │   │   │   │   │   ├── dto/         # 数据传输对象
│   │   │   │   │   │   └── mapper/      # DTO → Domain
│   │   │   │   │   ├── local/       # 本地数据
│   │   │   │   │   │   ├── database/    # Room 数据库
│   │   │   │   │   │   ├── dao/         # DAO
│   │   │   │   │   │   └── entity/      # 数据库实体
│   │   │   │   │   └── cache/       # 文件缓存
│   │   │   │   ├── util/        # 工具类
│   │   │   │   └── di/          # DI 配置
│   │   ├── androidMain/     # Android 平台特定
│   │   └── desktopMain/     # Desktop 平台特定
│   └── build.gradle.kts
│
├── docs/                    # 项目文档
│   ├── project/
│   │   └── 项目架构参考文档.md      # 完整架构文档 ⭐
│   ├── settings/
│   │   └── 设置系统架构.md
│   ├── shared/
│   │   └── TECH_STACK.md           # 技术栈文档
│   └── guides/
│       ├── API测试工具使用指南.md   # API 文档
│       └── 自适应布局指南.md
│
├── build.gradle.kts         # 根项目配置
├── settings.gradle.kts      # 模块配置
├── gradle.properties        # Gradle 属性
└── gradle/
    └── libs.versions.toml   # 依赖版本管理 ⭐
```

---

## 🏗️ 架构设计

### Clean Architecture 三层架构

```
┌─────────────────────────────────────────┐
│   Presentation Layer (composeApp)      │
│   • Screens (Composable)               │
│   • ViewModels (MVI)                   │
│   • Navigation (Voyager)               │
└─────────────────────────────────────────┘
                   ↓ 依赖
┌─────────────────────────────────────────┐
│   Domain Layer (shared/domain)         │
│   • Models (纯 Kotlin)                 │
│   • Repository Interfaces              │
│   • UseCases (业务逻辑)                │
└─────────────────────────────────────────┘
                   ↑ 实现
┌─────────────────────────────────────────┐
│   Data Layer (shared/data)             │
│   • Repository Implementations         │
│   • Remote: Pixiv Web API (Ktor)      │
│   • Local: Room Database               │
│   • Cache: File System (Okio)         │
└─────────────────────────────────────────┘
```

### MVI (Model-View-Intent) 模式

每个 Screen 都遵循 MVI 单向数据流：

```kotlin
// State - 屏幕状态
data class XxxScreenState(...)

// Intent - 用户操作
sealed interface XxxIntent { ... }

// ViewModel - 状态管理
class XxxViewModel : ViewModel() {
    val state: StateFlow<XxxScreenState>
    fun handleIntent(intent: XxxIntent)
}

// Screen - Composable UI
class XxxScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<XxxViewModel>()
        val state by viewModel.state.collectAsState()
        // UI...
    }
}
```

---

## 🔑 核心技术栈

> 📚 完整技术栈: [docs/shared/TECH_STACK.md](shared/TECH_STACK.md)

### 主要技术

| 技术 | 版本 | 用途 |
|-----|------|------|
| Kotlin | 2.2.20 | 编程语言 |
| Compose Multiplatform | 1.9.1 | UI 框架 |
| Koin | 4.1.1 | 依赖注入 |
| Ktor | 3.3.1 | HTTP 客户端 |
| Room | 2.8.3 | 本地数据库 |
| Voyager | 1.1.0-beta03 | 导航框架 |
| Coil | 3.3.0 | 图片加载 |

### 架构模式
- **Clean Architecture** - Domain / Data / Presentation
- **MVI** - Model-View-Intent 单向数据流
- **Repository Pattern** - 数据访问抽象
- **Dependency Injection** - Koin 跨平台 DI

### Pixiv API 集成

> 🌐 API 详细状态: [docs/shared/API_STATUS.md](shared/API_STATUS.md)

已集成 10 个主要 API 模块：
- ✅ **IllustApi** - 作品详情、搜索、推荐、发现、Ugoira
- ✅ **UserApi** - 用户信息、关注、作品列表
- ✅ **BookmarkApi** - 收藏管理、批量操作
- ✅ **RankingApi** - 各类排行榜
- ✅ **CommentApi** - 评论相关
- ✅ **NovelApi** - 小说相关
- ✅ **NovelSeriesApi** - 小说系列相关
- ✅ **TagApi** - 标签相关
- ✅ **MarkerApi** - 书签（稍后再读）
- ✅ **FollowApi** - 关注（已关注用户的作品、追更）

认证方式: PHPSESSID Cookie + CSRF Token (自动刷新)

---

## ✅ 已完成功能

### 基础架构
- ✅ Kotlin Multiplatform 项目结构 (Android + Desktop)
- ✅ Clean Architecture 三层分离
- ✅ MVI 架构模式
- ✅ Koin 依赖注入 (含平台特定实现)

### UI 层
- ✅ Material 3 主题 (深/浅色模式)
- ✅ 响应式布局 (手机/平板/桌面自适应)
- ✅ HomeScreen - 主屏幕 + 底部/侧边导航
- ✅ SettingsScreen - 设置页面
- ✅ UgoiraPlayer - Pixiv 动图播放器组件

### 数据层
- ✅ **Pixiv Web API 完整集成** (插画/用户/收藏/排行榜)
- ✅ Room 数据库 (跨平台)
- ✅ DataStore 配置存储
- ✅ Ugoira 缓存系统 (ZIP 下载/解压)

### 多语言
- ✅ 5 种语言支持 (简中/繁中/英文/日文/韩文)
- ✅ LocaleManager - 语言切换和持久化
- ✅ 应用语言 & Pixiv API 语言分离

---

## 🚧 待开发功能 (优先级排序)

### 高优先级
1. **登录认证系统** 🔐
   - PHPSESSID 配置界面
   - Token 自动刷新
   - 登录状态管理

2. **作品列表页面** 🖼️
   - 推荐作品流
   - 瀑布流布局 (StaggeredGrid)
   - 图片懒加载
   - 分页加载

3. **作品详情页面** 📄
   - 大图预览和缩放
   - 作品信息展示
   - 相关作品推荐

### 中优先级
4. **搜索功能** 🔍
5. **用户主页** 👤
6. **排行榜页面** 🏆

### 低优先级
7. **发现页面** 🌟
8. **离线缓存优化** 💾

---

## 📖 重要文档

请优先阅读以下文档以了解项目细节：

1. **`docs/project/项目架构参考文档.md`** ⭐⭐⭐
   - 最全面的架构文档
   - 包含所有技术细节和代码示例
   - 开发指南和注意事项

2. **`docs/guides/API测试工具使用指南.md`** ⭐⭐
   - Pixiv API 完整文档
   - 所有 API 端点说明
   - 使用示例和测试工具

3. **`README.md`** ⭐
   - 项目概览
   - 快速开始指南

4. **`gradle/libs.versions.toml`**
   - 所有依赖版本

---

## 🎯 协作指南

### 当你需要 AI 帮助时

#### 1. 添加新功能
```
我需要在 ProjectU 项目中实现 [功能名称]。

背景：
- 项目是 Kotlin Multiplatform (Android + Desktop)
- 使用 Clean Architecture + MVI 架构
- UI 层在 composeApp，业务逻辑在 shared

需求：
[详细描述功能需求]

请帮我：
1. 设计 MVI 组件 (State/Intent/ViewModel)
2. 实现 UI (Composable Screen)
3. 集成到现有导航系统

请遵循项目现有的架构模式和代码风格。
```

#### 2. 修复问题
```
ProjectU 项目中遇到 [问题描述]。

环境：
- Platform: [Android / Desktop]
- 相关文件: [文件路径]

错误信息：
[错误日志]

请帮我分析问题原因并提供解决方案。
```

#### 3. API 集成
```
我需要在 ProjectU 中调用 Pixiv API 的 [功能名称]。

当前状态：
- Pixiv API 已集成 (PixivApi, IllustApi, UserApi 等)
- 使用 Ktor 3.3.1
- 认证方式: PHPSESSID + CSRF Token

需求：
[API 功能描述]

请帮我：
1. 定义 DTO (shared/data/remote/dto/)
2. 添加 API 方法 (shared/data/remote/api/)
3. 实现 Repository (shared/data/repository/)
4. 创建 Mapper (DTO → Domain Model)
```

#### 4. UI 组件开发
```
我需要为 ProjectU 创建一个 [组件名称] 组件。

要求：
- 使用 Compose Multiplatform
- Material Design 3 风格
- 支持响应式布局 (手机/平板/桌面)

功能：
[组件功能描述]

请提供 Composable 函数实现。
```

---

## 🔧 开发规范

### 文件命名
- **Screen**: `XxxScreen.kt`
- **ViewModel**: `XxxViewModel.kt`
- **Repository**: `XxxRepository.kt` (接口), `XxxRepositoryImpl.kt` (实现)
- **DTO**: `XxxDto.kt`
- **Entity**: `XxxEntity.kt`
- **UseCase**: `XxxUseCase.kt`

### 代码风格
- 使用 Kotlin 官方代码风格
- 优先使用 `StateFlow` 而非 `LiveData`
- 使用 `Result<T>` 封装可能失败的操作
- 所有挂起函数使用 `suspend` 关键字

### 平台差异
```kotlin
// commonMain - 声明
expect fun platformSpecificFunction(): String

// androidMain - Android 实现
actual fun platformSpecificFunction(): String = "Android"

// desktopMain - Desktop 实现
actual fun platformSpecificFunction(): String = "Desktop"
```

### MVI 模板
```kotlin
// State
data class XxxScreenState(
    val data: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// Intent
sealed interface XxxIntent {
    data object LoadData : XxxIntent
    data class OnItemClick(val id: String) : XxxIntent
}

// ViewModel
class XxxViewModel(
    private val repository: XxxRepository
) : ViewModel() {
    private val _state = MutableStateFlow(XxxScreenState())
    val state: StateFlow<XxxScreenState> = _state.asStateFlow()
    
    fun handleIntent(intent: XxxIntent) {
        when (intent) {
            is XxxIntent.LoadData -> loadData()
            is XxxIntent.OnItemClick -> handleItemClick(intent.id)
        }
    }
    
    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.getData()
                .onSuccess { data ->
                    _state.update {
                        it.copy(data = data, isLoading = false, error = null)
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(isLoading = false, error = error.message)
                    }
                }
        }
    }
}
```

---

## 🚨 重要注意事项

### 1. 依赖注入
- 所有依赖通过 Koin 注入
- ViewModel 使用 `koinViewModel()` 获取
- Repository 通过构造函数注入

### 2. 协程使用
- UI 层: `viewModelScope.launch { }`
- Repository 层: `withContext(Dispatchers.IO) { }`
- Room 自动处理，无需手动切换线程

### 3. 错误处理
```kotlin
// Repository 层
suspend fun getData(): Result<Data> {
    return try {
        val data = api.getData()
        Result.success(data)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// ViewModel 层
result
    .onSuccess { data -> /* 处理成功 */ }
    .onFailure { error -> /* 处理错误 */ }
```

### 4. Pixiv API 使用
- 需要 PHPSESSID (从浏览器 Cookie 获取)
- CSRF Token 会自动刷新
- 所有 API 调用都在 `shared/data/remote/api/` 中定义

### 5. 多语言
- 在 `composeApp/src/commonMain/composeResources/values*/strings.xml` 添加字符串
- 使用 `stringResource(Res.string.xxx)` 访问
- 所有语言文件必须包含相同的 key

---

## 💡 快速参考

### 项目路径
- **UI 代码**: `composeApp/src/commonMain/kotlin/com/projectu/ui/`
- **ViewModel**: `composeApp/src/commonMain/kotlin/com/projectu/ui/screens/[feature]/`
- **Repository**: `shared/src/commonMain/kotlin/com/projectu/shared/data/repository/`
- **API**: `shared/src/commonMain/kotlin/com/projectu/shared/data/remote/api/`
- **数据库**: `shared/src/commonMain/kotlin/com/projectu/shared/data/local/`
- **DI 配置**: `composeApp/src/[platform]Main/kotlin/com/projectu/di/`

### 常用命令
```bash
# Android
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug

# Desktop
./gradlew :composeApp:run
./gradlew :composeApp:createDistributable

# 清理
./gradlew clean
```

### 关键依赖
```kotlin
// Koin DI
implementation(libs.koin.core)
implementation(libs.koin.compose)

// Ktor 网络
implementation(libs.ktor.client.core)
implementation(libs.ktor.client.cio)

// Room 数据库
implementation(libs.room.runtime)
ksp(libs.room.compiler)

// Compose UI
implementation(compose.material3)
implementation(compose.components.resources)

// Voyager 导航
implementation(libs.voyager.navigator)
```

---

## 📞 如何使用本文档

### 开始新对话时

**步骤 1**: 将本文档内容复制给 AI  
**步骤 2**: 说明你的具体需求  
**步骤 3**: 提供相关的代码文件（如果需要）  

**示例对话开场**:

```
[粘贴本文档全部内容]

---

基于上述 ProjectU 项目的背景，我现在需要实现作品列表页面。

需求：
1. 显示推荐作品列表
2. 使用瀑布流布局 (StaggeredGrid)
3. 支持分页加载
4. 点击作品进入详情页

请帮我设计并实现这个功能，包括：
- MVI 组件 (State/Intent/ViewModel)
- Composable UI
- Repository 集成

请遵循项目现有的架构模式。
```

---

## 🎓 学习资源

如果需要了解更多技术细节：

- **Kotlin Multiplatform**: https://kotlinlang.org/docs/multiplatform.html
- **Compose Multiplatform**: https://www.jetbrains.com/lp/compose-multiplatform/
- **Koin**: https://insert-koin.io/docs/reference/koin-mp/kmp
- **Ktor**: https://ktor.io/docs/client.html
- **Room**: https://developer.android.com/kotlin/multiplatform/room
- **Voyager**: https://voyager.adriel.cafe/

---

> 📝 **提示**: 本文档会随项目更新而更新，请定期检查最新版本。  
> 📅 **最后更新**: 2025-10-30  
> 🔗 **项目文档**: `docs/project/项目架构参考文档.md`
