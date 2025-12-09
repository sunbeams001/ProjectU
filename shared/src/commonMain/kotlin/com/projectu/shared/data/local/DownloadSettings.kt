package com.projectu.shared.data.local

/**
 * 下载设置数据模型
 * 
 * 注意：路径管理功能已由 DownloadRule 系统接管
 * 此类仅保留基础配置：基础路径、文件命名模板、格式选项
 * 
 * 相关文档：docs/guides/下载系统完整设计文档.md
 */
data class DownloadSettings(
    /**
     * 基础下载路径
     * Android: /storage/emulated/0/Pictures/ProjectU
     * Desktop: ~/Pictures/ProjectU
     * 
     * 此路径作为 DownloadRuleDefaults.getBuiltInRules() 的基础路径
     */
    val baseDownloadPath: String = getDefaultDownloadPath(),
    
    /**
     * 文件命名模板
     * 
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
     * GIF: 保存为GIF格式（体积较大，兼容性好）
     * MP4: 保存为MP4格式（H.264编码，体积小90%，高质量）
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
