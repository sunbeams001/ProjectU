# ProjectU - 快速开始指南

## 项目已完成的内容

### ✅ 已完成

1. **项目框架搭建**
   - Kotlin Multiplatform 项目结构
   - Gradle 配置文件（支持 Android + Desktop）
   - 依赖版本管理（libs.versions.toml）

2. **架构设计**
   - Clean Architecture 分层（Domain、Data、Presentation）
   - MVI 架构模式基础代码
   - Koin 依赖注入框架配置（含平台特定实现）

3. **UI 层**
   - Material Design 3 主题（浅色/深色模式）
   - Pixiv 风格配色方案
   - 基础主屏幕和底部导航
   - Voyager 导航框架集成
   - UgoiraPlayer 动图播放器组件

4. **数据层**
   - 领域模型定义（Artwork、User、UgoiraMetadata 等）
   - Repository 接口定义
   - DTO 和 Mapper
   - Room 数据库实体
   - Ktor 网络客户端配置
   - Ugoira 缓存管理（支持 ZIP 解压）

5. **多语言支持**
   - Moko Resources 集成
   - 5 种语言资源文件（英文、简中、繁中、日文、韩文）
   - 完整的字符串资源定义

6. **平台特定实现**
   - Android MainActivity 和 Application
   - Desktop main 函数
   - 平台特定的 Koin 模块
   - 平台特定的 ZIP 解压实现

7. **文档**
   - README.md - 项目介绍
   - PROJECT_STRUCTURE.md - 项目结构详解
   - GETTING_STARTED.md - 快速开始指南
   - .gitignore - Git 忽略规则

### 🚧 待实现

1. **Pixiv API 集成**
   - 集成你提供的 Pixiv 网页版 API 开源实现
   - 实现具体的 API 调用逻辑

2. **登录认证**
   - 登录界面
   - Token 管理
   - 会话保持

3. **核心功能**
   - 作品浏览（瀑布流布局）
   - 搜索功能
   - 排行榜
   - 用户资料
   - 作品详情页

4. **数据持久化完善**
   - Room DAO 实现
   - DataStore 配置
   - 缓存策略优化

5. **ViewModel 实现**
   - 各个 Screen 的 ViewModel
   - State 和 Intent 完整实现

## 快速开始

### 环境准备

1. **安装必要软件**
   ```bash
   # JDK 11+
   java -version
   
   # Android Studio Ladybug | 2024.2.1+
   # 下载地址: https://developer.android.com/studio
   ```

2. **克隆项目**（如果从 Git）
   ```bash
   git clone <your-repo-url>
   cd ProjectU
   ```

3. **同步 Gradle**
   ```bash
   # Windows
   .\gradlew.bat build
   
   # macOS/Linux
   ./gradlew build
   ```

### 运行 Android 版本

1. 打开 Android Studio
2. 选择 "Open" 并选择项目根目录
3. 等待 Gradle 同步完成
4. 连接 Android 设备或启动模拟器
5. 点击 "Run" 按钮或按 `Shift + F10`

或使用命令行：
```bash
./gradlew :composeApp:installDebug
```

### 运行 Desktop 版本

1. 在 Android Studio 中：
   - 点击顶部的运行配置下拉菜单
   - 选择 "composeApp [desktop]"
   - 点击 "Run" 按钮

或使用命令行：
```bash
./gradlew :composeApp:run
```

### 打包发布

#### Android APK
```bash
# Debug 版本
./gradlew :composeApp:assembleDebug

# Release 版本（需要配置签名）
./gradlew :composeApp:assembleRelease
```

输出路径: `composeApp/build/outputs/apk/`

#### Desktop 可执行文件
```bash
# 创建分发包
./gradlew :composeApp:createDistributable

# 打包成安装程序
./gradlew :composeApp:packageDistributionForCurrentOS
```

输出路径: `composeApp/build/compose/binaries/main/`

## 下一步开发建议

### 1. 集成 Pixiv API

在 `shared/src/commonMain/kotlin/com/projectu/shared/data/remote/api/` 目录下：

```kotlin
// PixivApi.kt
interface PixivApi {
    suspend fun getRecommendedArtworks(): List<ArtworkDto>
    suspend fun getArtworkDetail(id: String): ArtworkDto
    suspend fun searchArtworks(keyword: String): List<ArtworkDto>
    suspend fun login(username: String, password: String): AuthDto
    // ... 其他 API
}
```

### 2. 实现 Repository

在 `shared/src/commonMain/kotlin/com/projectu/shared/data/repository/` 目录下：

