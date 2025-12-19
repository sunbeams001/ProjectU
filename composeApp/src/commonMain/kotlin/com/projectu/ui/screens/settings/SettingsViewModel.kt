package com.projectu.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.navigator.Navigator
import com.projectu.shared.data.local.AppLanguage
import com.projectu.shared.data.local.AppSettings
import com.projectu.shared.data.local.FileNameMode
import com.projectu.shared.data.local.PixivLanguage
import com.projectu.shared.data.local.ThemeMode
import com.projectu.shared.domain.model.BookmarkAction
import com.projectu.shared.domain.model.CacheSize
import com.projectu.shared.domain.model.ImageQuality
import com.projectu.shared.domain.repository.AuthRepository
import com.projectu.shared.domain.repository.SettingsRepository
import com.projectu.ui.components.FileNamePreviewExample
import com.projectu.ui.screens.login.LoginScreen
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 设置页面 ViewModel
 * 管理设置相关的状态和业务逻辑
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    // 设置状态流
    val settingsState: StateFlow<AppSettings> = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings.DEFAULT
        )
    
    /**
     * 更新应用语言
     */
    fun updateAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.updateAppLanguage(language)
        }
    }
    
    /**
     * 更新 Pixiv API 语言偏好
     * 支持：简体中文、繁体中文、英语、日语、韩语、泰语、马来语
     * 注意：语言会通过 App.kt 的响应式监听自动同步到 PixivConfig
     */
    fun updatePixivLanguage(language: PixivLanguage) {
        viewModelScope.launch {
            settingsRepository.updatePixivLanguage(language)
        }
    }
    
    /**
     * 更新主题模式
     */
    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(mode)
        }
    }
    
    /**
     * 更新 R18 Sanity Level 阈值
     * 阈值范围: 0-9
     * - 0-1: 安全内容
     * - 2-3: 正常内容
     * - 4-5: 暗示性内容
     * - 6-9: R18 内容
     */
    fun updateR18SanityLevelThreshold(threshold: Int) {
        viewModelScope.launch {
            settingsRepository.updateR18SanityLevelThreshold(threshold)
        }
    }
    
    /**
     * 更新插画卡片首选图片质量
     */
    fun updatePreferredImageQuality(quality: ImageQuality) {
        viewModelScope.launch {
            settingsRepository.updatePreferredImageQuality(quality)
        }
    }
    
    /**
     * 更新插画详情页首选图片质量
     */
    fun updateDetailImageQuality(quality: com.projectu.shared.domain.model.DetailImageQuality) {
        viewModelScope.launch {
            settingsRepository.updateDetailImageQuality(quality)
        }
    }
    
    /**
     * 更新小说下载首选图片质量
     */
    fun updateNovelDownloadImageQuality(quality: com.projectu.shared.domain.model.NovelDownloadImageQuality) {
        viewModelScope.launch {
            settingsRepository.updateNovelDownloadImageQuality(quality)
        }
    }
    
    /**
     * 更新图片缓存大小
     * 注意：缓存大小变更需要重启应用才能完全生效
     */
    fun updateImageCacheSize(size: CacheSize) {
        viewModelScope.launch {
            settingsRepository.updateImageCacheSize(size)
        }
    }
    
    /**
     * 更新下载基础路径
     */
    fun updateBaseDownloadPath(path: String) {
        viewModelScope.launch {
            settingsRepository.updateBaseDownloadPath(path)
        }
    }
    
    /**
     * 重置设置
     */
    fun resetSettings() {
        viewModelScope.launch {
            settingsRepository.resetSettings()
        }
    }
    
    /**
     * 编辑 PHPSESSID
     */
    fun editPhpSessionId(newPhpSessionId: String) {
        viewModelScope.launch {
            authRepository.saveCredentials(newPhpSessionId)
        }
    }
    
    /**
     * 登出
     */
    fun logout(navigator: Navigator) {
        viewModelScope.launch {
            authRepository.clearCredentials()
            // 跳转到登录页面
            navigator.replaceAll(LoginScreen())
        }
    }
    
    /**
     * 更新点击收藏按钮的行为
     */
    fun updateClickBookmarkAction(action: BookmarkAction) {
        viewModelScope.launch {
            settingsRepository.updateClickBookmarkAction(action)
        }
    }
    
    /**
     * 更新长按收藏按钮的行为
     */
    fun updateLongPressBookmarkAction(action: BookmarkAction) {
        viewModelScope.launch {
            settingsRepository.updateLongPressBookmarkAction(action)
        }
    }
    
    // ==================== 文件命名模板相关 ====================
    
    /**
     * 文件命名模板预览示例
     */
    val fileNamePreviewExamples: StateFlow<List<FileNamePreviewExample>> = settingsState.map { settings ->
        val template = when (settings.downloadSettings.fileNameMode) {
            FileNameMode.STANDARD -> "{id}_{p}_{title}"
            FileNameMode.CUSTOM -> settings.downloadSettings.customFileNameTemplate
        }
        generatePreviewExamples(template)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    /**
     * 自定义模板验证错误信息（仅在自定义模式下有效）
     */
    val templateValidationError: StateFlow<String?> = settingsState.map { settings ->
        if (settings.downloadSettings.fileNameMode == FileNameMode.CUSTOM) {
            val validation = validateCustomTemplate(settings.downloadSettings.customFileNameTemplate)
            validation.error
        } else {
            null
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    
    /**
     * 自定义模板验证警告信息（仅在自定义模式下有效）
     */
    val templateValidationWarning: StateFlow<String?> = settingsState.map { settings ->
        if (settings.downloadSettings.fileNameMode == FileNameMode.CUSTOM) {
            val validation = validateCustomTemplate(settings.downloadSettings.customFileNameTemplate)
            validation.warning
        } else {
            null
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    
    /**
     * 自定义模板验证结果
     */
    data class TemplateValidation(
        val isValid: Boolean,
        val error: String? = null,
        val warning: String? = null
    )
    
    /**
     * 验证自定义模板
     */
    fun validateCustomTemplate(template: String): TemplateValidation {
        // 1. 检查是否为空
        if (template.isBlank()) {
            return TemplateValidation(
                isValid = false,
                error = "Template cannot be empty"
            )
        }
        
        // 2. 检查是否包含必需变量（至少有 {id} 或 {title}）
        if (!template.contains("{id}") && !template.contains("{title}")) {
            return TemplateValidation(
                isValid = false,
                error = "Template must contain {id} or {title} variable"
            )
        }
        
        // 3. 检查是否包含非法字符
        val illegalChars = Regex("""[\\/:*?"<>|]""")
        if (illegalChars.containsMatchIn(template)) {
            return TemplateValidation(
                isValid = false,
                error = "Template cannot contain special characters: \\ / : * ? \" < > |"
            )
        }
        
        // 4. 警告：多页漫画可能冲突
        if (!template.contains("{p}")) {
            return TemplateValidation(
                isValid = true,
                warning = "⚠️ Template does not contain {p}, multi-page manga may have filename conflicts"
            )
        }
        
        return TemplateValidation(isValid = true)
    }
    
    /**
     * 生成预览示例
     */
    private fun generatePreviewExamples(template: String): List<FileNamePreviewExample> {
        return listOf(
            FileNamePreviewExample(
                type = "Illustration",
                fileName = simulateFileName(template, isMultiPage = false, pageIndex = 0) + ".jpg"
            ),
            FileNamePreviewExample(
                type = "Manga P1",
                fileName = simulateFileName(template, isMultiPage = true, pageIndex = 0) + ".jpg"
            ),
            FileNamePreviewExample(
                type = "Manga P2",
                fileName = simulateFileName(template, isMultiPage = true, pageIndex = 1) + ".jpg"
            ),
            FileNamePreviewExample(
                type = "Ugoira",
                fileName = simulateFileName(template, isUgoira = true) + ".gif"
            ),
            FileNamePreviewExample(
                type = "Novel",
                fileName = simulateFileName(template, isNovel = true) + ".epub"
            )
        )
    }
    
    /**
     * 模拟文件名生成
     */
    private fun simulateFileName(
        template: String,
        isMultiPage: Boolean = false,
        isUgoira: Boolean = false,
        isNovel: Boolean = false,
        pageIndex: Int = 0
    ): String {
        // 对于动图和小说，移除 {p}
        val effectiveTemplate = if (isUgoira || isNovel) {
            template
                .replace("_{p}_", "_")
                .replace("_{p}", "")
                .replace("{p}_", "")
                .replace("{p}", "")
        } else {
            template
        }
        
        var result = effectiveTemplate
            .replace("{id}", "123456789")
            .replace("{p}", pageIndex.toString())
            .replace("{title}", if (isNovel) "Novel Title" else if (isUgoira) "Ugoira Title" else if (isMultiPage) "Manga Title" else "Landscape")
            .replace("{author_id}", "987654321")
            .replace("{author_name}", "Artist Name")
            .replace("{publish_date}", "2025-01-15")
            .replace("{download_date}", "2025-12-18")
            .replace("{ai}", "")
            .replace("{r18}", "")
            .replace("{tags}", "")
        
        // 清理连续的分隔符，但保留末尾的单个分隔符
        result = result
            .replace(Regex("_+"), "_")  // 多个下划线合并为一个
            .replace(Regex("-+"), "-")  // 多个连字符合并为一个
            .replace(Regex(" +"), " ")  // 多个空格合并为一个
            .replace(Regex("^[_\\-\\s]+"), "")  // 只移除开头的分隔符
        
        return result
    }
    
    /**
     * 更新文件命名模式
     */
    fun updateFileNameMode(mode: FileNameMode) {
        viewModelScope.launch {
            settingsRepository.updateFileNameMode(mode)
        }
    }
    
    /**
     * 更新自定义文件命名模板
     */
    fun updateCustomFileNameTemplate(template: String) {
        viewModelScope.launch {
            settingsRepository.updateCustomFileNameTemplate(template)
        }
    }
}

