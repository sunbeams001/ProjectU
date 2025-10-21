# ProjectU - Pixiv Kotlin Multiplatform Client

一个使用 Kotlin Compose Multiplatform 开发的跨平台 Pixiv 客户端应用。

## 项目简介

ProjectU 是一个现代化的 Pixiv 客户端，支持 Android 和 Desktop (Windows/Mac/Linux) 平台，采用最新的技术栈和架构设计。

### 主要特性

- 🎨 **跨平台支持**: Android 和 Desktop 平台
- 🌍 **多语言支持**: 简体中文、繁体中文、英文、日文、韩文
- 🎭 **Material Design 3**: 现代化的 UI 设计，支持浅色/深色主题
- 🏗️ **Clean Architecture**: MVI 架构 + 分层设计
- 🎬 **Ugoira 支持**: 完整的 Pixiv 动图播放支持
- 📱 **响应式设计**: 适配不同屏幕尺寸

### 核心功能

- ✅ **Pixiv API 集成** - 完整的 Pixiv Web API 支持
- ✅ 作品浏览和搜索
- ✅ 用户关注和收藏管理
- ✅ 排行榜查看
- ✅ 作品详情展示
- ✅ Ugoira 动图播放
- ⏳ 离线缓存（计划中）

## 技术栈

### 核心框架

- **Kotlin 2.2.20** - 编程语言
- **Compose Multiplatform 1.9.1** - UI 框架
- **Kotlin Coroutines 1.10.2** - 异步编程

### 架构和依赖注入

- **MVI Architecture** - 单向数据流架构
- **Clean Architecture** - 分层架构设计
- **Koin 4.1.1** - 依赖注入框架

### 网络和数据

- **Ktor 3.3.1** - HTTP 客户端
- **Pixiv Web API** - 基于 Ktor 的 Pixiv API 客户端
- **kotlinx-serialization 1.9.0** - JSON 序列化
- **Room 2.8.2** - 本地数据库
- **DataStore 1.1.7** - 键值对存储
- **Okio 3.16.2** - 文件和 ZIP 处理

### UI 和导航

- **Voyager 1.1.0-beta03** - 导航框架
- **Coil 3.3.0** - 图片加载
- **Moko Resources 0.25.1** - 多语言资源管理

## 项目结构

```
ProjectU/
├── composeApp/              # Compose UI 代码
│   ├── src/
│   │   ├── commonMain/      # 共享 UI 代码
│   │   │   ├── kotlin/
│   │   │   │   ├── ui/      # UI 层
│   │   │   │   │   ├── screens/     # 页面
│   │   │   │   │   ├── components/  # 组件
│   │   │   │   │   ├── theme/       # 主题
│   │   │   │   │   └── navigation/  # 导航
│   │   │   │   └── di/      # 依赖注入
│   │   │   └── resources/   # 多语言资源
│   │   ├── androidMain/     # Android 特定代码
│   │   └── desktopMain/     # Desktop 特定代码
│   └── build.gradle.kts
│
├── shared/                  # 共享业务逻辑
│   ├── src/
│   │   ├── commonMain/
│   │   │   ├── kotlin/
│   │   │   │   ├── domain/      # 领域层
│   │   │   │   │   ├── model/       # 数据模型
│   │   │   │   │   ├── repository/  # Repository 接口
│   │   │   │   │   └── usecase/     # 业务用例
│   │   │   │   ├── data/        # 数据层
│   │   │   │   │   ├── repository/  # Repository 实现
│   │   │   │   │   ├── remote/      # 网络 API
│   │   │   │   │   ├── local/       # 本地数据库
│   │   │   │   │   └── cache/       # 缓存策略
│   │   │   │   └── util/        # 工具类
│   │   ├── androidMain/
│   │   └── desktopMain/
│   └── build.gradle.kts
│
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 开始使用

### 环境要求

- JDK 11 或更高版本
- Android Studio Ladybug | 2024.2.1 或更高版本
- Kotlin 2.0.21

### 构建项目

#### Android

```bash
./gradlew :composeApp:assembleDebug
```

#### Desktop

```bash
./gradlew :composeApp:run
```

### 运行项目

#### Android

在 Android Studio 中打开项目，选择 Android 运行配置并运行。

#### Desktop

```bash
./gradlew :composeApp:runDistributable
```

## 架构设计

### MVI 架构

每个 Screen 遵循 MVI (Model-View-Intent) 模式：

```kotlin
// State - 屏幕状态
data class HomeScreenState(
    val isLoading: Boolean = false,
    val artworks: List<Artwork> = emptyList(),
    val error: String? = null
)

// Intent - 用户意图
sealed interface HomeScreenIntent {
    data object LoadArtworks : HomeScreenIntent
    data class OnArtworkClick(val id: String) : HomeScreenIntent
}

// ViewModel - 状态管理
class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(HomeScreenState())
    val state = _state.asStateFlow()
    
    fun handleIntent(intent: HomeScreenIntent) { ... }
}
```

### 数据流

```
UI (Compose) 
  ↓ emit Intent
ViewModel 
  ↓ call UseCase
UseCase (Business Logic)
  ↓ fetch from Repository
Repository 
  ↓ Remote API / Local DB
Data Sources
```

## Ugoira 动图支持

Pixiv 的 Ugoira (うごイラ) 是一种特殊的动图格式：

1. 从 API 获取动图元数据（ZIP URL + 帧延迟时间）
2. 下载 ZIP 文件并解压获取所有帧图片
3. 使用 `UgoiraPlayer` 组件按时序播放
4. 支持播放/暂停、速度调节等控制

## 多语言支持

使用 Moko Resources 实现多语言支持，资源文件位于：

```
composeApp/src/commonMain/resources/MR/
├── base/           # 英文（默认）
├── zh-rCN/        # 简体中文
├── zh-rTW/        # 繁体中文
├── ja/            # 日文
└── ko/            # 韩文
```

## 开发路线图

- [x] 项目框架搭建
- [x] 基础 UI 组件和主题
- [x] 多语言系统集成
- [x] Koin 依赖注入配置
- [x] Ugoira 动图播放器
- [x] **Pixiv Web API 集成** ✨ 新完成
- [ ] 登录认证实现
- [ ] 作品浏览功能
- [ ] 搜索和发现功能
- [ ] 排行榜功能
- [ ] 用户个人资料
- [ ] 离线缓存优化

## 📖 Pixiv API 文档

已成功集成 Pixiv Web API，提供完整的作品、用户、收藏、排行榜等功能支持。

- **快速开始**: 查看 [PIXIV_API_QUICKSTART.md](PIXIV_API_QUICKSTART.md)
- **完整文档**: 查看 [PIXIV_API_INTEGRATION.md](PIXIV_API_INTEGRATION.md)
- **集成报告**: 查看 [INTEGRATION_SUMMARY.md](INTEGRATION_SUMMARY.md)

### API 功能概览

| 模块 | 功能 | 状态 |
|------|------|------|
| 插画 API | 详情、搜索、推荐、发现 | ✅ 完成 |
| 用户 API | 信息、关注、作品列表 | ✅ 完成 |
| 收藏 API | 添加、删除、批量操作 | ✅ 完成 |
| 排行榜 API | 日榜、周榜、月榜等 | ✅ 完成 |
| 评论 API | - | ⏳ 计划中 |
| 小说 API | - | ⏳ 计划中 |

## 许可证

本项目仅供学习和研究使用。

## 致谢

- [Pixiv](https://www.pixiv.net/) - 提供优秀的创作平台
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) - 跨平台技术
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) - UI 框架

