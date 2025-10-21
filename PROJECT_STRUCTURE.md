# ProjectU - 项目结构说明

## 概览

本文档详细说明了 ProjectU 的项目结构和各个模块的职责。

## 模块划分

### 1. composeApp 模块

**职责**: UI 层，包含所有 Compose 相关代码

```
composeApp/
├── src/
│   ├── commonMain/              # 共享 UI 代码
│   │   ├── kotlin/com/projectu/
│   │   │   ├── App.kt           # 应用入口
│   │   │   ├── ui/
│   │   │   │   ├── screens/     # 页面
│   │   │   │   │   ├── home/
│   │   │   │   │   │   ├── HomeScreen.kt     # 主页面（包含底部导航）
│   │   │   │   │   │   └── tabs/             # 各个Tab页面
│   │   │   │   │   ├── discovery/           # 发现页面
│   │   │   │   │   ├── ranking/             # 排行榜页面
│   │   │   │   │   ├── user/                # 用户页面
│   │   │   │   │   ├── artwork/             # 作品详情页面
│   │   │   │   │   └── login/               # 登录页面
│   │   │   │   ├── components/  # 可复用组件
│   │   │   │   │   └── UgoiraPlayer.kt      # Ugoira 播放器
│   │   │   │   ├── navigation/  # 导航定义
│   │   │   │   └── theme/       # 主题配置
│   │   │   │       ├── Theme.kt
│   │   │   │       └── Type.kt
│   │   │   └── di/              # Koin 模块定义
│   │   │       └── AppModule.kt
│   │   └── resources/MR/        # 多语言资源
│   │       ├── base/            # 英文（默认）
│   │       ├── zh-rCN/          # 简体中文
│   │       ├── zh-rTW/          # 繁体中文
│   │       ├── ja/              # 日文
│   │       └── ko/              # 韩文
│   ├── androidMain/             # Android 特定
│   │   ├── kotlin/
│   │   │   └── com/projectu/
│   │   │       ├── MainActivity.kt
│   │   │       ├── PixivApplication.kt
│   │   │       └── di/
│   │   │           └── PlatformModule.android.kt
│   │   └── AndroidManifest.xml
│   └── desktopMain/             # Desktop 特定
│       └── kotlin/
│           └── com/projectu/
│               ├── main.kt
│               └── di/
│                   └── PlatformModule.desktop.kt
└── build.gradle.kts
```

### 2. shared 模块

**职责**: 业务逻辑层，包含领域模型、数据访问、业务用例

```
shared/
├── src/
│   ├── commonMain/
│   │   └── kotlin/com/projectu/shared/
│   │       ├── domain/              # 领域层（业务核心）
│   │       │   ├── model/           # 领域模型
│   │       │   │   └── Artwork.kt   # 作品模型（含 Ugoira）
│   │       │   ├── repository/      # Repository 接口定义
│   │       │   │   ├── ArtworkRepository.kt
│   │       │   │   └── UserRepository.kt
│   │       │   └── usecase/         # 业务用例
│   │       │       └── GetUgoiraUseCase.kt
│   │       ├── data/                # 数据层
│   │       │   ├── repository/      # Repository 实现
│   │       │   │   └── .gitkeep
│   │       │   ├── remote/          # 网络数据源
│   │       │   │   ├── api/         # API 接口定义
│   │       │   │   │   └── .gitkeep
│   │       │   │   ├── dto/         # 数据传输对象
│   │       │   │   │   └── ArtworkDto.kt
│   │       │   │   └── mapper/      # DTO → Domain 映射
│   │       │   │       └── ArtworkMapper.kt
│   │       │   ├── local/           # 本地数据源
│   │       │   │   ├── dao/         # Room DAO
│   │       │   │   │   └── .gitkeep
│   │       │   │   └── entity/      # 数据库实体
│   │       │   │       └── ArtworkEntity.kt
│   │       │   └── cache/           # 缓存管理
│   │       │       └── UgoiraCache.kt    # Ugoira 缓存
│   │       ├── util/                # 工具类
│   │       │   └── NetworkClient.kt      # Ktor 客户端配置
│   │       └── di/                  # 依赖注入
│   │           └── SharedModule.kt
│   ├── androidMain/
│   │   └── kotlin/com/projectu/shared/
│   │       └── data/cache/
│   │           └── UgoiraCache.android.kt    # Android ZIP 解压
│   └── desktopMain/
│       └── kotlin/com/projectu/shared/
│           └── data/cache/
│               └── UgoiraCache.desktop.kt    # Desktop ZIP 解压
└── build.gradle.kts
```

## 架构分层

### Clean Architecture 分层

```
┌─────────────────────────────────────────────┐
│           Presentation Layer                │
│        (composeApp 模块)                     │
│  ┌─────────────────────────────────────┐   │
│  │  UI (Compose)                       │   │
│  │  - Screens                          │   │
│  │  - Components                       │   │
│  │  - ViewModels                       │   │
│  └─────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────────┐
│            Domain Layer                     │
│         (shared/domain/)                    │
│  ┌─────────────────────────────────────┐   │
│  │  Business Logic                     │   │
│  │  - Models                           │   │
│  │  - UseCases                         │   │
│  │  - Repository Interfaces            │   │
│  └─────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────────┐
│            Data Layer                       │
│          (shared/data/)                     │
│  ┌─────────────────────────────────────┐   │
│  │  Data Sources                       │   │
│  │  - Repository Implementations       │   │
│  │  - Remote API (Ktor)                │   │
│  │  - Local DB (Room)                  │   │
│  │  - Cache (Okio)                     │   │
│  └─────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
```

