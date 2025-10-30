# ProjectU - Pixiv Kotlin Multiplatform Client

> 📅 最后更新: 2025-10-30  
> 🚀 当前版本: v0.1.0-alpha  
> 📱 支持平台: Android (API 24+) | Desktop (Windows/Mac/Linux)

一个使用 Kotlin Compose Multiplatform 开发的现代化跨平台 Pixiv 客户端。

---

## 🎯 项目简介

ProjectU 是一个功能完整的 Pixiv 客户端应用，采用最新的跨平台技术栈和最佳架构实践。

### ✨ 核心特性

- 🎨 **真正的跨平台** - 一套代码，支持 Android 和 Desktop
- �️ **Clean Architecture** - MVI 模式 + 三层架构设计
- 🎭 **Material Design 3** - 现代化 UI，深色/浅色主题
- 🌍 **完整多语言** - 简中/繁中/英文/日文/韩文
- � **Pixiv Web API** - 完整集成官方 Web API
- 🎬 **Ugoira 支持** - Pixiv 动图下载和播放
- 📱 **响应式设计** - 手机/平板/桌面自适应布局
- 💾 **数据持久化** - Room 数据库 + DataStore 配置

## 🛠️ 技术栈

> 完整技术栈请查看: [docs/shared/TECH_STACK.md](docs/shared/TECH_STACK.md)

### 核心技术

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

- **Clean Architecture** - Domain / Data / Presentation 三层分离
- **MVI** - Model-View-Intent 单向数据流
- **Repository Pattern** - 数据访问抽象层
- **Dependency Injection** - Koin 跨平台 DI

## 📁 项目结构

```
ProjectU/
├── composeApp/                  # 表现层 (Presentation Layer)
│   ├── src/
│   │   ├── commonMain/          # 跨平台 UI 代码
│   │   │   ├── kotlin/com/projectu/
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screens/         # 页面 (MVI)
│   │   │   │   │   ├── components/      # 可复用组件
│   │   │   │   │   ├── theme/           # Material 3 主题
│   │   │   │   │   └── localization/    # 多语言管理
│   │   │   │   └── di/                  # Koin DI
│   │   │   └── composeResources/        # 多语言资源
│   │   ├── androidMain/         # Android 平台
│   │   └── desktopMain/         # Desktop 平台
│   └── build.gradle.kts
│
├── shared/                      # 业务逻辑层 (Domain + Data)
│   ├── src/
│   │   ├── commonMain/
│   │   │   ├── kotlin/com/projectu/shared/
│   │   │   │   ├── domain/          # 领域层
│   │   │   │   │   ├── model/           # 领域模型
│   │   │   │   │   ├── repository/      # Repository 接口
│   │   │   │   │   └── usecase/         # 业务用例
│   │   │   │   ├── data/            # 数据层
│   │   │   │   │   ├── repository/      # Repository 实现
│   │   │   │   │   ├── remote/          # Pixiv Web API
│   │   │   │   │   ├── local/           # Room 数据库
│   │   │   │   │   └── cache/           # 文件缓存
│   │   │   │   └── util/            # 工具类
│   │   ├── androidMain/         # Android 平台
│   │   └── desktopMain/         # Desktop 平台
│   └── build.gradle.kts
│
├── docs/                        # 项目文档
│   ├── project/                 # 架构文档
│   ├── pixiv/                   # API 文档
│   ├── shared/                  # 共用文档片段
│   └── guides/                  # 开发指南
│
├── gradle/
│   └── libs.versions.toml       # 依赖版本管理
│
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

> 📖 详细架构说明: [docs/project/项目架构参考文档.md](docs/project/项目架构参考文档.md)

## 🚀 快速开始

### 环境要求

| 工具 | 版本要求 |
|-----|---------|
| JDK | 11+ |
| Android Studio | Ladybug 2024.2.1+ |
| Gradle | 8.x (自动下载) |

### 克隆项目

```bash
git clone https://github.com/yourusername/ProjectU.git
cd ProjectU
```

### 构建和运行

#### Android

```bash
# 方式 1: 使用脚本
./build-android.sh    # Linux/Mac
build-android.bat     # Windows

# 方式 2: 使用 Gradle
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug

# 方式 3: Android Studio
# 打开项目后，点击 Run 按钮
```

#### Desktop

```bash
# 方式 1: 使用脚本
./build-desktop.sh    # Linux/Mac
build-desktop.bat     # Windows

# 方式 2: 使用 Gradle
./gradlew :composeApp:run

# 打包为可分发应用
./gradlew :composeApp:createDistributable
```

### 配置 Pixiv API

1. 在浏览器中登录 [Pixiv](https://www.pixiv.net/)
2. 打开开发者工具 → Application/Storage → Cookies
3. 复制 `PHPSESSID` 的值
4. 在应用设置中填入 PHPSESSID

> 📖 详细配置: [docs/pixiv/PIXIV_API_集成指南.md](docs/pixiv/PIXIV_API_集成指南.md)

## 🏗️ 架构设计

### Clean Architecture 三层架构

```
┌─────────────────────────────────────┐
│   Presentation Layer (composeApp)  │  ← UI、ViewModel、导航
│   • Screens (Composable)           │
│   • ViewModels (MVI)               │
└─────────────────────────────────────┘
                 ↓ 依赖
┌─────────────────────────────────────┐
│   Domain Layer (shared/domain)     │  ← 纯业务逻辑
│   • Models (领域模型)               │
│   • Repository Interfaces          │
│   • UseCases (业务用例)            │
└─────────────────────────────────────┘
                 ↑ 实现
