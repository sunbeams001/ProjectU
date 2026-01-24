package com.projectu.shared.domain.usecase

import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.ArtworkShareType
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.ShareData

/**
 * 准备分享内容用例
 * 
 * 职责：将领域模型（Artwork、Novel等）转换为可分享的数据格式
 * 
 * 设计原则：
 * - 不涉及平台特定逻辑（不处理 Intent、剪贴板等）
 * - 只负责数据转换，不负责文本格式化（格式化由UI层完成）
 * - 生成的 ShareData 可以被任何平台的 ShareExecutor 使用
 */
class PrepareShareContentUseCase {
    
    /**
     * 准备插画分享数据
     * 
     * @param artwork 插画作品
     * @param formattedText 已格式化的文本（由UI层提供，包含本地化）
     * @param shareType 分享类型
     * @param pageIndex 页码（仅当 shareType = IMAGE_SPECIFIC_PAGE 时有效）
     * @return 准备好的分享数据
     */
    fun prepareArtworkShare(
        artwork: Artwork,
        formattedText: String,
        shareType: ArtworkShareType,
        pageIndex: Int = 0
    ): ShareData {
        return when (shareType) {
            ArtworkShareType.LINK_ONLY -> {
                ShareData.LinkShare(
                    title = artwork.title,
                    url = "https://www.pixiv.net/artworks/${artwork.id}",
                    description = formatArtworkDescription(artwork)
                )
            }
            
            ArtworkShareType.TEXT_WITH_IMAGE -> {
                val imageUrl = artwork.imageUrls.pages.firstOrNull()?.urls?.original
                    ?: artwork.imageUrls.pages.firstOrNull()?.urls?.master1200
                    ?: artwork.imageUrls.pages.firstOrNull()?.urls?.large
                    ?: ""
                
                ShareData.TextWithImage(
                    title = artwork.title,
                    text = formattedText,
                    imageUrl = imageUrl,
                    url = "https://www.pixiv.net/artworks/${artwork.id}"
                )
            }
            
            ArtworkShareType.IMAGE_ONLY -> {
                val imageUrl = artwork.imageUrls.pages.firstOrNull()?.urls?.original
                    ?: artwork.imageUrls.pages.firstOrNull()?.urls?.master1200
                    ?: ""
                
                ShareData.ImageShare(
                    imageUrl = imageUrl,
                    title = artwork.title
                )
            }
            
            ArtworkShareType.IMAGE_SPECIFIC_PAGE -> {
                val page = artwork.imageUrls.pages.getOrNull(pageIndex)
                val imageUrl = page?.urls?.original
                    ?: page?.urls?.master1200
                    ?: ""
                
                ShareData.ImageShare(
                    imageUrl = imageUrl,
                    title = "${artwork.title} (${pageIndex + 1}/${artwork.pageCount})"
                )
            }
        }
    }
    
    /**
     * 准备用户分享数据
     * 
     * @param userId 用户ID
     * @param userName 用户名
     * @param formattedDescription 已格式化的描述（由UI层提供）
     * @return 准备好的分享数据
     */
    fun prepareUserShare(
        userId: String,
        userName: String,
        formattedDescription: String
    ): ShareData {
        return ShareData.LinkShare(
            title = userName,
            url = "https://www.pixiv.net/users/$userId",
            description = formattedDescription
        )
    }
    
    /**
     * 准备小说分享数据
     * 
     * @param novel 小说作品
     * @param formattedDescription 已格式化的描述（由UI层提供）
     * @return 准备好的分享数据
     */
    fun prepareNovelShare(
        novel: Novel,
        formattedDescription: String
    ): ShareData {
        return ShareData.LinkShare(
            title = novel.title,
            url = "https://www.pixiv.net/novel/show.php?id=${novel.id}",
            description = formattedDescription
        )
    }
    
    /**
     * 格式化插画描述（用于链接分享）
     */
    private fun formatArtworkDescription(artwork: Artwork): String {
        return buildString {
            append(artwork.title)
            append(" by ${artwork.userName}\n")
            
            if (artwork.description.isNotBlank()) {
                val cleanDescription = artwork.description
                    .replace(Regex("<[^>]*>"), "") // 移除HTML标签
                    .replace(Regex("\\s+"), " ") // 合并多余空格
                    .trim()
                
                append("\n")
                if (cleanDescription.length > 100) {
                    append(cleanDescription.take(100))
                    append("...")
                } else {
                    append(cleanDescription)
                }
            }
        }
    }
    

    

    /**
     * 格式化数字（大于1000时使用k表示）
     * 公开此方法供UI层使用
     */
    fun formatCount(count: Int): String {
        return when {
            count >= 10000 -> "${count / 1000}k"
            count >= 1000 -> String.format("%.1fk", count / 1000.0)
            else -> count.toString()
        }
    }
    
    companion object {
        /**
         * 格式化数字（大于1000时使用k表示）
         * 静态方法，供UI层ShareTextFormatter使用
         */
        fun formatCount(count: Int): String {
            return when {
                count >= 10000 -> "${count / 1000}k"
                count >= 1000 -> String.format("%.1fk", count / 1000.0)
                else -> count.toString()
            }
        }
    }
}
