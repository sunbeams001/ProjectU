package com.projectu.ui.util

import androidx.compose.runtime.Composable
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.usecase.PrepareShareContentUseCase
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.Res
import projectu.composeapp.generated.resources.*

/**
 * 分享文本格式化器
 * 
 * 职责：
 * - 负责生成本地化的分享文本
 * - 使用字符串资源实现多语言支持
 * - 在Composable上下文中使用
 */
object ShareTextFormatter {
    
    /**
     * 格式化插画文本（用于文字+图片分享）
     */
    @Composable
    fun formatArtworkText(artwork: Artwork): String {
        val leftQuote = stringResource(Res.string.share_left_quote)
        val rightQuote = stringResource(Res.string.share_right_quote)
        val authorLabel = stringResource(Res.string.share_author_label)
        
        return buildString {
            append("$leftQuote${artwork.title}$rightQuote\n")
            append("$authorLabel${artwork.userName}\n")
            append("❤️ ${PrepareShareContentUseCase.formatCount(artwork.likeCount)}  ")
            append("⭐ ${PrepareShareContentUseCase.formatCount(artwork.bookmarkCount)}  ")
            append("👁️ ${PrepareShareContentUseCase.formatCount(artwork.viewCount)}")
        }
    }
    
    /**
     * 格式化小说描述
     */
    @Composable
    fun formatNovelDescription(novel: Novel): String {
        val wordCountUnit = stringResource(Res.string.share_word_count_unit)
        val aboutPrefix = stringResource(Res.string.share_about_prefix)
        val minutesUnit = stringResource(Res.string.share_minutes_unit)
        
        return buildString {
            append(novel.title)
            append(" by ${novel.userName}\n")
            
            if (novel.description.isNotBlank()) {
                val cleanDescription = novel.description
                    .replace(Regex("<[^>]*>"), "") // 移除HTML标签
                    .replace(Regex("\\s+"), " ") // 合并多余空格
                    .trim()
                
                append("\n")
                if (cleanDescription.length > 150) {
                    append(cleanDescription.take(150))
                    append("...")
                } else {
                    append(cleanDescription)
                }
            }
            
            append("\n\n")
            append("📖 ${PrepareShareContentUseCase.formatCount(novel.textCount)}$wordCountUnit")
            
            val readingMinutes = novel.readingTime / 60
            if (readingMinutes > 0) {
                append(" · ⏱️ $aboutPrefix${readingMinutes}$minutesUnit")
            }
        }
    }
    
    /**
     * 格式化用户描述
     */
    @Composable
    fun formatUserDescription(userName: String): String {
        val userPrefix = stringResource(Res.string.share_user_prefix)
        return "$userPrefix$userName"
    }
}
