package com.projectu.shared.data.util

import com.projectu.shared.data.local.DownloadSettings
import com.projectu.shared.data.local.FileNameMode
import com.projectu.shared.domain.model.DownloadTask
import com.projectu.shared.domain.model.ResourceType
import okio.FileSystem
import java.text.SimpleDateFormat
import java.util.*

/**
 * 下载路径构建器
 * 
 * 注意：路径生成功能已由 DownloadRule 系统接管
 * 此类仅保留文件名生成功能
 * 
 * 相关文档：docs/guides/下载系统完整设计文档.md
 */
class DownloadPathBuilder(
    private val fileSystem: FileSystem
) {
    
    /**
     * 生成文件名（含扩展名）
     * 
     * 这是唯一保留的功能，路径生成已由 DownloadRule 系统接管
     * 
     * 命名规则：
     * - 插画/漫画：使用完整模板（包含 {p} 页码）
     * - 动图/小说/小说系列：移除 {p} 变量（它们永远是单文件）
     */
    fun buildFileName(task: DownloadTask, settings: DownloadSettings, extension: String = "jpg"): String {
        // 根据模式获取模板
        val baseTemplate = when (settings.fileNameMode) {
            FileNameMode.STANDARD -> "{id}_{p}_{title}"
            FileNameMode.CUSTOM -> settings.customFileNameTemplate
        }
        
        // 对于动图、小说、小说系列，它们永远只有单个文件，移除模板中的 {p} 及其前后的下划线
        val effectiveTemplate = when (task.resourceType) {
            ResourceType.UGOIRA, ResourceType.NOVEL, ResourceType.NOVEL_SERIES -> {
                // 移除 {p} 及其前后的下划线
                // 例如: "{id}_{p}_{title}" -> "{id}_{title}"
                baseTemplate
                    .replace("_{p}_", "_")   // 中间的 _{p}_
                    .replace("_{p}", "")     // 末尾的 _{p}
                    .replace("{p}_", "")     // 开头的 {p}_
                    .replace("{p}", "")      // 单独的 {p}
            }
            else -> baseTemplate  // 插画和漫画保持原样
        }
        
        // 替换模板变量
        val fileName = effectiveTemplate
            .replace("{id}", task.resourceId)
            .replace("{p}", task.pageIndex?.toString() ?: "0")
            .replace("{title}", sanitizeFileName(task.title))
            .replace("{author_id}", task.authorId)
            .replace("{author_name}", sanitizeFileName(task.authorName))
            .replace("{publish_date}", formatDate(task.publishTime))
            .replace("{download_date}", formatDate(task.downloadTime))
            .replace("{ai}", if (task.isAi) "AI" else "")
            .replace("{r18}", if (task.isR18) "R-18" else "")
            .replace("{tags}", task.tags.take(5).joinToString("_") { sanitizeFileName(it) })
        
        // 清理连续的下划线和首尾空格
        val cleanFileName = fileName
            .replace(Regex("_+"), "_")
            .trim('_', ' ')
            .let { sanitizeFileName(it) }
        
        return "$cleanFileName.$extension"
    }
    
    /**
     * 文件名合法化处理（跨平台）
     * 移除或替换Windows/Linux/macOS不支持的字符
     */
    private fun sanitizeFileName(name: String): String {
        // Windows禁用字符: \ / : * ? " < > |
        // macOS/Linux禁用字符: /
        // 同时移除控制字符
        val illegalChars = Regex("""[\\/:*?"<>|\u0000-\u001F\u007F]""")
        
        return name
            .replace(illegalChars, "_")
            .trim()
            .take(200) // 限制长度，避免路径过长（Windows MAX_PATH = 260）
            .ifEmpty { "untitled" } // 避免空文件名
    }
    
    /**
     * 格式化日期（yyyy-MM-dd）
     */
    private fun formatDate(timestamp: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.format(Date(timestamp))
    }
}
