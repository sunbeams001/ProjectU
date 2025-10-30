# 技术栈

> 📅 最后更新: 2025-10-30

## 核心版本

| 技术 | 版本 | 说明 |
|-----|------|------|
| **Kotlin** | 2.2.20 | 编程语言 |
| **Compose Multiplatform** | 1.9.1 | UI 框架 |
| **Compose Plugin** | 1.7.1 | Compose 编译器 |
| **Android Gradle Plugin** | 8.13.0 | Android 构建工具 |
| **KSP** | 2.2.20-2.0.4 | Kotlin 符号处理 |

## 平台要求

| 平台 | 最低版本 | 目标版本 |
|------|---------|---------|
| **Android** | API 24 (Android 7.0) | API 36 (Android 14) |
| **JVM** | Java 11 | Java 11 |
| **Desktop** | Windows 10+ / macOS 10.14+ / Linux | - |

## 核心依赖

### 架构与依赖注入
- **Koin 4.1.1** - 依赖注入框架
  - koin-core, koin-android, koin-compose, koin-compose-viewmodel

### 网络层
- **Ktor 3.3.1** - HTTP 客户端
  - ktor-client-core, ktor-client-cio
  - ktor-client-content-negotiation
  - ktor-serialization-kotlinx-json
  - ktor-client-logging, ktor-client-auth

### 数据持久化
- **Room 2.8.3** - SQLite 数据库 ORM
- **SQLite Bundled 2.6.1** - SQLite 驱动
- **DataStore 1.1.7** - 键值对存储

### UI 与导航
- **Voyager 1.1.0-beta03** - 导航框架
- **Coil 3.3.0** - 图片加载
- **Compose Resources** - 官方多语言方案
- **Material Design 3** - UI 组件库

### 工具库
- **kotlinx-coroutines 1.10.2** - 异步编程
- **kotlinx-serialization 1.9.0** - JSON 序列化
- **kotlinx-datetime 0.7.1** - 日期时间处理
- **Okio 3.16.2** - 文件 I/O 和 ZIP 处理
- **AndroidX Lifecycle 2.9.4** - ViewModel & Lifecycle

## 详细配置

所有依赖版本在 `gradle/libs.versions.toml` 中统一管理。