### MVI 数据流

```
┌─────────┐
│   UI    │
└─────────┘
     │ User Action
     ↓
┌─────────┐
│ Intent  │  (用户意图)
└─────────┘
     │
     ↓
┌─────────┐
│ViewModel│  (处理 Intent)
└─────────┘
     │
     ↓
┌─────────┐
│ UseCase │  (业务逻辑)
└─────────┘
     │
     ↓
┌──────────────┐
│ Repository   │  (数据访问)
└──────────────┘
     │
     ↓
┌──────────────┐
│    State     │  (新的状态)
└──────────────┘
     │
     ↓
┌─────────┐
│   UI    │  (重新渲染)
└─────────┘
```

## 依赖关系

### 模块依赖

```
composeApp → shared
```

- composeApp 依赖 shared
- shared 不依赖 composeApp（保持独立性）

### 层级依赖

```
Presentation → Domain → Data
```

- Presentation 层可以访问 Domain 层
- Domain 层定义接口，Data 层实现
- Data 层不能直接被 Presentation 访问（通过 Domain 接口）

## 关键技术实现

### 1. 依赖注入（Koin）

**平台特定实现**:
- `composeApp/src/androidMain/.../PlatformModule.android.kt`
- `composeApp/src/desktopMain/.../PlatformModule.desktop.kt`

**共享模块**:
- `composeApp/src/commonMain/.../di/AppModule.kt`
- `shared/src/commonMain/.../di/SharedModule.kt`

### 2. 网络层（Ktor）

**配置**: `shared/.../util/NetworkClient.kt`
- ContentNegotiation (JSON)
- Logging
- Authentication (Bearer Token)
- Timeout 配置

### 3. 数据库（Room）

**实体**: `shared/.../data/local/entity/`
- ArtworkEntity - 作品缓存
- UgoiraCacheEntity - 动图缓存

### 4. Ugoira 动图处理

**流程**:
1. API 获取元数据 → `GetUgoiraUseCase`
2. 下载 ZIP → `UgoiraCache.saveZipFile()`
3. 解压帧图片 → `extractZipPlatform()` (平台特定)
4. 播放动画 → `UgoiraPlayer` 组件

**关键文件**:
- `shared/.../data/cache/UgoiraCache.kt` - 缓存管理
- `shared/.../data/cache/UgoiraCache.android.kt` - Android 解压
- `shared/.../data/cache/UgoiraCache.desktop.kt` - Desktop 解压
- `composeApp/.../ui/components/UgoiraPlayer.kt` - 播放器

### 5. 多语言（Moko Resources）

**资源文件**: `composeApp/src/commonMain/resources/MR/`

**支持语言**:
- base/ - English (默认)
- zh-rCN/ - 简体中文
- zh-rTW/ - 繁体中文
- ja/ - 日本語
- ko/ - 한국어

## 开发指南

### 添加新页面

1. 在 `composeApp/src/commonMain/kotlin/com/projectu/ui/screens/` 创建页面目录
2. 创建 Screen 类（继承 `cafe.adriel.voyager.core.screen.Screen`）
3. 定义 State、Intent、ViewModel（MVI 模式）
4. 在导航中注册

### 添加新 API

1. 在 `shared/.../data/remote/dto/` 定义 DTO
2. 在 `shared/.../data/remote/mapper/` 创建映射器
3. 在 `shared/.../data/remote/api/` 定义 API 接口
4. 在 `shared/.../domain/model/` 定义领域模型
5. 在 `shared/.../domain/repository/` 定义 Repository 接口
6. 在 `shared/.../data/repository/` 实现 Repository

### 添加新的多语言字符串

1. 在所有语言的 `strings.xml` 中添加相同的 key
2. 使用 `MR.strings.your_key` 访问

## 配置文件

### Gradle 配置

- `build.gradle.kts` - 根级构建配置
- `settings.gradle.kts` - 项目设置
- `gradle.properties` - Gradle 属性
- `gradle/libs.versions.toml` - 依赖版本管理（推荐）

### Android 配置

- `composeApp/src/androidMain/AndroidManifest.xml`
- minSdk: 24
- targetSdk: 35
- compileSdk: 35

### Desktop 配置

- 在 `composeApp/build.gradle.kts` 的 `compose.desktop` 块配置
- 支持 DMG (macOS)、MSI (Windows)、DEB (Linux)

## 下一步开发

待实现的功能（按优先级）:

1. **Pixiv Web API 集成** - 集成用户提供的开源 API 实现
2. **登录认证** - 实现完整的登录流程和 Token 管理
3. **作品列表** - 实现作品浏览、瀑布流布局
4. **搜索功能** - 作品搜索、用户搜索、标签搜索
5. **作品详情** - 完整的作品详情页面
6. **用户资料** - 用户主页、关注列表
7. **排行榜** - 各类排行榜展示
8. **离线缓存** - 优化缓存策略和管理

## 注意事项

1. **平台差异**: 使用 `expect`/`actual` 关键字处理平台特定实现
2. **协程作用域**: UI 层使用 `viewModelScope`，数据层使用 `Dispatchers.IO`
3. **错误处理**: 使用 `Result<T>` 封装可能失败的操作
4. **资源释放**: 注意 Bitmap、流等资源的及时释放
5. **缓存管理**: 定期清理过期缓存，避免存储空间占用过大

