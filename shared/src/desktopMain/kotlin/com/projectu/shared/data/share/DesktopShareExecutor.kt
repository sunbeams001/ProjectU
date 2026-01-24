package com.projectu.shared.data.share

import com.projectu.shared.domain.model.ShareData
import com.projectu.shared.domain.model.ShareResult
import com.projectu.shared.domain.model.ShareTarget
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * Desktop 平台分享实现
 * 
 * Desktop 平台的分享功能主要通过以下方式实现：
 * 1. 复制到剪贴板（链接、文字）
 * 2. 保存图片到文件（图片分享，未来扩展）
 * 
 * 设计说明：
 * - 默认行为：将内容复制到剪贴板
 * - 图片分享：暂不支持（可提示用户手动保存）
 * - 返回 Success 表示已复制到剪贴板
 */
class DesktopShareExecutor : ShareExecutor {
    
    override suspend fun executeShare(shareData: ShareData): ShareResult {
        return try {
            when (shareData) {
                is ShareData.LinkShare -> {
                    val text = formatLinkShareText(shareData)
                    copyToClipboard(text)
                    ShareResult.Success
                }
                
                is ShareData.TextWithImage -> {
                    // Desktop 暂不支持图片分享，只复制文字部分
                    val text = buildString {
                        append(shareData.text)
                        append("\n\n")
                        append(shareData.url)
                        append("\n\n")
                        // TODO: 将由UI层在shareData.text中处理本地化
                        append("Image link: ${shareData.imageUrl}")
                    }
                    copyToClipboard(text)
                    ShareResult.Success
                }
                
                is ShareData.ImageShare -> {
                    // Desktop 暂不支持直接分享图片，复制图片URL
                    val text = buildString {
                        append(shareData.title)
                        append("\n")
                        // TODO: 将由UI层处理
                        append("Image link: ${shareData.imageUrl}")
                    }
                    copyToClipboard(text)
                    ShareResult.Success
                }
            }
        } catch (e: Exception) {
            ShareResult.Error("Failed to copy to clipboard: ${e.message}")
        }
    }
    
    /**
     * 格式化链接分享文本
     */
    private fun formatLinkShareText(shareData: ShareData.LinkShare): String {
        return buildString {
            append(shareData.title)
            append("\n\n")
            append(shareData.description)
            append("\n\n")
            append(shareData.url)
        }
    }
    
    /**
     * 复制文本到系统剪贴板
     */
    private fun copyToClipboard(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(text)
        clipboard.setContents(selection, selection)
    }
    
    override fun isShareSupported(): Boolean = true
    
    override suspend fun getAvailableShareTargets(): List<ShareTarget> {
        // Desktop 提供复制到剪贴板选项
        // TODO: 将由UI层处理本地化
        return listOf(
            ShareTarget(
                id = "clipboard",
                name = "Copy to Clipboard"
            )
        )
    }
}
