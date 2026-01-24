package com.projectu.shared.data.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.projectu.shared.domain.model.ShareData
import com.projectu.shared.domain.model.ShareResult
import com.projectu.shared.domain.model.ShareTarget
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android 平台分享实现
 * 
 * 使用 Android Intent 系统实现分享功能
 * 
 * 功能特性：
 * - 链接分享：使用 text/plain MIME 类型
 * - 图片分享：下载图片到缓存目录，使用内容URI提供访问
 * - 文字+图片：组合文字和图片内容
 * 
 * 注意事项：
 * - 图片需要先下载到本地缓存
 * - Android 7.0+ 需要使用 content:// URI（通过 Context.getUriForFile）
 */
class AndroidShareExecutor(
    private val context: Context,
    private val httpClient: HttpClient
) : ShareExecutor {
    
    companion object {
        private const val SHARE_CACHE_DIR = "share_cache"
    }
    
    override suspend fun executeShare(shareData: ShareData): ShareResult {
        return withContext(Dispatchers.Main) {
            try {
                val intent = createShareIntent(shareData)
                val chooser = Intent.createChooser(intent, null) // 标题由系统决定
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
                ShareResult.Success
            } catch (e: Exception) {
                ShareResult.Error(e.message ?: "Share failed")
            }
        }
    }
    
    /**
     * 创建分享 Intent
     */
    private suspend fun createShareIntent(shareData: ShareData): Intent {
        return when (shareData) {
            is ShareData.LinkShare -> createLinkShareIntent(shareData)
            is ShareData.TextWithImage -> createTextWithImageIntent(shareData)
            is ShareData.ImageShare -> createImageShareIntent(shareData)
        }
    }
    
    /**
     * 创建链接分享 Intent
     */
    private fun createLinkShareIntent(shareData: ShareData.LinkShare): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, shareData.title)
            putExtra(Intent.EXTRA_TEXT, buildString {
                append(shareData.description)
                append("\n\n")
                append(shareData.url)
            })
        }
    }
    
    /**
     * 创建文字+图片分享 Intent
     */
    private suspend fun createTextWithImageIntent(shareData: ShareData.TextWithImage): Intent {
        val imageUri = downloadAndCacheImage(shareData.imageUrl, shareData.title)
        
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_TEXT, buildString {
                append(shareData.text)
                append("\n\n")
                append(shareData.url)
            })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    
    /**
     * 创建纯图片分享 Intent
     */
    private suspend fun createImageShareIntent(shareData: ShareData.ImageShare): Intent {
        val imageUri = downloadAndCacheImage(shareData.imageUrl, shareData.title)
        
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    
    /**
     * 下载图片并缓存到本地
     * 
     * @param imageUrl 图片URL
     * @param title 图片标题（用于生成文件名）
     * @return FileProvider URI
     */
    private suspend fun downloadAndCacheImage(imageUrl: String, title: String): Uri {
        return withContext(Dispatchers.IO) {
            try {
                // 创建缓存目录
                val cacheDir = File(context.cacheDir, SHARE_CACHE_DIR)
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }
                
                // 清理旧的缓存文件（保留最近的10个）
                cleanupOldCacheFiles(cacheDir)
                
                // 生成文件名
                val fileExtension = getFileExtension(imageUrl)
                val sanitizedTitle = title
                    .replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5]"), "_")
                    .take(50)
                val fileName = "${sanitizedTitle}_${System.currentTimeMillis()}.$fileExtension"
                val file = File(cacheDir, fileName)
                
                // 下载图片（需要添加Referer header，否则Pixiv服务器会拒绝请求）
                val response = httpClient.get(imageUrl) {
                    headers.append("Referer", "https://www.pixiv.net/")
                }
                val imageData = response.readRawBytes()
                file.writeBytes(imageData)
                
                // 使用 FileProvider 生成 content:// URI（Android 7.0+ 必需）
                val authority = "${context.packageName}.fileprovider"
                FileProvider.getUriForFile(context, authority, file)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to download image: ${e.message}", e)
            }
        }
    }
    
    /**
     * 清理旧的缓存文件
     * 保留最近的N个文件，删除其他文件
     */
    private fun cleanupOldCacheFiles(cacheDir: File, keepCount: Int = 10) {
        try {
            val files = cacheDir.listFiles() ?: return
            if (files.size <= keepCount) return
            
            // 按修改时间排序，删除最旧的文件
            files.sortedBy { it.lastModified() }
                .take(files.size - keepCount)
                .forEach { it.delete() }
        } catch (e: Exception) {
            // 清理失败不影响分享功能
        }
    }
    
    /**
     * 从URL获取文件扩展名
     */
    private fun getFileExtension(url: String): String {
        return when {
            url.contains(".jpg", ignoreCase = true) -> "jpg"
            url.contains(".jpeg", ignoreCase = true) -> "jpg"
            url.contains(".png", ignoreCase = true) -> "png"
            url.contains(".gif", ignoreCase = true) -> "gif"
            url.contains(".webp", ignoreCase = true) -> "webp"
            else -> "jpg" // 默认使用 jpg
        }
    }
    
    override fun isShareSupported(): Boolean = true
    
    override suspend fun getAvailableShareTargets(): List<ShareTarget> {
        // Android 使用系统分享面板，不需要预定义目标
        return emptyList()
    }
}
