# ProjectU - Pixiv Kotlin Multiplatform Client

> 📅 最后更新: 2025-12-23  
> 🚀 当前版本: v1.0.0  
> 📱 支持平台: Android (API 24+) | Desktop (Windows/Mac/Linux)

一个功能完整的 Pixiv 客户端应用，使用 Kotlin Compose Multiplatform 开发，提供现代化的界面体验和丰富的功能。

---

## 🎯 项目简介

ProjectU 是一个开源的跨平台 Pixiv 客户端，采用最新的跨平台技术栈构建，提供完整的作品浏览、搜索、下载等功能。

### ✨ 核心特性

- 🎨 **真正的跨平台** - 一套代码，同时支持 Android 和 Desktop（Windows/Mac/Linux）
- 🏛️ **Clean Architecture** - MVI 模式 + 三层架构设计，代码结构清晰
- 🎭 **Material Design 3** - 现代化 UI，支持深色/浅色主题切换
- 🌍 **完整多语言** - 支持简体中文/繁体中文/English/日本語/한국어
- 🌐 **Pixiv Web API** - 完整集成 Pixiv Web API，支持95%以上的功能
- 🎬 **Ugoira 支持** - 完整支持 Pixiv 动图（うごイラ）的播放，支持下载为 GIF 和 MP4 格式
- 📱 **响应式设计** - 智能适配手机/平板/桌面不同屏幕尺寸
- 💾 **智能缓存** - Room 数据库 + 文件缓存系统，流畅的离线体验
- 📥 **下载管理** - 支持自定义下载路径规则和文件命名规则
- 🔍 **图片搜索** - 支持 SauceNAO 和 Ascii2d 以图搜图（Android）

## 🛠️ 技术栈

> 完整技术栈请查看: [docs/shared/TECH_STACK.md](docs/shared/TECH_STACK.md)

### 核心技术

| 技术 | 版本 | 用途 |
|-----|------|------|
| Kotlin | 2.2.21 | 编程语言 |
| Compose Multiplatform | 1.9.2 | UI 框架 |
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
│   ├── settings/                # 设置系统文档
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
git clone https://github.com/sunbeams001/ProjectU.git
cd ProjectU
```

### 构建和运行

#### Android

```bash
# 方式 1: 使用脚本
./build-android.sh    # Linux/Mac
build-android.bat     # Windows

# 方式 2: 使用 Gradle
# Debug 版本（开发测试）
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug

# Release 版本（正式发布）
./gradlew :composeApp:assembleRelease

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

### 配置 Pixiv 登录

首次运行应用后，需要登录 Pixiv 账号：

**方式 1: 使用内置 Webview 登录（推荐）**

1. 进入应用的 **登录** 页面
2. 点击 **使用浏览器登录** 按钮
3. 在弹出的 Webview 中输入 Pixiv 账号和密码
4. 登录成功后自动获取凭据，无需手动操作

**方式 2: 手动配置 PHPSESSID**

如果 Webview 登录遇到问题，可以手动配置：

