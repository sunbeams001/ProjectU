package com.projectu.shared.data.local

/**
 * 下载设置数据模型
 * 存储下载相关的配置信息
 */
data class DownloadSettings(
    /**
     * 基础下载路径
     * Android: /storage/emulated/0/Pictures/ProjectU
     * Desktop: ~/Pictures/ProjectU
     */
    val baseDownloadPath: String = getDefaultDownloadPath(),
    
    // ==================== 分类路径设置 ====================
    /**
     * 插画保存路径（相对于基础路径）
     */
    val illustPath: String = "Illustrations",
    
    /**
     * 漫画保存路径（相对于基础路径）
     */
    val mangaPath: String = "Manga",
    
    /**
     * 动图保存路径（相对于基础路径）
     */
    val ugoiraPath: String = "Ugoira",
    
    /**
     * 小说保存路径（相对于基础路径）
     */
    val novelPath: String = "Novels",
    
    /**
     * 小说系列保存路径（相对于基础路径）
     */
    val novelSeriesPath: String = "NovelSeries",
    
    // ==================== R-18分离设置 ====================
    /**
     * 插画R-18作品是否保存到单独文件夹
     */
    val separateR18Illust: Boolean = false,
    
    /**
     * 插画R-18作品保存路径
     */
    val r18IllustPath: String = "R18/Illustrations",
    
    /**
     * 漫画R-18作品是否保存到单独文件夹
     */
    val separateR18Manga: Boolean = false,
    
    /**
     * 漫画R-18作品保存路径
     */
    val r18MangaPath: String = "R18/Manga",
    
    /**
     * 动图R-18作品是否保存到单独文件夹
     */
    val separateR18Ugoira: Boolean = false,
    
    /**
     * 动图R-18作品保存路径
     */
    val r18UgoiraPath: String = "R18/Ugoira",
    
    /**
     * 小说R-18作品是否保存到单独文件夹
     */
    val separateR18Novel: Boolean = false,
    
    /**
     * 小说R-18作品保存路径
     */
    val r18NovelPath: String = "R18/Novels",
    
    // ==================== AI作品分离设置 ====================
    /**
     * 插画AI作品是否保存到单独文件夹
     */
    val separateAiIllust: Boolean = false,
    
    /**
     * 插画AI作品保存路径
     */
    val aiIllustPath: String = "AI/Illustrations",
    
    /**
     * 漫画AI作品是否保存到单独文件夹
     */
    val separateAiManga: Boolean = false,
    
    /**
     * 漫画AI作品保存路径
     */
    val aiMangaPath: String = "AI/Manga",
    
    /**
     * 动图AI作品是否保存到单独文件夹
     */
    val separateAiUgoira: Boolean = false,
    
    /**
     * 动图AI作品保存路径
     */
    val aiUgoiraPath: String = "AI/Ugoira",
    
    // ==================== 其他设置 ====================
    /**
     * 作者文件夹模式
     * DISABLED: 不启用
     * AUTHOR_ID: 按作者ID分文件夹
     * AUTHOR_NAME: 按作者名分文件夹
     */
    val authorFolderMode: AuthorFolderMode = AuthorFolderMode.DISABLED,
    
    /**
     * 文件命名模板
     * 支持的变量：
     * - {id}: 作品ID
     * - {p}: P数（页码）
     * - {title}: 作品标题
     * - {author_id}: 作者ID
     * - {author_name}: 作者名
     * - {publish_date}: 发布时间
     * - {download_date}: 下载时间
     * - {ai}: AI标识
     * - {r18}: R-18标识
     * - {tags}: 标签（多个用_连接）
     */
    val fileNameTemplate: String = "{id}_{p}_{title}",
    
    /**
     * Ugoira动图格式
     * GIF: 保存为GIF格式
     * MP4: 保存为MP4格式（未来支持）
     */
    val ugoiraFormat: UgoiraFormat = UgoiraFormat.GIF,
    
    /**
     * 下载质量选项
     * ORIGINAL: 下载最高质量原图
     * CACHED: 优先使用已缓存的图片
     */
    val downloadQuality: DownloadQuality = DownloadQuality.ORIGINAL
) {
    companion object {
        /**
         * 默认设置
         */
        val DEFAULT = DownloadSettings()
    }
}

/**
 * 作者文件夹模式枚举
 */
enum class AuthorFolderMode {
    /**
     * 不启用作者文件夹
     */
    DISABLED,
    
    /**
     * 按作者ID创建文件夹
     */
    AUTHOR_ID,
    
    /**
     * 按作者名创建文件夹
     */
    AUTHOR_NAME
}

/**
 * Ugoira格式枚举
 */
enum class UgoiraFormat {
    /**
     * GIF格式（体积较大，兼容性好）
     */
    GIF,
    
    /**
     * MP4格式（H.264编码，体积小90%，高质量）
     */
    MP4
}

/**
 * 下载质量枚举
 */
enum class DownloadQuality {
    /**
     * 下载最高质量原图
     */
    ORIGINAL,
    
    /**
     * 优先使用已缓存的图片
     */
    CACHED
}
