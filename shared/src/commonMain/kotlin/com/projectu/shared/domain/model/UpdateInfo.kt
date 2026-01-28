package com.projectu.shared.domain.model

/**
 * 更新信息
 */
data class UpdateInfo(
    /**
     * 版本名称 (例如: "1.0.11")
     */
    val versionName: String,
    
    /**
     * 版本代码
     */
    val versionCode: Int,
    
    /**
     * 更新说明
     */
    val releaseNotes: String,
    
    /**
     * 下载链接
     */
    val downloadUrl: String,
    
    /**
     * 文件大小 (字节)
     */
    val fileSize: Long,
    
    /**
     * 发布时间
     */
    val publishedAt: String
)

/**
 * 更新检查结果
 */
sealed class UpdateCheckResult {
    /**
     * 有新版本可用
     */
    data class HasUpdate(val updateInfo: UpdateInfo) : UpdateCheckResult()
    
    /**
     * 已是最新版本
     */
    data object NoUpdate : UpdateCheckResult()
    
    /**
     * 检查失败
     */
    data class Error(val message: String) : UpdateCheckResult()
}
