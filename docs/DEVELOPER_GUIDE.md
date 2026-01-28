# ProjectU 开发者指南

> 本文档面向技术人员，提供项目的技术架构、开发环境配置和代码实现细节。

---

## 目录

- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [架构设计](#架构设计)
- [环境配置](#环境配置)
- [构建和运行](#构建和运行)
- [API 集成](#api-集成)
- [参考文档](#参考文档)

---

## 🛠️ 技术栈

> 完整技术栈请查看: [docs/shared/TECH_STACK.md](shared/TECH_STACK.md)

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

---

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

> 📖 详细架构说明: [project/项目架构参考文档.md](project/项目架构参考文档.md)

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

> 📖 详细架构文档: [project/项目架构参考文档.md](project/项目架构参考文档.md)

---

## 🚀 环境配置

### 环境要求

| 工具 | 版本要求 | 说明 |
|-----|---------|------|
| JDK | 17+ | Gradle 9.x 要求 JDK 17 或更高版本 |
| Android SDK | API 24+ | 仅 Android 构建需要 |
| Android Studio | Ladybug 2024.2.1+ | 推荐 IDE |
| Gradle | 9.2.1 | 自动下载，无需手动安装 |

### 配置开发环境

1. **配置 JDK**（推荐使用 JDK 17 或 21）

   创建或编辑 `local.properties` 文件，添加 JDK 路径：
   
   ```properties
   # Android SDK 路径
   sdk.dir=D\:\\Dev\\Android-Sdk
   
   # JDK 路径（推荐方式）
   org.gradle.java.home=C\:\\Program Files\\Eclipse Adoptium\\jdk-21.0.3.9-hotspot
   ```
   
   > 💡 **提示**：
   > - `local.properties` 文件不会被提交到 Git，适合配置本地环境
   > - 路径中的反斜杠需要转义（使用 `\\`）
   > - 也可以设置系统环境变量 `JAVA_HOME`，gradlew 会自动使用

2. **配置签名密钥**（仅 Android Release 构建需要）

   创建 `keystore.properties` 文件：
   
   ```properties
   storeFile=path/to/your/keystore.jks
   storePassword=your-store-password
   keyAlias=your-key-alias
   keyPassword=your-key-password
   ```

### 克隆项目

```bash
git clone https://github.com/sunbeams001/ProjectU.git
cd ProjectU
```

---

## 🔨 构建和运行

### Android

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

### Desktop

```bash
# 方式 1: 使用脚本（推荐）

# Windows - 仅构建 Windows 版本（体积小）
build-desktop-windows.bat

# Windows - 生成 MSI 安装包
build-desktop-release.bat

# Windows - 通用构建（包含所有平台，体积大）
build-desktop.bat

# Linux/Mac
./build-desktop.sh

# 方式 2: 使用 Gradle

# 运行桌面端应用
./gradlew :composeApp:run

# 仅打包当前操作系统（推荐，体积小）
./gradlew :composeApp:packageDistributionForCurrentOS

# 打包为可分发应用（包含所有平台，体积大）
./gradlew :composeApp:createDistributable

# 生成 Windows MSI 安装包
./gradlew :composeApp:packageMsi

# 生成 macOS DMG 镜像
./gradlew :composeApp:packageDmg

# 生成 Linux DEB 包
./gradlew :composeApp:packageDeb
```

> 💡 **提示**：
> - 开发测试推荐使用 `build-desktop-windows.bat` 或 `packageDistributionForCurrentOS`（体积小，仅包含当前平台）
> - 正式发布推荐使用 `build-desktop-release.bat` 或 `packageMsi/Dmg/Deb`（生成安装包）
> - 详细构建说明: [guides/Desktop构建指南.md](guides/Desktop构建指南.md)

---

## 🌐 Pixiv API 集成

> API 详细状态: [shared/API_STATUS.md](shared/API_STATUS.md)

本项目完整集成了 Pixiv Web API，实现了 **78 个 API 方法**，测试覆盖率达 **100%**。

### 已集成 API 模块

| API 模块 | 功能 | 方法数 | 测试状态 |
|---------|------|--------|---------|
| **IllustApi** | 作品详情、推荐、发现、Ugoira、点赞 | 8 | ✅ 100% |
| **IllustSeriesApi** | 漫画系列详情、追更 | 3 | ✅ 100% |
| **UserApi** | 用户信息、关注、作品列表、收藏、好P友、标签筛选 | 16 | ✅ 100% |
| **BookmarkApi** | 添加/删除收藏、批量操作、标签管理 | 10 | ✅ 100% |
| **RankingApi** | 插画排行榜、小说排行榜 | 2 | ✅ 100% |
| **CommentApi** | 查看/发表/删除评论（插画+小说） | 8 | ✅ 100% |
| **NovelApi** | 小说详情、收藏状态、发现、推荐 | 5 | ✅ 100% |
| **FollowApi** | 关注用户最新作品、追更列表 | 4 | ✅ 100% |
| **NovelSeriesApi** | 小说系列详情、内容列表、追更 | 5 | ✅ 100% |
| **TagApi** | 标签搜索、标签建议、标签信息、标签编辑 | 8 | ✅ 100% |
| **MarkerApi** | 小说阅读标记（稍后再读） | 4 | ✅ 100% |
| **SearchApi** | 搜索作品、小说、用户 | 3 | ✅ 100% |
| **PixivisionApi** | Pixivision特辑文章列表、详情 | 2 | ✅ 100% |

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

> 📖 完整 API 文档: [guides/API测试工具使用指南.md](guides/API测试工具使用指南.md)

---

## 📚 参考文档

完整的技术文档帮助您深入了解项目实现细节：

| 文档 | 说明 |
|-----|------|
| [项目架构参考文档](project/项目架构参考文档.md) | 完整的架构设计和技术细节 |
| [技术栈](shared/TECH_STACK.md) | 依赖版本和技术选型 |
| [API 测试工具使用指南](guides/API测试工具使用指南.md) | API 使用文档和代码集成指南 |
| [API 状态](shared/API_STATUS.md) | Pixiv API 集成状态 |
| [设置系统架构](settings/设置系统架构.md) | 设置功能设计文档 |
| [自适应布局指南](guides/自适应布局指南.md) | 响应式布局实现 |
| [下载系统设计文档](guides/下载系统完整设计文档.md) | 下载功能架构说明 |
| [Desktop构建指南](guides/Desktop构建指南.md) | Desktop平台构建详解 |

---

## 🤝 贡献指南

### 代码规范

- 遵循 Kotlin 官方代码风格指南
- 使用 MVI 架构模式组织代码
- 确保新功能同时支持 Android 和 Desktop 平台
- 添加必要的注释和文档
- 提交前运行测试确保代码质量

### 提交 Pull Request

1. Fork 本仓库到您的账号
2. 创建特性分支：`git checkout -b feature/AmazingFeature`
3. 提交更改：`git commit -m 'Add some AmazingFeature'`
4. 推送到分支：`git push origin feature/AmazingFeature`
5. 开启 Pull Request，描述您的更改

---

<div align="center">

Made with ❤️ using Kotlin Multiplatform

</div>
