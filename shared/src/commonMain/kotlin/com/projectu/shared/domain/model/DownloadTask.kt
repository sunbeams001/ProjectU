package com.projectu.shared.domain.model

/**
 * 下载任务领域模型
 * 表示一个完整的下载任务
 */
data class DownloadTask(
    /**
     * 任务唯一标识符（UUID）
     */
    val id: String,
    
    /**
     * 资源类型
     */
    val resourceType: ResourceType,
    
    /**
     * 资源ID（作品ID或小说ID）
     */
    val resourceId: String,
    
    /**
     * 作品标题
     */
    val title: String,
    
    /**
     * 作者ID
     */
    val authorId: String,
    
    /**
     * 作者名
     */
    val authorName: String,
    
    /**
     * 页码索引（仅插画/漫画有效）
     * null表示下载所有页
     */
    val pageIndex: Int? = null,
    
    /**
     * 总页数
     */
    val totalPages: Int = 1,
    
    /**
     * 是否为R-18作品
     */
    val isR18: Boolean = false,
    
    /**
     * 是否为AI作品
     */
    val isAi: Boolean = false,
    
    /**
     * 作品标签列表
     */
    val tags: List<String> = emptyList(),
    
    /**
     * 缩略图 URL（来自 Pixiv API）
     */
    val thumbnailUrl: String? = null,
    
    /**
     * 发布时间（时间戳）
     */
    val publishTime: Long,
    
    /**
     * 下载时间（时间戳）
     */
    val downloadTime: Long = System.currentTimeMillis(),
    
    /**
     * 下载状态
     */
    val status: DownloadStatus = DownloadStatus.PENDING,
    
    /**
     * 下载进度（0.0 - 1.0）
     */
    val progress: Float = 0f,
    
    /**
     * 目标保存路径（不含文件名）
     */
    val targetPath: String,
    
    /**
     * 文件名（含扩展名）
     */
    val fileName: String,
    
    /**
     * 文件总大小（字节）
     */
    val fileSize: Long = 0L,
    
    /**
     * 已下载大小（字节）
     */
    val downloadedSize: Long = 0L,
    
    /**
     * 错误信息（失败时）
     */
    val error: String? = null,
    
    /**
     * 任务创建时间
     */
    val createdAt: Long = System.currentTimeMillis(),
    
    /**
     * 任务最后更新时间
     */
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 资源类型枚举
 */
enum class ResourceType {
    /**
     * 单图插画
     */
    ILLUSTRATION,
    
    /**
     * 漫画（多图）
     */
    MANGA,
    
    /**
     * 动图（Ugoira）
     */
    UGOIRA,
    
    /**
     * 小说
     */
    NOVEL,
    
    /**
     * 小说系列
     */
    NOVEL_SERIES
}

/**
 * 下载状态枚举
 */
enum class DownloadStatus {
    /**
     * 等待开始
     */
    PENDING,
    
    /**
     * 下载中
     */
    DOWNLOADING,
    
    /**
     * 已暂停
     */
    PAUSED,
    
    /**
     * 下载完成
     */
    COMPLETED,
    
    /**
     * 下载失败
     */
    FAILED,
    
    /**
     * 已取消
     */
    CANCELLED
}
