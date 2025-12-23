# 技术栈

> 📅 最后更新: 2025-12-23

## 核心版本

| 技术 | 版本 | 说明 |
|-----|------|------|
| **Kotlin** | 2.2.21 | 编程语言 |
| **Compose Multiplatform** | 1.9.3 | UI 框架 |
| **Android Gradle Plugin** | 8.13.2 | Android 构建工具 |
| **KSP** | 2.2.21-2.0.4 | Kotlin 符号处理 |

## 平台要求

| 平台 | 最低版本 | 目标版本 | 编译版本 |
|------|---------|---------|---------|
| **Android** | API 24 (Android 7.0) | API 36 (Android 15) | API 36 |
| **JVM** | Java 11 | Java 11 | - |
| **Desktop** | Windows 10+ / macOS 10.14+ / Linux | - | - |

## 核心依赖

### 架构与依赖注入
- **Koin 4.1.1** - 依赖注入框架
  - koin-core, koin-android, koin-compose, koin-compose-viewmodel
  - 跨平台依赖注入，支持 ViewModel 和 Compose 集成

### 网络层
- **Ktor 3.3.3** - HTTP 客户端
  - ktor-client-core - 核心客户端
  - ktor-client-cio - CIO 引擎（Android & Desktop）
  - ktor-client-content-negotiation - 内容协商
  - ktor-serialization-kotlinx-json - JSON 序列化
  - ktor-client-logging - 请求日志
  - ktor-client-auth - 认证支持

### 数据持久化
- **Room 2.8.4** - SQLite 数据库 ORM
  - 跨平台数据库支持（Android & Desktop）
  - 使用 KSP 生成代码
- **SQLite Bundled 2.6.2** - SQLite 驱动
  - 提供跨平台 SQLite 支持
- **DataStore 1.2.0** - 键值对存储
  - datastore-preferences-core - Preferences 数据存储

### UI 与导航
- **Voyager 1.1.0-beta03** - 导航框架
  - voyager-navigator - 基础导航
  - voyager-tab-navigator - 底部导航栏
  - voyager-transitions - 页面转场动画
  - voyager-koin - Koin 集成
- **Coil 3.3.0** - 图片加载库
  - coil-compose - Compose 集成
  - coil-network-ktor - Ktor 网络支持
- **Compose Resources** - 官方多语言方案
  - 内置于 Compose Multiplatform
- **Material Design 3** - UI 组件库
  - compose.material3 - Material 3 组件
  - compose.materialIconsExtended - 扩展图标

### 工具库
- **kotlinx-coroutines 1.10.2** - 异步编程
  - kotlinx-coroutines-core - 核心库
  - kotlinx-coroutines-android - Android 支持
  - kotlinx-coroutines-swing - Desktop 支持
- **kotlinx-serialization 1.9.0** - JSON 序列化
  - kotlinx-serialization-json - JSON 支持
- **kotlinx-datetime 0.7.1** - 日期时间处理
  - 跨平台日期时间库
- **Okio 3.16.4** - 文件 I/O
  - 文件操作和 ZIP 处理
- **AndroidX Lifecycle 2.10.0** - 生命周期管理
  - androidx-lifecycle-viewmodel - ViewModel
  - androidx-lifecycle-runtime-compose - Compose 集成

### Android 特定依赖
- **AndroidX Activity 1.12.2** - Activity 支持
  - androidx-activity-compose - Activity Compose 集成
- **AndroidX Core Splashscreen 1.2.0** - 启动屏幕
  - 原生启动屏幕 API
- **AndroidX DocumentFile 1.1.0** - 文档文件
  - SAF (Storage Access Framework) 支持

### 多媒体处理
- **GIF.kt 0.3.0** - GIF 编解码库
  - 用于 Ugoira 动图的 GIF 格式支持
- **JavaCV 1.5.12** - FFmpeg 封装（仅 Desktop）
  - javacv-platform - Desktop 平台 MP4 编码

### WebView
- **Compose WebView Multiplatform 2.0.3** - 跨平台 WebView
  - 支持 Android 和 Desktop 平台

## Pixiv API 集成

本项目完整集成了 Pixiv Web API，包含以下 API 模块：

| API 模块 | 功能 | 状态 |
|---------|------|------|
| **IllustApi** | 插画详情、搜索、推荐、发现、Ugoira | ✅ 已实现 |
| **IllustSeriesApi** | 漫画系列管理、追更 | ✅ 已实现 |
| **UserApi** | 用户信息、关注、作品列表 | ✅ 已实现 |
| **BookmarkApi** | 收藏管理、批量操作 | ✅ 已实现 |
| **RankingApi** | 各类排行榜 | ✅ 已实现 |
| **CommentApi** | 评论管理 | ✅ 已实现 |
| **NovelApi** | 小说相关功能 | ✅ 已实现 |
| **NovelSeriesApi** | 小说系列管理 | ✅ 已实现 |
| **TagApi** | 标签相关功能 | ✅ 已实现 |
| **MarkerApi** | 书签管理（稍后再读） | ✅ 已实现 |
| **FollowApi** | 关注用户的作品、追更 | ✅ 已实现 |
| **SearchApi** | 搜索功能 | ✅ 已实现 |

详细 API 状态请查看：[docs/shared/API_STATUS.md](API_STATUS.md)

## 架构模式

### Clean Architecture
- **Domain Layer** (shared 模块) - 业务核心
  - 领域模型（Model）
  - Repository 接口
  - 业务用例（UseCase）
- **Data Layer** (shared 模块) - 数据访问
  - Repository 实现
  - Pixiv API 集成
  - Room 数据库
  - DataStore 配置存储
  - 文件缓存系统
- **Presentation Layer** (composeApp 模块) - UI 表现
  - MVI 架构的 Screen 和 ViewModel
  - Compose UI 组件
  - 导航管理
  - 多语言支持

### MVI 模式
- **Model** - 不可变状态（State）
- **View** - Compose UI，观察 State 并渲染
- **Intent** - 用户行为和系统事件

## 详细配置

所有依赖版本在 [gradle/libs.versions.toml](../../gradle/libs.versions.toml) 中统一管理。

## 构建工具

- **Gradle 8.x** - 构建系统
- **Gradle Version Catalog** - 依赖版本管理
- **Gradle Kotlin DSL** - Kotlin 构建脚本

## 相关文档

- [项目架构参考文档](../project/项目架构参考文档.md) - 完整架构设计
- [API 集成状态](API_STATUS.md) - Pixiv API 实现进度
- [README](../../README.md) - 项目概览
