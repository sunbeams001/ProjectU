# Desktop 桌面端构建指南

## 📝 概述

ProjectU 支持跨平台桌面构建（Windows/Linux/macOS），本文档说明如何针对特定平台进行构建，以及如何优化构建产物的大小。

## 🎯 构建任务说明

### 基础构建任务

| 任务 | 说明 | 产物大小 | 使用场景 |
|-----|------|---------|---------|
| `createDistributable` | 创建当前 OS 的可分发包 | **较大**（包含所有平台依赖） | 开发测试 |
| `packageDistributionForCurrentOS` | 仅打包当前操作系统 | **中等** | 日常开发 |
| `packageMsi` / `packageDmg` / `packageDeb` | 生成特定平台安装包 | **最小** | 正式发布 |

### 📦 Windows 构建任务

```bash
# 1. 创建可分发包（包含所有平台资源，体积大）
gradlew :composeApp:createDistributable

# 2. 仅打包 Windows 版本（推荐）
gradlew :composeApp:packageDistributionForCurrentOS

# 3. 生成 Windows MSI 安装包（正式发布用）
gradlew :composeApp:packageMsi

# 4. 生成 Windows EXE 可执行文件
gradlew :composeApp:createReleaseDistributable
```

### 🐧 Linux 构建任务

```bash
# 打包 Linux 版本
./gradlew :composeApp:packageDistributionForCurrentOS

# 生成 DEB 安装包
./gradlew :composeApp:packageDeb

# 生成 RPM 安装包
./gradlew :composeApp:packageRpm
```

### 🍎 macOS 构建任务

```bash
# 打包 macOS 版本
./gradlew :composeApp:packageDistributionForCurrentOS

# 生成 DMG 安装包
./gradlew :composeApp:packageDmg
```

## 🔧 优化构建大小

### 问题原因

**问题1：多平台依赖**

默认使用 `compose.desktop.currentOs` 时，Compose Desktop 会在**编译时**包含所有平台的本地库：

```kotlin
desktopMain.dependencies {
    implementation(compose.desktop.currentOs)  // 包含 Windows/Linux/macOS 所有架构
}
```

这导致打包后的文件包含：
- `skiko-awt-runtime-all.jar`（包含所有平台）
  - `windows-x64` / `windows-arm64`
  - `linux-x64` / `linux-arm64`
  - `macos-x64` / `macos-arm64`

**问题2：JRE 运行时体积**

Desktop 应用打包时会包含完整的 Java 运行时环境（JRE），这通常占据 **400-600 MB** 的空间。这是正常的，因为需要确保应用在没有安装 Java 的系统上也能运行。

### 解决方案

#### ✅ 已实现：平台特定依赖（推荐）

项目已配置为根据当前操作系统自动选择对应的平台依赖：

```kotlin
desktopMain.dependencies {
    // 根据操作系统自动选择
    val osName = System.getProperty("os.name").lowercase()
    when {
        osName.contains("win") -> implementation(compose.desktop.windows_x64)
        osName.contains("mac") -> implementation(compose.desktop.macos_arm64) // 或 macos_x64
        osName.contains("linux") -> implementation(compose.desktop.linux_x64)
    }
}
```

**效果**：
- ✅ 只包含当前平台的 skiko 本地库（~0.5 MB）
- ✅ 避免打包所有平台的资源
- ❌ JRE 运行时仍然较大（~400-600 MB，这是正常的）

#### 📊 构建产物大小对比

| 构建方式 | 应用代码 | skiko 库 | JRE 运行时 | 总大小 | 说明 |
|---------|---------|---------|-----------|-------|------|
| **旧方式** (currentOs) | ~50 MB | **~100 MB** (所有平台) | ~600 MB | **~750 MB** | ❌ 包含多余平台资源 |
| **新方式** (平台特定) | ~50 MB | **~0.5 MB** (单平台) | ~600 MB | **~650 MB** | ✅ 只包含需要的平台 |

**优化效果**：减少约 100 MB（移除了其他平台的 skiko 资源）

#### 进一步优化（可选）

如果你希望进一步减小 JRE 体积，可以使用 `jlink` 创建自定义 JRE：

```kotlin
compose.desktop {
    application {
        nativeDistributions {
            // 创建自定义 JRE，只包含需要的模块
            modules(
                "java.base",
                "java.desktop",
                "java.logging",
                "java.naming",
                "java.sql",
                "jdk.unsupported"
            )
        }
    }
}
```

**注意**：使用自定义 JRE 可能导致某些功能不可用，需要仔细测试。

## 📋 快捷脚本

项目提供了以下构建脚本：

### Windows 批处理脚本

| 脚本 | 说明 |
|-----|------|
| `build-desktop.bat` | 通用构建（包含所有平台） |
| `build-desktop-windows.bat` | 仅构建 Windows 版本 |
| `build-desktop-release.bat` | 生成 Windows MSI 安装包 |

### Linux/macOS Shell 脚本

| 脚本 | 说明 |
|-----|------|
| `build-desktop.sh` | 通用构建 |

## 📊 构建产物对比

| 构建方式 | 产物大小 | 包含内容 | 适用场景 |
|---------|---------|---------|---------|
| `packageDistributionForCurrentOS` | ~650 MB | 当前平台 + JRE | 日常使用 ⭐ |
| `packageMsi/Dmg/Deb` | ~650 MB | 单平台安装包 + JRE | 正式发布 ⭐⭐⭐ |
| `createDistributable`（旧配置） | ~750 MB | 所有平台 + JRE | ❌ 不推荐 |

**注意**：大部分空间（~600MB）被 JRE 运行时占用，这是正常的，因为需要让应用在没有 Java 的系统上运行。

## 🎯 推荐使用

### 开发阶段
```bash
# Windows
build-desktop-windows.bat

# Linux/macOS
./build-desktop.sh
```

### 发布阶段
```bash
# Windows - 生成 MSI 安装包
build-desktop-release.bat

# Linux - 生成 DEB 包
./gradlew :composeApp:packageDeb

# macOS - 生成 DMG 镜像
./gradlew :composeApp:packageDmg
```

## 🔍 验证构建产物

构建完成后，检查产物位置：

```bash
# 可分发包
composeApp/build/compose/binaries/main/app/

# Windows MSI 安装包
composeApp/build/compose/binaries/main/msi/

# macOS DMG 镜像
composeApp/build/compose/binaries/main/dmg/

# Linux DEB 包
composeApp/build/compose/binaries/main/deb/
```

## 📝 注意事项

1. **依赖大小**：`compose.desktop.currentOs` 是为了支持跨平台开发的便利性，适合开发阶段
2. **发布优化**：正式发布时使用 `packageMsi`/`packageDmg`/`packageDeb` 生成单一平台安装包
3. **CI/CD**：在 CI/CD 流程中，建议为每个平台单独构建，使用 `-Dcompose.desktop.targetPlatform` 参数
4. **签名**：Windows MSI 和 macOS DMG 在发布前需要代码签名

## 🚀 快速开始

```bash
# 1. 清理旧构建
gradlew clean

# 2. 构建当前平台（推荐）
# Windows
build-desktop-windows.bat

# 3. 运行测试
gradlew :composeApp:run

# 4. 生成发布包（可选）
# Windows
build-desktop-release.bat
```

---

> 📅 最后更新: 2025-12-26  
> 🔗 相关文档: [README.md](../README.md) | [TECH_STACK.md](shared/TECH_STACK.md)
