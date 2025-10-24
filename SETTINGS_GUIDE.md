# 设置系统使用指南

## 概述

本项目已经实现了一个完整的、可扩展的设置系统，支持以下设置项：

1. **应用语言设置** - 控制应用界面的显示语言
2. **Pixiv 语言设置** - 控制从 Pixiv API 获取数据时的语言偏好
3. **主题模式** - 支持浅色、深色和跟随系统

## 架构设计

### 分层结构

```
┌─────────────────────────────────────┐
│  UI Layer (SettingsScreen)          │
│  - 设置界面                          │
│  - SettingsViewModel                │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│  Domain Layer                       │
│  - SettingsRepository (接口)        │
│  - SyncPixivLanguageUseCase         │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│  Data Layer                         │
│  - SettingsRepositoryImpl           │
│  - SettingsStore                    │
│  - AppSettings (数据模型)           │
└─────────────────────────────────────┘
```

## 核心文件说明

### 1. 数据模型

#### `AppSettings.kt`
定义了应用的所有设置项：
```kotlin
data class AppSettings(
    val appLanguage: AppLanguage,      // 应用界面语言
    val pixivLanguage: PixivLanguage,  // Pixiv 数据语言
    val themeMode: ThemeMode           // 主题模式
)
```

#### `AppLanguage` 枚举
支持的应用语言：
- `SIMPLIFIED_CHINESE` - 简体中文 (zh-CN)
- `TRADITIONAL_CHINESE` - 繁体中文 (zh-TW)
- `ENGLISH` - 英文 (en)
- `JAPANESE` - 日文 (ja)
- `KOREAN` - 韩文 (ko)

#### `PixivLanguage` 枚举
支持的 Pixiv API 语言：
- `SIMPLIFIED_CHINESE` - 简体中文 (zh)
- `TRADITIONAL_CHINESE` - 繁体中文 (zh-tw)
- `ENGLISH` - 英文 (en)
- `JAPANESE` - 日文 (ja)
- `KOREAN` - 韩文 (ko)

### 2. 存储层

#### `SettingsStore.kt`
管理设置的内存存储（注意：这是临时实现，生产环境应使用 DataStore）：
```kotlin
class SettingsStore {
    val settings: Flow<AppSettings>
    fun setAppLanguage(language: AppLanguage)
    fun setPixivLanguage(language: PixivLanguage)
    fun setThemeMode(mode: ThemeMode)
}
```

### 3. 仓储层

#### `SettingsRepository.kt` (接口)
定义设置数据访问的抽象接口。

#### `SettingsRepositoryImpl.kt` (实现)
使用 `SettingsStore` 实现设置的 CRUD 操作。

### 4. UI 层

#### `SettingsScreen.kt`
设置页面的 Compose UI 实现，包含：
- 分组的设置项列表
- 语言选择对话框
- 主题选择对话框

#### `SettingsViewModel.kt`
管理设置相关的状态和业务逻辑。

### 5. 集成层

#### Pixiv 语言同步
`SyncPixivLanguageUseCase` - 自动将 Pixiv 语言设置同步到 `PixivConfig`。

## 使用方式

### 1. 访问设置页面

从"我的"标签页点击"设置"按钮即可进入设置页面：

```kotlin
// 在 ProfileTab 中
Button(onClick = { navigator.push(SettingsScreen()) }) {
    Icon(Icons.Default.Settings)
    Text("设置")
}
```

### 2. 在代码中访问设置

#### 通过 ViewModel 访问
```kotlin
@Composable
fun MyScreen() {
    val viewModel: SettingsViewModel = koinInject()
    val settings by viewModel.settingsState.collectAsState()
    
    // 使用设置
    Text("当前语言: ${settings.appLanguage.displayName}")
}
```

#### 通过 Repository 访问
```kotlin
class MyUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend fun doSomething() {
        val settings = settingsRepository.getCurrentSettings()
        // 使用设置
    }
}
```

### 3. 修改设置

#### 通过 ViewModel
```kotlin
viewModel.updateAppLanguage(AppLanguage.ENGLISH)
viewModel.updatePixivLanguage(PixivLanguage.JAPANESE)
viewModel.updateThemeMode(ThemeMode.DARK)
```

#### 通过 Repository
```kotlin
settingsRepository.updateAppLanguage(AppLanguage.ENGLISH)
```

### 4. 监听设置变化

```kotlin
@Composable
fun MyScreen() {
    val settingsRepository: SettingsRepository = koinInject()
    
    LaunchedEffect(Unit) {
        settingsRepository.getSettings().collect { settings ->
            // 响应设置变化
            println("语言已更改为: ${settings.appLanguage}")
        }
    }
}
```

## 如何扩展新的设置项

### 步骤 1: 更新数据模型

在 `AppSettings.kt` 中添加新字段：

```kotlin
data class AppSettings(
    val appLanguage: AppLanguage,
    val pixivLanguage: PixivLanguage,
    val themeMode: ThemeMode,
    val newSetting: String = "default"  // 新增设置项
)
```

### 步骤 2: 更新 SettingsStore

在 `SettingsStore.kt` 中添加修改方法：

```kotlin
class SettingsStore {
    // ...
    
    fun setNewSetting(value: String) {
        _settings.value = _settings.value.copy(newSetting = value)
    }
}
```

### 步骤 3: 更新 Repository

