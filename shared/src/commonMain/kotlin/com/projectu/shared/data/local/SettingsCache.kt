package com.projectu.shared.data.local

import com.projectu.shared.domain.model.ImageQuality
import com.projectu.shared.domain.model.DetailImageQuality
import com.projectu.shared.domain.model.NovelDownloadImageQuality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 应用设置缓存管理器
 * 
 * 统一管理所有需要高频访问的配置项，从 SettingsStore 订阅变化并缓存在内存中
 * 
 * 设计模式：
 * - 单一职责：只负责配置的内存缓存，不负责持久化
 * - 响应式更新：自动订阅 SettingsStore 的 Flow，配置变更时自动更新缓存
 * - 高性能访问：提供同步的 getter 方法，避免数据库查询
 * 
 * 使用场景：
 * - 需要在业务逻辑中频繁访问的配置项
 * - 例如：R18 阈值判定、语言偏好、过滤规则等
 * 
 * @param settingsStore 设置数据源
 */
class SettingsCache(
    private val settingsStore: SettingsStore
) {
    // ==================== 缓存字段 ====================
    
    /**
     * Pixiv API 语言偏好缓存
     * 用于 API 请求时快速获取语言代码
     */
    private val _pixivLanguage = MutableStateFlow(PixivLanguage.SIMPLIFIED_CHINESE)
    val pixivLanguage: StateFlow<PixivLanguage> = _pixivLanguage.asStateFlow()
    
    /**
     * R18 Sanity Level 阈值缓存
     * 用于作品年龄限制判定时快速获取阈值
     */
    private val _r18SanityLevelThreshold = MutableStateFlow(6)
    val r18SanityLevelThreshold: StateFlow<Int> = _r18SanityLevelThreshold.asStateFlow()
    
    /**
     * 插画卡片首选图片质量缓存
     * 用于列表和瀑布流中快速获取图片质量设置（高频访问）
     */
    private val _preferredImageQuality = MutableStateFlow(ImageQuality.SQUARE_MEDIUM)
    val preferredImageQuality: StateFlow<ImageQuality> = _preferredImageQuality.asStateFlow()
    
    /**
     * 插画详情页首选图片质量缓存
     * 用于作品详情页中快速获取图片质量设置（高频访问）
     */
    private val _detailImageQuality = MutableStateFlow(DetailImageQuality.LARGE)
    val detailImageQuality: StateFlow<DetailImageQuality> = _detailImageQuality.asStateFlow()
    
    /**
     * 小说下载首选图片质量缓存
     * 用于下载小说为 EPUB 时快速获取图片质量设置（高频访问 - 每次下载小说都需要）
     */
    private val _novelDownloadImageQuality = MutableStateFlow(NovelDownloadImageQuality.LARGE)
    val novelDownloadImageQuality: StateFlow<NovelDownloadImageQuality> = _novelDownloadImageQuality.asStateFlow()
    
    /**
     * 下载基础路径缓存
     * 用于下载管理器中快速获取下载路径（高频访问 - 每次下载都需要）
     */
    private val _baseDownloadPath = MutableStateFlow("")
    val baseDownloadPath: StateFlow<String> = _baseDownloadPath.asStateFlow()
    
    /**
     * 文件命名模式缓存
     * 用于下载管理器中快速获取文件命名模式（高频访问 - 每次下载都需要）
     */
    private val _fileNameMode = MutableStateFlow(FileNameMode.STANDARD)
    val fileNameMode: StateFlow<FileNameMode> = _fileNameMode.asStateFlow()
    
    /**
     * 自定义文件命名模板缓存
     * 用于下载管理器中快速获取自定义模板（高频访问 - 每次下载都需要）
     */
    private val _customFileNameTemplate = MutableStateFlow("{id}_{p}_{title}")
    val customFileNameTemplate: StateFlow<String> = _customFileNameTemplate.asStateFlow()
    
    // TODO: 后续添加更多配置项缓存
    // private val _someOtherConfig = MutableStateFlow(defaultValue)
    // val someOtherConfig: StateFlow<Type> = _someOtherConfig.asStateFlow()
    
    init {
        // 在后台协程中监听设置变化，自动更新所有缓存
        CoroutineScope(Dispatchers.Default).launch {
            settingsStore.settings.collect { settings ->
                // 更新所有缓存字段
                _pixivLanguage.value = settings.pixivLanguage
                _r18SanityLevelThreshold.value = settings.r18SanityLevelThreshold
                _preferredImageQuality.value = settings.preferredImageQuality
                _detailImageQuality.value = settings.detailImageQuality
                _novelDownloadImageQuality.value = settings.novelDownloadImageQuality
                _baseDownloadPath.value = settings.downloadSettings.baseDownloadPath
                _fileNameMode.value = settings.downloadSettings.fileNameMode
                _customFileNameTemplate.value = settings.downloadSettings.customFileNameTemplate
                
                // TODO: 后续添加更多字段的同步
                // _someOtherConfig.value = settings.someOtherConfig
            }
        }
    }
    
    // ==================== 同步访问方法 ====================
    
    /**
     * 获取当前 Pixiv API 语言偏好（同步方法，使用内存缓存）
     */
    fun getPixivLanguage(): PixivLanguage {
        return _pixivLanguage.value
    }
    
    /**
     * 获取当前 Pixiv API 语言代码（同步方法，使用内存缓存）
     */
    fun getPixivLanguageCode(): String {
        return _pixivLanguage.value.code
    }
    
    /**
     * 获取当前 R18 Sanity Level 阈值（同步方法，使用内存缓存）
     */
    fun getR18SanityLevelThreshold(): Int {
        return _r18SanityLevelThreshold.value
    }
    
    /**
     * 获取当前插画卡片首选图片质量（同步方法，使用内存缓存）
     * 用于 ArtworkCard 等组件快速获取图片质量设置
     */
    fun getPreferredImageQuality(): ImageQuality {
        return _preferredImageQuality.value
    }
    
    /**
     * 获取当前插画详情页首选图片质量（同步方法，使用内存缓存）
     * 用于 ArtworkDetailContent 等组件快速获取图片质量设置
     */
    fun getDetailImageQuality(): DetailImageQuality {
        return _detailImageQuality.value
    }
    
    /**
     * 获取当前小说下载首选图片质量（同步方法，使用内存缓存）
     * 用于 NovelToEpubConverter 等组件快速获取图片质量设置
     * 
     * 性能说明：
     * - 每次下载小说都需要获取图片质量设置
     * - 使用内存缓存避免每次都查询数据库，显著提升性能
     */
    fun getNovelDownloadImageQuality(): NovelDownloadImageQuality {
        return _novelDownloadImageQuality.value
    }
    
    /**
     * 获取当前下载基础路径（同步方法，使用内存缓存）
     * 用于 DownloadManager 等组件快速获取下载路径设置
     * 
     * 性能说明：
     * - 在大量下载场景下（批量下载50+作品），每个任务都需要获取路径
     * - 使用内存缓存避免每次都查询数据库，显著提升性能
     */
    fun getBaseDownloadPath(): String {
        return _baseDownloadPath.value
    }
    
    /**
     * 获取当前文件命名模式（同步方法，使用内存缓存）
     * 用于 DownloadManager 中快速获取文件命名模式
     * 
     * 性能说明：
     * - 每次创建下载任务都需要生成文件名
     * - 使用内存缓存避免每次都查询数据库
     */
    fun getFileNameMode(): FileNameMode {
        return _fileNameMode.value
    }
    
    /**
     * 获取当前自定义文件命名模板（同步方法，使用内存缓存）
     * 用于 DownloadManager 中快速获取自定义模板
     * 
     * 性能说明：
     * - 每次创建下载任务都需要生成文件名
     * - 使用内存缓存避免每次都查询数据库
     */
    fun getCustomFileNameTemplate(): String {
        return _customFileNameTemplate.value
    }
    
    // TODO: 后续添加更多配置项的 getter
    // fun getSomeOtherConfig(): Type {
    //     return _someOtherConfig.value
    // }
}