1. 在浏览器中登录 [Pixiv](https://www.pixiv.net/)
2. 打开开发者工具（F12） → Application/Storage → Cookies
3. 复制 `PHPSESSID` 的值
4. 在应用的设置页面中填入 PHPSESSID

配置完成后即可使用所有功能。

> 📖 详细配置说明: [docs/guides/API测试工具使用指南.md](docs/guides/API测试工具使用指南.md)

---

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

##  主要功能

### 作品浏览

- **作品详情** - 查看作品完整信息、标签、相关作品
- **图片查看器** - 支持缩放、全屏浏览
- **Ugoira 动图** - 流畅播放 Pixiv 动图，支持暂停、播放控制
- **推荐作品** - 基于个人喜好的智能推荐
- **发现** - 探索新作品、新小说、新创作者

### 搜索功能

- **关键词搜索** - 搜索插画、漫画、Ugoira、小说
- **高级筛选** - 支持多种排序方式，支持复杂筛选条件
- **标签搜索** - 快速按标签查找作品
- **以图搜图** - 使用 SauceNAO 和 Ascii2d 反向搜索图片来源（Android）

### 排行榜

- **插画排行榜** - 日榜、周榜、月榜、新人榜
- **小说排行榜** - 各类小说排行
- **R-18 排行榜** - 支持成人内容排行（需配置）
- **多维度筛选** - 按内容类型（插画/漫画/动图）筛选

### 用户系统

- **用户主页** - 查看用户信息、作品列表、收藏
- **关注系统** - 关注/取消关注创作者
- **用户关系** - 查看关注列表、粉丝列表
- **关注动态** - 查看关注用户的最新作品

### 收藏管理

- **作品收藏** - 收藏插画、漫画、Ugoira
- **小说收藏** - 收藏喜欢的小说
- **收藏标签** - 为收藏添加自定义标签
- **批量操作** - 批量管理收藏

### 系列作品

- **漫画系列** - 查看完整漫画系列，支持追更
- **小说系列** - 阅读连载小说，追踪更新

### 下载功能

- **下载路径规则** - 支持自定义下载路径，可按作品类型、作者等组织文件夹结构
- **文件命名规则** - 灵活配置文件命名规则（作品ID、标题、作者等变量）
- **下载队列** - 智能管理下载任务
- **权限管理** - Android 存储权限自动处理

### 评论互动

- **查看评论** - 浏览作品评论
- **评论管理** - 完整的评论功能支持

### 设置与配置

- **多语言切换** - 应用界面和 API 语言独立设置
- **主题切换** - 浅色/深色/跟随系统
- **Pixiv 配置** - PHPSESSID 登录凭据管理
- **文件命名规则** - 自定义下载文件命名格式

---

## 🌐 Pixiv API 集成

> API 详细状态: [docs/shared/API_STATUS.md](docs/shared/API_STATUS.md)

本项目完整集成了 Pixiv Web API，实现了 **64 个 API 方法**，测试覆盖率达 **100%**。

### 已集成 API 模块

| API 模块 | 功能 | 方法数 | 测试状态 |
|---------|------|--------|---------|
| **IllustApi** | 作品详情、推荐、发现、Ugoira | 7 | ✅ 100% |
| **IllustSeriesApi** | 漫画系列详情、追更 | 3 | ✅ 100% |
| **UserApi** | 用户信息、关注、作品列表、收藏、好P友 | 12 | ✅ 100% |
| **BookmarkApi** | 添加/删除收藏、批量操作、标签管理 | 10 | ✅ 100% |
| **RankingApi** | 插画排行榜、小说排行榜 | 2 | ✅ 100% |
| **CommentApi** | 查看/发表/删除评论（插画+小说） | 8 | ✅ 100% |
| **NovelApi** | 小说详情、收藏状态、发现 | 3 | ✅ 100% |
| **FollowApi** | 关注用户最新作品、追更列表 | 4 | ✅ 100% |
| **NovelSeriesApi** | 小说系列详情、内容列表、追更 | 5 | ✅ 100% |
| **TagApi** | 标签搜索、标签建议、标签信息 | 4 | ✅ 100% |
| **MarkerApi** | 小说阅读标记（稍后再读） | 3 | ✅ 100% |
| **SearchApi** | 搜索作品、小说、用户 | 3 | ✅ 100% |

**支持的排行榜类型**：
- 一般排行：日榜、周榜、月榜、新人榜、原创、AI 作品
- R-18 排行：R-18 日榜、周榜、AI 作品
- R-18G 排行（重口向）

**支持的内容类型**：插画、漫画、Ugoira 动图、小说

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

> 📖 完整 API 文档: [docs/guides/API测试工具使用指南.md](docs/guides/API测试工具使用指南.md)

## 📚 项目文档

完整的技术文档帮助您了解项目架构和实现细节：

| 文档 | 说明 |
|-----|------|
| [项目架构参考文档](docs/project/项目架构参考文档.md) | 完整的架构设计和技术细节 |
| [技术栈](docs/shared/TECH_STACK.md) | 依赖版本和技术选型 |
| [API 测试工具使用指南](docs/guides/API测试工具使用指南.md) | API 使用文档和代码集成指南 |
| [API 状态](docs/shared/API_STATUS.md) | Pixiv API 集成状态 |
| [设置系统架构](docs/settings/设置系统架构.md) | 设置功能设计文档 |
| [自适应布局指南](docs/guides/自适应布局指南.md) | 响应式布局实现 |
| [下载系统设计文档](docs/guides/下载系统完整设计文档.md) | 下载功能架构说明 |

---

## 📸 截图

_敬请期待，我们将在后续版本中添加应用截图_

---

## 🤝 参与贡献

欢迎为项目贡献代码、报告问题或提出建议！

### 贡献方式

1. **报告问题** - 在 [Issues](https://github.com/sunbeams001/ProjectU/issues) 中报告 Bug 或提出功能建议
2. **提交代码** - Fork 项目并提交 Pull Request
3. **完善文档** - 帮助改进文档和使用指南
4. **分享反馈** - 分享您的使用体验和建议

### 提交 Pull Request

1. Fork 本仓库到您的账号
2. 创建特性分支：`git checkout -b feature/AmazingFeature`
3. 提交更改：`git commit -m 'Add some AmazingFeature'`
4. 推送到分支：`git push origin feature/AmazingFeature`
5. 开启 Pull Request，描述您的更改

### 代码规范

- 遵循 Kotlin 官方代码风格指南
- 使用 MVI 架构模式组织代码
- 确保新功能同时支持 Android 和 Desktop 平台
- 添加必要的注释和文档
- 提交前运行测试确保代码质量

---

## ⚠️ 免责声明

本项目仅供学习和研究使用，请遵守 Pixiv 的服务条款和使用规则。作者不对使用本软件产生的任何问题负责。

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