在 `SettingsRepository.kt` 中添加接口方法：

```kotlin
interface SettingsRepository {
    // ...
    suspend fun updateNewSetting(value: String)
}
```

在 `SettingsRepositoryImpl.kt` 中实现：

```kotlin
override suspend fun updateNewSetting(value: String) {
    settingsStore.setNewSetting(value)
}
```

### 步骤 4: 更新 ViewModel

在 `SettingsViewModel.kt` 中添加方法：

```kotlin
fun updateNewSetting(value: String) {
    viewModelScope.launch {
        settingsRepository.updateNewSetting(value)
    }
}
```

### 步骤 5: 更新 UI

在 `SettingsScreen.kt` 中添加设置项：

```kotlin
// 在 LazyColumn 中添加新的设置项
item {
    SettingsItem(
        title = "新设置项",
        subtitle = settings.newSetting,
        onClick = { /* 显示选择对话框 */ }
    )
}
```

### 步骤 6: 添加多语言字符串

在所有语言的 `strings.xml` 文件中添加相应的字符串资源。

## 多语言字符串资源

设置相关的字符串已添加到以下文件：
- `composeApp/src/commonMain/resources/MR/base/strings.xml` (英文)
- `composeApp/src/commonMain/resources/MR/zh-rCN/strings.xml` (简体中文)
- `composeApp/src/commonMain/resources/MR/zh-rTW/strings.xml` (繁体中文)
- `composeApp/src/commonMain/resources/MR/ja/strings.xml` (日文)
- `composeApp/src/commonMain/resources/MR/ko/strings.xml` (韩文)

可用的字符串 key：
- `settings_title` - 设置
- `settings_general` - 通用设置
- `settings_pixiv` - Pixiv 设置
- `settings_app_language` - 应用语言
- `settings_pixiv_language` - Pixiv 语言偏好
- `settings_pixiv_language_desc` - 获取 Pixiv 数据时的语言设置
- `settings_theme_mode` - 主题模式
- `settings_select_app_language` - 选择应用语言
- `settings_select_pixiv_language` - 选择 Pixiv 语言
- `settings_select_theme` - 选择主题

## Pixiv 语言同步机制

当用户修改 Pixiv 语言设置时，系统会自动将语言偏好同步到 `PixivConfig`：

```kotlin
// 在 SettingsViewModel 中
fun updatePixivLanguage(language: PixivLanguage) {
    viewModelScope.launch {
        settingsRepository.updatePixivLanguage(language)
        // 自动同步到 PixivConfig
    }
}
```

也可以手动同步：
```kotlin
val syncUseCase: SyncPixivLanguageUseCase = koinInject()
syncUseCase.syncNow()
```

## 依赖注入配置

设置系统已在 Koin 中注册：

### Android 平台
```kotlin
// composeApp/src/androidMain/kotlin/com/projectu/di/PlatformModule.android.kt
actual val databaseModule: Module = module {
    single { SettingsStore() }
}

actual val repositoryModule: Module = module {
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
}

actual val viewModelModule: Module = module {
    viewModel { SettingsViewModel(get()) }
}
```

### Desktop 平台
```kotlin
// composeApp/src/desktopMain/kotlin/com/projectu/di/PlatformModule.desktop.kt
actual val databaseModule: Module = module {
    single { SettingsStore() }
}

actual val repositoryModule: Module = module {
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
}

actual val viewModelModule: Module = module {
    single { SettingsViewModel(get()) }
}
```

## 持久化存储 (TODO)

当前实现使用内存存储（`SettingsStore`），应用重启后设置会丢失。

**后续改进建议：**

1. **Android**: 使用 Jetpack DataStore
   ```kotlin
   val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")
   ```

2. **Desktop**: 使用文件存储或 SQLite
   ```kotlin
   // 使用 kotlinx.serialization 保存到 JSON 文件
   val settingsFile = File(appDir, "settings.json")
   ```

3. 更新 `SettingsStore` 以支持持久化：
   ```kotlin
   class SettingsStore(
       private val dataStore: DataStore<Preferences>  // 或其他持久化方案
   ) {
       // 从持久化存储加载设置
       // 保存设置到持久化存储
   }
   ```

## 最佳实践

1. **单一数据源**: 所有设置都通过 `SettingsRepository` 访问
2. **响应式更新**: 使用 Flow 监听设置变化，自动更新 UI
3. **类型安全**: 使用枚举而不是字符串来表示选项
4. **可扩展性**: 遵循既定模式添加新的设置项
5. **国际化**: 所有文本都使用多语言字符串资源

## 故障排查

### 设置没有保存
- 检查是否正确调用了 ViewModel 的更新方法
- 当前使用内存存储，应用重启会丢失设置

### Pixiv 语言没有同步
- 确保 `PixivConfigStore` 已在 Koin 中注册
- 检查 `SyncPixivLanguageUseCase` 是否正确执行

### UI 没有更新
- 确保在 Composable 中使用 `collectAsState()` 收集 Flow
- 检查 ViewModel 是否正确注入

## 未来改进

- [ ] 实现持久化存储（DataStore）
- [ ] 添加设置导入/导出功能
- [ ] 添加设置重置确认对话框
- [ ] 实现应用语言切换后自动重启
- [ ] 添加更多设置项（缓存大小、下载质量等）
- [ ] 实现设置搜索功能
- [ ] 添加设置项的描述和帮助文本

