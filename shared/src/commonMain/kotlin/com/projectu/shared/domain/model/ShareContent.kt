package com.projectu.shared.domain.model

/**
 * 分享数据（平台无关）
 * 
 * 这是准备好的分享数据，由 UseCase 生成，传递给平台特定的 ShareExecutor 执行
 */
sealed interface ShareData {
    /**
     * 纯链接分享
     * 适用场景：分享作品/用户/小说链接
     */
    data class LinkShare(
        val title: String,
        val url: String,
        val description: String
    ) : ShareData
    
    /**
     * 文字+图片分享
     * 适用场景：分享作品，包含作品信息和第一张图
     */
    data class TextWithImage(
        val title: String,
        val text: String,
        val imageUrl: String,
        val url: String
    ) : ShareData
    
    /**
     * 纯图片分享
     * 适用场景：分享单张图片
     */
    data class ImageShare(
        val imageUrl: String,
        val title: String
    ) : ShareData
}

/**
 * 插画分享类型
 */
enum class ArtworkShareType {
    /** 仅链接（标题+URL） */
    LINK_ONLY,
    
    /** 文字+图片（标题+第一张图） */
    TEXT_WITH_IMAGE,
    
    /** 纯图片（第一张） */
    IMAGE_ONLY,
    
    /** 纯图片（指定页） */
    IMAGE_SPECIFIC_PAGE
}

/**
 * 分享结果
 */
sealed interface ShareResult {
    /** 分享成功 */
    data object Success : ShareResult
    
    /** 分享失败 */
    data class Error(val message: String) : ShareResult
    
    /** 用户取消分享 */
    data object Cancelled : ShareResult
}

/**
 * 分享目标（平台特定）
 * 
 * Android: 使用系统分享面板，通常不需要预定义目标
 * Desktop: 提供复制到剪贴板、保存文件等选项
 */
data class ShareTarget(
    val id: String,
    val name: String,
    val icon: String? = null
)
