package com.projectu.shared.data.util

import com.projectu.shared.data.local.AuthorFolderMode
import com.projectu.shared.data.local.DownloadSettings
import com.projectu.shared.domain.model.DownloadTask
import com.projectu.shared.domain.model.ResourceType
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import java.text.SimpleDateFormat
import java.util.*

/**
 * 下载路径构建器
 * 负责根据配置和任务信息生成完整的文件路径和文件名
 */
class DownloadPathBuilder(
    private val fileSystem: FileSystem
) {
    
    /**
     * 生成完整的下载路径（不含文件名）
     */
    fun buildPath(task: DownloadTask, settings: DownloadSettings): Path {
        val basePath = settings.baseDownloadPath.toPath()
        val relativePath = buildRelativePath(task, settings)
        return basePath / relativePath
    }
    
    /**
     * 生成相对路径（不包含基础路径）
     * 用于支持 SAF URI 的路径构建
     */
    fun buildRelativePath(task: DownloadTask, settings: DownloadSettings): String {
        // 1. 确定资源类型的基础路径
        var relativePath = getResourceTypePath(task.resourceType, settings)
        
        // 2. 处理R-18分离
        if (task.isR18 && shouldSeparateR18(task.resourceType, settings)) {
            relativePath = getR18Path(task.resourceType, settings)
        }
        
        // 3. 处理AI作品分离
        if (task.isAi && shouldSeparateAi(task.resourceType, settings)) {
            relativePath = getAiPath(task.resourceType, settings)
        }
        
        // 4. 处理作者文件夹
        if (settings.authorFolderMode != AuthorFolderMode.DISABLED) {
            val authorFolder = when (settings.authorFolderMode) {
                AuthorFolderMode.AUTHOR_ID -> task.authorId
                AuthorFolderMode.AUTHOR_NAME -> sanitizeFileName(task.authorName)
                else -> ""
            }
            relativePath = "$relativePath/$authorFolder"
        }
        
        return relativePath
    }
    
    /**
     * 生成文件名（含扩展名）
     */
    fun buildFileName(task: DownloadTask, settings: DownloadSettings, extension: String = "jpg"): String {
        val template = settings.fileNameTemplate
        
        // 替换模板变量
        val fileName = template
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
     * 获取资源类型对应的基础路径
     */
    private fun getResourceTypePath(resourceType: ResourceType, settings: DownloadSettings): String {
        return when (resourceType) {
            ResourceType.ILLUSTRATION -> settings.illustPath
            ResourceType.MANGA -> settings.mangaPath
            ResourceType.UGOIRA -> settings.ugoiraPath
            ResourceType.NOVEL -> settings.novelPath
            ResourceType.NOVEL_SERIES -> settings.novelSeriesPath
        }
    }
    
    /**
     * 是否应该分离R-18作品
     */
    private fun shouldSeparateR18(resourceType: ResourceType, settings: DownloadSettings): Boolean {
        return when (resourceType) {
            ResourceType.ILLUSTRATION -> settings.separateR18Illust
            ResourceType.MANGA -> settings.separateR18Manga
            ResourceType.UGOIRA -> settings.separateR18Ugoira
            ResourceType.NOVEL -> settings.separateR18Novel
            ResourceType.NOVEL_SERIES -> false // 小说系列不支持R-18分离
        }
    }
    
    /**
     * 获取R-18作品路径
     */
    private fun getR18Path(resourceType: ResourceType, settings: DownloadSettings): String {
        return when (resourceType) {
            ResourceType.ILLUSTRATION -> settings.r18IllustPath
            ResourceType.MANGA -> settings.r18MangaPath
            ResourceType.UGOIRA -> settings.r18UgoiraPath
            ResourceType.NOVEL -> settings.r18NovelPath
            else -> getResourceTypePath(resourceType, settings)
        }
    }
    
    /**
     * 是否应该分离AI作品
     */
    private fun shouldSeparateAi(resourceType: ResourceType, settings: DownloadSettings): Boolean {
        return when (resourceType) {
            ResourceType.ILLUSTRATION -> settings.separateAiIllust
            ResourceType.MANGA -> settings.separateAiManga
            ResourceType.UGOIRA -> settings.separateAiUgoira
            else -> false // 小说不支持AI分离
        }
    }
    
    /**
     * 获取AI作品路径
     */
    private fun getAiPath(resourceType: ResourceType, settings: DownloadSettings): String {
        return when (resourceType) {
            ResourceType.ILLUSTRATION -> settings.aiIllustPath
            ResourceType.MANGA -> settings.aiMangaPath
            ResourceType.UGOIRA -> settings.aiUgoiraPath
            else -> getResourceTypePath(resourceType, settings)
        }
    }
    
    /**
     * 格式化日期（yyyy-MM-dd）
     */
    private fun formatDate(timestamp: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.format(Date(timestamp))
    }
    
    /**
     * 确保目录存在
     */
    fun ensureDirectoryExists(path: Path) {
        if (!fileSystem.exists(path)) {
            fileSystem.createDirectories(path)
        }
    }
    
    /**
     * 检查路径是否有效
     */
    fun isValidPath(path: Path): Boolean {
        return try {
            // 尝试创建目录来验证路径有效性
            if (!fileSystem.exists(path)) {
                fileSystem.createDirectories(path)
                fileSystem.delete(path) // 立即删除测试目录
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