┌─────────────────────────────────────┐
│   Data Layer (shared/data)         │  ← 数据访问
│   • Repository Implementations     │
│   • Remote: Pixiv Web API (Ktor)  │
│   • Local: Room Database           │
│   • Cache: File System (Okio)     │
└─────────────────────────────────────┘
```

### MVI (Model-View-Intent) 模式

```kotlin
// State - 屏幕状态
data class XxxScreenState(
    val data: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// Intent - 用户操作
sealed interface XxxIntent {
    data object LoadData : XxxIntent
    data class OnItemClick(val id: String) : XxxIntent
}

// ViewModel - 状态管理
class XxxViewModel(repo: Repository) : ViewModel() {
    val state: StateFlow<XxxScreenState>
    fun handleIntent(intent: XxxIntent)
}
```

> 📖 详细架构文档: [docs/project/项目架构参考文档.md](docs/project/项目架构参考文档.md)

## 🎬 核心功能

### Ugoira 动图支持

Pixiv 的 Ugoira (うごイラ) 是一种特殊的动图格式，本项目提供完整支持：

1. **获取元数据** - 从 API 获取 ZIP URL 和帧延迟时间
2. **下载和解压** - 下载 ZIP 文件并解压所有帧图片
3. **缓存管理** - 智能缓存，避免重复下载
4. **流畅播放** - 使用 `UgoiraPlayer` 组件按时序播放

### 多语言系统

使用 Compose Resources 官方多语言方案：

| 语言 | 代码 | 状态 |
|-----|------|------|
| 简体中文 | zh-rCN | ✅ |
| 繁体中文 | zh-rTW | ✅ |
| English | en | ✅ |
| 日本語 | ja | ✅ |
| 한국어 | ko | ✅ |

- **应用界面语言** 和 **Pixiv API 语言** 独立配置
- 支持运行时动态切换，无需重启应用
- 所有字符串统一管理，便于维护

## 📊 开发进度

> 完整开发状态: [docs/shared/DEVELOPMENT_STATUS.md](docs/shared/DEVELOPMENT_STATUS.md)

### 已完成 ✅

- ✅ 基础架构 (Clean Architecture + MVI)
- ✅ UI 框架 (Material 3 + 响应式布局)
- ✅ Pixiv Web API 完整集成
- ✅ 数据持久化 (Room + DataStore)
- ✅ 多语言系统 (5 种语言)
- ✅ Ugoira 动图播放器

### 开发中 🚧

- 🚧 登录认证系统 (高优先级)
- 🚧 作品列表页面 (高优先级)
- 🚧 作品详情页面 (高优先级)

### 计划中 📋

- 📋 搜索功能
- 📋 用户主页
- 📋 排行榜页面
- 📋 离线缓存优化

---

## 🌐 Pixiv API 集成

> API 详细状态: [docs/shared/API_STATUS.md](docs/shared/API_STATUS.md)

### 已集成 API ✅

| API 模块 | 功能 | 完成度 |
|---------|------|--------|
| **IllustApi** | 作品详情、搜索、推荐、发现、Ugoira | 100% |
| **UserApi** | 用户信息、关注、作品列表、收藏列表 | 100% |
| **BookmarkApi** | 添加/删除收藏、批量操作 | 100% |
| **RankingApi** | 各类排行榜 (日/周/月/新人/R18) | 100% |
| **CommentApi** | 评论功能 | 0% |
| **NovelApi** | 小说功能 | 0% |

### 使用方法

```kotlin
// 1. 配置 PHPSESSID
val pixivApi = PixivApi.create(
    httpClient = httpClient,
    phpSessionId = "从浏览器获取的 PHPSESSID"
)

// 2. 调用 API
val artwork = pixivApi.illustApi.getIllustDetail("123456")
val search = pixivApi.illustApi.searchArtworks("初音ミク")
val ranking = pixivApi.rankingApi.getRanking(mode = "daily")
```

> 📖 完整 API 文档: [docs/pixiv/PIXIV_API_集成指南.md](docs/pixiv/PIXIV_API_集成指南.md)

## 📚 项目文档

| 文档 | 说明 |
|-----|------|
| [项目架构参考文档](docs/project/项目架构参考文档.md) | 完整的架构设计和技术细节 ⭐⭐⭐ |
| [AI 助手协作提示词](docs/AI_ASSISTANT_PROMPT.md) | 用于新 AI 对话的完整上下文 ⭐⭐ |
| [Pixiv API 集成指南](docs/pixiv/PIXIV_API_集成指南.md) | API 使用文档和示例 ⭐⭐ |
| [开发进度](docs/shared/DEVELOPMENT_STATUS.md) | 功能完成状态和待办事项 |
| [技术栈](docs/shared/TECH_STACK.md) | 依赖版本和技术选型 |
| [API 状态](docs/shared/API_STATUS.md) | Pixiv API 集成状态 |
| [设置系统架构](docs/settings/设置系统架构.md) | 设置功能设计文档 |
| [自适应布局指南](docs/guides/自适应布局指南.md) | 响应式布局实现 |

---

## 🤝 参与贡献

欢迎提交 Issue 和 Pull Request！

### 开发流程

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 代码规范

- 遵循 Kotlin 官方代码风格
- 使用 MVI 架构模式
- 所有功能必须支持 Android 和 Desktop 双平台
- 添加新功能时更新相关文档

---

## 📄 许可证

本项目仅供学习和研究使用。请遵守 Pixiv 的使用条款。

---

## 🙏 致谢

- [Pixiv](https://www.pixiv.net/) - 提供优秀的创作平台
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) - 强大的跨平台技术
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) - 现代化的 UI 框架
- [Koin](https://insert-koin.io/) - 简洁的依赖注入框架

---

<div align="center">

**[⬆ 回到顶部](#projectu---pixiv-kotlin-multiplatform-client)**

Made with ❤️ using Kotlin Multiplatform

</div>