```kotlin
// ArtworkRepositoryImpl.kt
class ArtworkRepositoryImpl(
    private val pixivApi: PixivApi,
    private val artworkDao: ArtworkDao,
    private val ugoiraCache: UgoiraCache
) : ArtworkRepository {
    override fun getRecommendedArtworks(): Flow<Result<List<Artwork>>> = flow {
        try {
            // 1. 尝试从本地缓存加载
            val cached = artworkDao.getRecommendedArtworks()
            if (cached.isNotEmpty()) {
                emit(Result.success(cached.map { it.toDomain() }))
            }
            
            // 2. 从网络加载最新数据
            val artworks = pixivApi.getRecommendedArtworks()
            emit(Result.success(artworks.map { it.toDomain() }))
            
            // 3. 更新缓存
            artworkDao.insertAll(artworks.map { it.toEntity() })
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    // ... 其他方法实现
}
```

### 3. 创建 ViewModel

在 `composeApp/src/commonMain/kotlin/com/projectu/ui/screens/home/` 目录下：

```kotlin
// HomeViewModel.kt
class HomeViewModel(
    private val artworkRepository: ArtworkRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(HomeScreenState())
    val state = _state.asStateFlow()
    
    fun handleIntent(intent: HomeScreenIntent) {
        when (intent) {
            is HomeScreenIntent.LoadArtworks -> loadArtworks()
            is HomeScreenIntent.OnArtworkClick -> navigateToDetail(intent.id)
            // ... 处理其他 Intent
        }
    }
    
    private fun loadArtworks() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            artworkRepository.getRecommendedArtworks()
                .collect { result ->
                    result.onSuccess { artworks ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                artworks = artworks,
                                error = null
                            )
                        }
                    }.onFailure { error ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = error.message
                            )
                        }
                    }
                }
        }
    }
}
```

### 4. 更新 Koin 模块

在 DI 模块中注册所有依赖：

```kotlin
// shared/src/commonMain/kotlin/.../di/SharedModule.kt
val dataModule = module {
    single<ArtworkRepository> { ArtworkRepositoryImpl(get(), get(), get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
    // ...
}

val useCaseModule = module {
    factory { GetRecommendedArtworksUseCase(get()) }
    factory { GetUgoiraUseCase(get()) }
    // ...
}

// composeApp/.../di/PlatformModule.kt
val viewModelModule = module {
    viewModel { HomeViewModel(get()) }
    viewModel { ArtworkDetailViewModel(get()) }
    // ...
}
```

## 项目配置说明

### Gradle 版本

- Kotlin: 2.2.20
- Compose: 1.9.1
- AGP (Android Gradle Plugin): 8.13.0
- KSP: 2.2.20-2.0.4

### 最低版本要求

- Android: API 24 (Android 7.0)
- JVM: Java 11

### 关键依赖

| 库 | 版本 | 用途 |
|---|---|---|
| Koin | 4.1.1 | 依赖注入 |
| Ktor | 3.3.1 | 网络请求 |
| Room | 2.8.2 | 本地数据库 |
| Voyager | 1.1.0-beta03 | 导航 |
| Coil | 3.3.0 | 图片加载 |
| Moko Resources | 0.25.1 | 多语言 |
| Okio | 3.16.2 | 文件处理 |

## 常见问题

### Q: 编译失败，提示找不到某个包

A: 请确保已经同步 Gradle：
```bash
./gradlew build --refresh-dependencies
```

### Q: Android 版本运行失败

A: 检查以下几点：
1. Android SDK 是否已安装（需要 API 24+）
2. 设备/模拟器系统版本是否 ≥ Android 7.0
3. 清理并重新构建：`./gradlew clean build`

### Q: Desktop 版本字体显示异常

A: 这是 Compose Desktop 的已知问题，可以在主题配置中指定系统字体。

### Q: 如何添加新的语言支持？

A: 
1. 在 `composeApp/src/commonMain/resources/MR/` 下创建新的语言文件夹
2. 复制 `base/strings.xml` 到新文件夹
3. 翻译所有字符串
4. Moko Resources 会自动识别

## 开发工具推荐

1. **Android Studio Ladybug** - 官方 IDE，最佳支持
2. **IntelliJ IDEA Ultimate** - 也支持 KMP 开发
3. **Kotlin Multiplatform Mobile Plugin** - AS 必装插件

## 参考资源

- [Kotlin Multiplatform 官方文档](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform 文档](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Koin 文档](https://insert-koin.io/)
- [Voyager 文档](https://voyager.adriel.cafe/)
- [Ktor 文档](https://ktor.io/)

## 联系和反馈

如有问题，请查看：
- `README.md` - 项目概览
- `PROJECT_STRUCTURE.md` - 详细的项目结构说明

---

**祝开发顺利！🎉**

