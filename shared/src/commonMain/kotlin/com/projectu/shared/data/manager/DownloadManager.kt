package com.projectu.shared.data.manager

import com.projectu.shared.data.cache.DownloadRulesCache
import com.projectu.shared.data.local.SettingsCache
import com.projectu.shared.data.local.dao.DownloadDao
import com.projectu.shared.data.local.entity.toDownloadTask
import com.projectu.shared.data.local.entity.toEntity
import com.projectu.shared.data.remote.api.PixivApi
import com.projectu.shared.data.remote.api.NovelApi
import com.projectu.shared.data.remote.api.NovelSeriesApi
import com.projectu.shared.data.remote.mapper.toNovel
import com.projectu.shared.data.remote.mapper.toNovelSeries
import com.projectu.shared.data.remote.mapper.toUgoiraMetadata
import com.projectu.shared.data.util.DownloadPathBuilder
import com.projectu.shared.data.util.EpubBuilder
import com.projectu.shared.data.util.NovelToEpubConverter
import com.projectu.shared.data.util.PlatformFileWriter
import com.projectu.shared.data.util.UgoiraGifConverter
import com.projectu.shared.data.util.UgoiraMp4Converter
import com.projectu.shared.data.local.UgoiraFormat
import com.projectu.shared.domain.model.DownloadStatus
import com.projectu.shared.domain.model.DownloadTask
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.ResourceType
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import java.util.UUID

/**
 * 缓存文件提供器接口
 * 用于从UI层的图片缓存获取已缓存的文件
 */
interface CachedFileProvider {
    /**
     * 获取已缓存的图片文件路径
     * @param illustId 作品ID
     * @param pageIndex 页码索引
     * @return 缓存文件路径，如果不存在返回null
     */
    fun getCachedImageFile(illustId: String, pageIndex: Int?): Path?
}

/**
 * 下载管理器
 * 负责下载任务的创建、执行、暂停、删除等核心业务逻辑
 * 
 * 性能优化说明：
 * - 使用 SettingsCache 而非 Flow<DownloadSettings> 获取配置
 * - 在大量下载场景下（批量下载50+作品），每个任务都需要获取路径配置
 * - SettingsCache 提供内存缓存的同步访问，避免每次都查询数据库
 * - 使用 DownloadRulesCache 匹配下载规则，避免每次查询数据库
 */
class DownloadManager(
    private val pixivApi: PixivApi,
    private val novelApi: NovelApi,
    private val novelSeriesApi: NovelSeriesApi,
    private val downloadDao: DownloadDao,
    private val pathBuilder: DownloadPathBuilder,
    private val fileSystem: FileSystem,
    private val platformFileWriter: PlatformFileWriter,
    private val httpClient: HttpClient,
    private val cachedFileProvider: CachedFileProvider?,
    private val settingsCache: SettingsCache,
    private val downloadRulesCache: DownloadRulesCache,
    private val ugoiraGifConverter: UgoiraGifConverter,
    private val ugoiraMp4Converter: UgoiraMp4Converter,
    private val ageLimitDeterminer: com.projectu.shared.util.AgeLimitDeterminer,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    
    // 活动下载任务的Job映射
    private val activeDownloads = mutableMapOf<String, Job>()
    
    // 最大并发下载数
    private val maxConcurrentDownloads = 3
    
    // 小说内容转换器（懒加载）
    private val novelConverter by lazy {
        NovelToEpubConverter(httpClient, settingsCache)
    }
    
    /**
     * 获取所有下载任务流
     */
    fun getAllTasks(): Flow<List<DownloadTask>> {
        return downloadDao.getAllTasks().map { entities ->
            entities.map { it.toDownloadTask() }
        }
    }
    
    /**
     * 从缓存获取当前下载设置（同步方法，使用内存缓存）
     * 
     * 性能说明：
     * - 直接从 SettingsCache 读取内存缓存，避免数据库查询
     * - 在大量下载场景下显著提升性能
     */
    private fun getCurrentDownloadSettings(): com.projectu.shared.data.local.DownloadSettings {
        return com.projectu.shared.data.local.DownloadSettings(
            baseDownloadPath = settingsCache.getBaseDownloadPath()
        )
    }
    
    /**
     * 获取指定状态的下载任务流
     */
    fun getTasksByStatus(status: com.projectu.shared.domain.model.DownloadStatus): Flow<List<DownloadTask>> {
        return downloadDao.getTasksByStatus(status.name).map { entities ->
            entities.map { it.toDownloadTask() }
        }
    }
    
    /**
     * 添加插画/漫画下载任务（从Artwork对象，避免重复请求）
     * @param artwork 作品对象
     * @param pageIndex 页码索引（null表示下载所有页）
     */
    suspend fun addIllustrationDownloadTask(
        artwork: com.projectu.shared.domain.model.Artwork,
        pageIndex: Int? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 从Artwork对象直接提取信息，无需API请求
            val resourceType = when {
                artwork.pageCount > 1 -> ResourceType.MANGA
                artwork.type == com.projectu.shared.domain.model.ArtworkType.UGOIRA -> ResourceType.UGOIRA
                else -> ResourceType.ILLUSTRATION
            }
            
            // 如果是下载所有页，创建多个任务
            if (pageIndex == null && artwork.pageCount > 1) {
                val taskIds = mutableListOf<String>()
                for (i in 0 until artwork.pageCount) {
                    // 获取对应页的缩略图 URL
                    val pageThumbnail = artwork.imageUrls.pages.getOrNull(i)?.urls?.squareMedium
                    
                    val taskId = createDownloadTask(
                        resourceType = resourceType,
                        resourceId = artwork.id,
                        title = artwork.title,
                        authorId = artwork.userId,
                        authorName = artwork.userName,
                        pageIndex = i,
                        totalPages = artwork.pageCount,
                        isR18 = artwork.ageLimit == com.projectu.shared.domain.model.AgeLimit.R18 || 
                                artwork.ageLimit == com.projectu.shared.domain.model.AgeLimit.R18G,
                        isAi = artwork.isAiGenerated,
                        tags = artwork.tags.map { it.name },
                        publishTime = System.currentTimeMillis(),
                        thumbnailUrl = pageThumbnail
                    )
                    taskIds.add(taskId)
                }
                
                // 自动开始第一个任务
                taskIds.firstOrNull()?.let { startDownload(it) }
                
                return@withContext Result.success(taskIds.first())
            } else {
                // 获取缩略图 URL
                val thumbnailUrl = if (pageIndex != null) {
                    artwork.imageUrls.pages.getOrNull(pageIndex)?.urls?.squareMedium
                } else {
                    artwork.imageUrls.pages.firstOrNull()?.urls?.squareMedium
                }
                
                // 创建单个下载任务
                val taskId = createDownloadTask(
                    resourceType = resourceType,
                    resourceId = artwork.id,
                    title = artwork.title,
                    authorId = artwork.userId,
                    authorName = artwork.userName,
                    pageIndex = pageIndex,
                    totalPages = artwork.pageCount,
                    isR18 = artwork.ageLimit == com.projectu.shared.domain.model.AgeLimit.R18 || 
                            artwork.ageLimit == com.projectu.shared.domain.model.AgeLimit.R18G,
                    isAi = artwork.isAiGenerated,
                    tags = artwork.tags.map { it.name },
                    publishTime = System.currentTimeMillis(),
                    thumbnailUrl = thumbnailUrl
                )
                
                // 自动开始下载
                startDownload(taskId)
                
                return@withContext Result.success(taskId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * 添加插画/漫画下载任务（从作品ID，会发起API请求）
     * @param illustId 作品ID
     * @param pageIndex 页码索引（null表示下载所有页）
     */
    suspend fun addIllustrationDownloadTask(
        illustId: Long,
        pageIndex: Int? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. 获取作品详情
            val illustResponse = pixivApi.illustApi.getDetail(illustId)
            if (illustResponse.error) {
                return@withContext Result.failure(Exception(illustResponse.message ?: "Failed to get illust detail"))
            }
            val illust = illustResponse.body ?: throw IllegalStateException("Response body is null")
            
            // 2. 确定资源类型
            val resourceType = when {
                illust.pageCount > 1 -> ResourceType.MANGA
                illust.illustType == 2 -> ResourceType.UGOIRA
                else -> ResourceType.ILLUSTRATION
            }
            
            // 3. 如果是下载所有页，创建多个任务
            if (pageIndex == null && illust.pageCount > 1) {
                val taskIds = mutableListOf<String>()
                for (i in 0 until illust.pageCount) {
                    val taskId = createDownloadTask(
                        resourceType = resourceType,
                        resourceId = illustId.toString(),
                        title = illust.title,
                        authorId = illust.userId.toString(),
                        authorName = illust.userName,
                        pageIndex = i,
                        totalPages = illust.pageCount,
                        isR18 = illust.xRestrict >= 1,
                        isAi = illust.aiType == 2,
                        tags = illust.tags.tags.toList().map { it.tag },
                        publishTime = System.currentTimeMillis() // 使用当前时间作为默认值
                    )
                    taskIds.add(taskId)
                }
                
                // 自动开始第一个任务
                taskIds.firstOrNull()?.let { startDownload(it) }
                
                return@withContext Result.success(taskIds.first())
            } else {
                // 4. 创建单个下载任务
                val taskId = createDownloadTask(
                    resourceType = resourceType,
                    resourceId = illustId.toString(),
                    title = illust.title,
                    authorId = illust.userId.toString(),
                    authorName = illust.userName,
                    pageIndex = pageIndex,
                    totalPages = illust.pageCount,
                    isR18 = illust.xRestrict >= 1,
                    isAi = illust.aiType == 2,
                    tags = illust.tags.tags.toList().map { it.tag },
                    publishTime = System.currentTimeMillis()
                )
                
                // 5. 自动开始下载
                startDownload(taskId)
                
                return@withContext Result.success(taskId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * 创建下载任务
     */
    private suspend fun createDownloadTask(
        resourceType: ResourceType,
        resourceId: String,
        title: String,
        authorId: String,
        authorName: String,
        pageIndex: Int?,
        totalPages: Int,
        isR18: Boolean,
        isAi: Boolean,
        tags: List<String>,
        publishTime: Long,
        thumbnailUrl: String? = null,
        customFileExtension: String? = null
    ): String {
        val settings = getCurrentDownloadSettings()
        val taskId = UUID.randomUUID().toString()
        
        // 创建一个临时task用于路径生成
        val tempTask = DownloadTask(
            id = taskId,
            resourceType = resourceType,
            resourceId = resourceId,
            title = title,
            authorId = authorId,
            authorName = authorName,
            pageIndex = pageIndex,
            totalPages = totalPages,
            isR18 = isR18,
            isAi = isAi,
            tags = tags,
            thumbnailUrl = thumbnailUrl,
            publishTime = publishTime,
            downloadTime = System.currentTimeMillis(),
            status = DownloadStatus.PENDING,
            progress = 0f,
            targetPath = "", // 暂时为空
            fileName = "" // 暂时为空
        )
        
        val targetPath = pathBuilder.buildPath(tempTask, settings)
        val fileExtension = customFileExtension ?: getExtension(resourceType)
        val fileName = pathBuilder.buildFileName(tempTask, settings, fileExtension)
        val fullPath = targetPath / fileName
        
        // 检查文件是否已存在，如果存在则直接标记为已完成，避免重复下载
        val (initialStatus, initialProgress) = if (fileSystem.exists(fullPath)) {
            DownloadStatus.COMPLETED to 1f
        } else {
            DownloadStatus.PENDING to 0f
        }
        
        val task = tempTask.copy(
            targetPath = targetPath.toString(),
            fileName = fileName,
            status = initialStatus,
            progress = initialProgress
        )
        
        // 保存到数据库
        downloadDao.upsertTask(task.toEntity())
        
        return taskId
    }
    
    /**
     * 开始/恢复下载
     */
    suspend fun startDownload(taskId: String) {
        val task = downloadDao.getTask(taskId)?.toDownloadTask() ?: return
        
        // 检查文件是否真实存在，如果不存在但状态为已完成，则重置为等待下载
        if (task.status == DownloadStatus.COMPLETED) {
            val fullPath = task.targetPath.toPath() / task.fileName
            if (!fileSystem.exists(fullPath)) {
                // 文件已被外部删除，重置任务状态
                downloadDao.updateTaskStatus(taskId, DownloadStatus.PENDING.name, 0f)
            } else {
                // 文件已存在，不需要重新下载
                return
            }
        }
        
        // 检查并发限制
        while (downloadDao.getDownloadingTaskCount() >= maxConcurrentDownloads) {
            delay(1000) // 等待其他任务完成
        }
        
        // 如果已经在下载中，不重复启动
        if (activeDownloads.containsKey(taskId)) {
            return
        }
        
        val job = coroutineScope.launch {
            try {
                downloadDao.updateTaskStatus(taskId, DownloadStatus.DOWNLOADING.name, 0f)
                
                when (task.resourceType) {
                    ResourceType.ILLUSTRATION, ResourceType.MANGA -> downloadIllustration(task)
                    ResourceType.UGOIRA -> downloadUgoira(task)
                    ResourceType.NOVEL -> downloadNovel(task)
                    ResourceType.NOVEL_SERIES -> downloadNovelSeries(task)
                }
                
                downloadDao.updateTaskStatus(taskId, DownloadStatus.COMPLETED.name, 1.0f)
            } catch (e: kotlinx.coroutines.CancellationException) {
                downloadDao.updateTaskStatus(taskId, DownloadStatus.PAUSED.name)
            } catch (e: Exception) {
                e.printStackTrace()
                downloadDao.updateTaskStatus(
                    taskId, 
                    DownloadStatus.FAILED.name, 
                    error = e.message ?: "Unknown error"
                )
            } finally {
                activeDownloads.remove(taskId)
            }
        }
        
        activeDownloads[taskId] = job
    }
    
    /**
     * 暂停下载
     */
    fun pauseDownload(taskId: String) {
        activeDownloads[taskId]?.cancel()
        activeDownloads.remove(taskId)
    }
    
    /**
     * 删除任务
     */
    suspend fun deleteTask(taskId: String, deleteFile: Boolean = false) {
        pauseDownload(taskId)
        
        if (deleteFile) {
            val task = downloadDao.getTask(taskId)?.toDownloadTask()
            task?.let {
                val filePath = it.targetPath.toPath() / it.fileName
                if (fileSystem.exists(filePath)) {
                    fileSystem.delete(filePath)
                }
            }
        }
        
        downloadDao.deleteTask(taskId)
    }
    
    /**
     * 检查资源是否已下载
     */
    suspend fun isDownloaded(
        resourceType: ResourceType,
        resourceId: String,
        pageIndex: Int? = null
    ): Boolean {
        return downloadDao.isResourceDownloaded(resourceType.name, resourceId, pageIndex)
    }
    
    /**
     * 下载插画/漫画
     */
    private suspend fun downloadIllustration(task: DownloadTask) = withContext(Dispatchers.IO) {
        // 使用规则系统获取匹配的下载规则
        val rule = downloadRulesCache.findMatchingRule(task)
        
        // 从规则获取基础路径和相对路径
        val baseDownloadPath = rule.targetPath
        val relativePath = rule.buildRelativePath(task)
        val fileName = task.fileName
        
        // 确保目录存在
        platformFileWriter.ensureDirectoryExists(baseDownloadPath, relativePath)
        
        // 1. 尝试从缓存获取（如果提供了缓存提供器）
        val cachedFile = cachedFileProvider?.getCachedImageFile(task.resourceId, task.pageIndex)
        if (cachedFile != null && fileSystem.exists(cachedFile)) {
            // 从缓存复制到目标位置
            val sink = platformFileWriter.createSinkFromUri(baseDownloadPath, relativePath, fileName)
            sink.buffer().use { bufferedSink ->
                fileSystem.source(cachedFile).buffer().use { source ->
                    bufferedSink.writeAll(source)
                }
            }
            return@withContext
        }
        
        // 2. 获取原图URL
        val imageUrl = getOriginalImageUrl(task.resourceId.toLong(), task.pageIndex ?: 0)
        
        // 3. 直接下载到目标位置
        downloadFileWithProgressFromUri(imageUrl, baseDownloadPath, relativePath, fileName, task.id)
    }
    
    /**
     * 下载Ugoira并转换为指定格式（GIF或MP4）
     */
    private suspend fun downloadUgoira(task: DownloadTask) = withContext(Dispatchers.IO) {
        // 1. 尝试从缓存加载元数据，如果不存在则从 API 获取
        val metadata = ugoiraGifConverter.ugoiraCache.loadMetadata(task.resourceId)
            ?: run {
                // 从 API 获取元数据
                val metadataResult = pixivApi.illustApi.getUgoiraMeta(task.resourceId.toLong())
                if (metadataResult.error) {
                    throw Exception(metadataResult.message ?: "Failed to get Ugoira metadata")
                }
                val metadataBody = metadataResult.body 
                    ?: throw IllegalStateException("Ugoira metadata is null")
                
                // 转换为领域模型
                val newMetadata = metadataBody.toUgoiraMetadata()
                
                // 保存到缓存以便下次使用
                ugoiraGifConverter.ugoiraCache.saveMetadata(task.resourceId, newMetadata)
                
                newMetadata
            }
        
        // 2. 使用规则系统获取目标路径
        val rule = downloadRulesCache.findMatchingRule(task)
        val baseDownloadPath = rule.targetPath
        val relativePath = rule.buildRelativePath(task)
        val fileName = task.fileName
        
        // 确保目录存在
        platformFileWriter.ensureDirectoryExists(baseDownloadPath, relativePath)
        
        // 3. 根据文件扩展名判断格式，调用对应的转换器
        val fileBytes = if (fileName.endsWith(".mp4", ignoreCase = true)) {
            // 使用 MP4 转换器
            ugoiraMp4Converter.convertToMp4(
                artworkId = task.resourceId,
                metadata = metadata,
                onProgress = { current, total ->
                    // 更新进度（转换过程）
                    val progress = current.toFloat() / total
                    coroutineScope.launch {
                        downloadDao.updateTaskProgress(task.id, progress, 0L)
                    }
                }
            )
        } else {
            // 默认使用 GIF 转换器
            ugoiraGifConverter.convertToGif(
                artworkId = task.resourceId,
                metadata = metadata,
                onProgress = { current, total ->
                    // 更新进度（转换过程）
                    val progress = current.toFloat() / total
                    coroutineScope.launch {
                        downloadDao.updateTaskProgress(task.id, progress, 0L)
                    }
                }
            )
        }
        
        // 4. 使用 PlatformFileWriter 写入文件（支持 Content URI）
        val sink = platformFileWriter.createSinkFromUri(baseDownloadPath, relativePath, fileName)
        sink.buffer().use { bufferedSink ->
            bufferedSink.write(fileBytes)
        }
    }
    
    /**
     * 添加Ugoira动图下载任务
     * @param artwork 作品对象（必须是UGOIRA类型）
     * @param format 下载格式（GIF或MP4），默认为GIF
     */
    suspend fun addUgoiraDownloadTask(
        artwork: com.projectu.shared.domain.model.Artwork,
        format: UgoiraFormat = UgoiraFormat.GIF
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 验证作品类型
            if (artwork.type != com.projectu.shared.domain.model.ArtworkType.UGOIRA) {
                return@withContext Result.failure(IllegalArgumentException("Artwork is not a Ugoira"))
            }
            
            // 获取缩略图 URL
            val thumbnailUrl = artwork.imageUrls.pages.firstOrNull()?.urls?.squareMedium
            
            // 根据格式确定文件扩展名
            val fileExtension = when (format) {
                UgoiraFormat.GIF -> "gif"
                UgoiraFormat.MP4 -> "mp4"
                else -> "gif" // 默认 GIF
            }
            
            // 创建下载任务（文件名模板会被应用，但需要确保扩展名正确）
            val taskId = createDownloadTask(
                resourceType = ResourceType.UGOIRA,
                resourceId = artwork.id,
                title = artwork.title,
                authorId = artwork.userId,
                authorName = artwork.userName,
                pageIndex = null,
                totalPages = 1,
                isR18 = artwork.ageLimit == com.projectu.shared.domain.model.AgeLimit.R18 || 
                        artwork.ageLimit == com.projectu.shared.domain.model.AgeLimit.R18G,
                isAi = artwork.isAiGenerated,
                tags = artwork.tags.map { it.name },
                publishTime = System.currentTimeMillis(),
                thumbnailUrl = thumbnailUrl,
                customFileExtension = fileExtension // 使用自定义扩展名
            )
            
            // 自动开始下载
            startDownload(taskId)
            
            Result.success(taskId)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * 添加小说下载任务
     * @param novelId 小说ID
     */
    suspend fun addNovelDownloadTask(
        novelId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 获取小说详情
            val response = novelApi.getDetail(novelId.toLong())
            val novelBody = response.body ?: throw Exception("Failed to fetch novel details")
            
            // 创建下载任务
            val taskId = createDownloadTask(
                resourceType = ResourceType.NOVEL,
                resourceId = novelBody.id,
                title = novelBody.title,
                authorId = novelBody.userId,
                authorName = novelBody.userName,
                pageIndex = null,
                totalPages = 1,
                isR18 = novelBody.xRestrict > 0,
                isAi = false, // 小说 DTO 没有直接的 AI 字段
                tags = novelBody.tags.tags.map { it.tag },
                publishTime = System.currentTimeMillis(),
                thumbnailUrl = novelBody.coverUrl
            )
            
            // 自动开始下载
            startDownload(taskId)
            
            Result.success(taskId)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * 添加小说系列下载任务
     * @param seriesId 系列ID
     */
    suspend fun addNovelSeriesDownloadTask(
        seriesId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 获取系列详情
            val response = novelSeriesApi.getDetail(seriesId.toLong())
            val seriesBody = response.body ?: throw Exception("Failed to fetch series details")
            
            // 创建下载任务
            val taskId = createDownloadTask(
                resourceType = ResourceType.NOVEL_SERIES,
                resourceId = seriesBody.id,
                title = seriesBody.title,
                authorId = seriesBody.userId,
                authorName = seriesBody.userName,
                pageIndex = null,
                totalPages = 1,
                isR18 = seriesBody.xRestrict > 0,
                isAi = false,
                tags = seriesBody.tags,
                publishTime = System.currentTimeMillis(),
                thumbnailUrl = seriesBody.cover?.urls?.size480mw
            )
            
            // 自动开始下载
            startDownload(taskId)
            
            Result.success(taskId)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * 下载小说为 EPUB 格式
     */
    private suspend fun downloadNovel(task: DownloadTask) = withContext(Dispatchers.IO) {
        // 1. 获取小说详情（包含完整内容）
        val novelResponse = pixivApi.novelApi.getDetail(task.resourceId.toLong())
        if (novelResponse.error) {
            throw Exception(novelResponse.message ?: "Failed to get novel detail")
        }
        val novelBody = novelResponse.body ?: throw IllegalStateException("Novel body is null")
        val novel = novelBody.toNovel(ageLimitDeterminer)
        
        // 2. 转换为 EPUB 格式
        val conversionResult = novelConverter.convert(
            novel = novel,
            downloadImages = true
        )
        
        // 3. 使用规则系统获取目标路径
        val rule = downloadRulesCache.findMatchingRule(task)
        val baseDownloadPath = rule.targetPath
        val relativePath = rule.buildRelativePath(task)
        val fileName = task.fileName
        
        // 确保目录存在
        platformFileWriter.ensureDirectoryExists(baseDownloadPath, relativePath)
        
        // 4. 创建输出 Sink（使用 PlatformFileWriter 支持 Android SAF）
        val sink = platformFileWriter.createSinkFromUri(baseDownloadPath, relativePath, fileName)
        
        // 5. 使用 buffer() 包装 Sink 并自动关闭
        sink.buffer().use { bufferedSink ->
            // 创建 EPUB 构建器
            val epubBuilder = EpubBuilder(
                fileSystem = fileSystem,
                outputSink = bufferedSink
            )
            
            // 设置元数据
            epubBuilder.setMetadata(
                EpubBuilder.Metadata(
                    title = novel.title,
                    author = novel.userName,
                    language = novel.language,
                    identifier = "pixiv-novel-${novel.id}",
                    publisher = "Pixiv",
                    description = novel.description
                )
            )
            
            // 添加章节
            conversionResult.chapters.forEach { chapter ->
                epubBuilder.addChapter(
                    EpubBuilder.Chapter(
                        id = chapter.id,
                        title = chapter.title,
                        htmlContent = chapter.htmlContent,
                        order = chapter.order
                    )
                )
            }
            
            // 添加图片
            conversionResult.images.forEach { image ->
                epubBuilder.addImage(
                    EpubBuilder.Image(
                        id = image.id,
                        fileName = image.fileName,
                        data = image.data,
                        mimeType = image.mimeType
                    )
                )
            }
            
            // 构建 EPUB
            epubBuilder.build()
        }
    }
    
    /**
     * 下载小说系列为 EPUB 格式
     */
    private suspend fun downloadNovelSeries(task: DownloadTask) = withContext(Dispatchers.IO) {
        // 1. 获取系列详情
        val seriesResponse = pixivApi.novelSeriesApi.getDetail(task.resourceId.toLong())
        if (seriesResponse.error) {
            throw Exception(seriesResponse.message ?: "Failed to get series detail")
        }
        val seriesBody = seriesResponse.body ?: throw IllegalStateException("Series body is null")
        
        // 2. 获取系列中所有章节的标题和ID
        val titlesResponse = pixivApi.novelSeriesApi.getTitles(task.resourceId.toLong())
        if (titlesResponse.error) {
            throw Exception(titlesResponse.message ?: "Failed to get series titles")
        }
        val titles = titlesResponse.body ?: throw IllegalStateException("Series titles is null")
        
        // 3. 逐个获取每个章节的详细内容
        val novels = mutableListOf<Novel>()
        for ((index, titleInfo) in titles.withIndex()) {
            if (!titleInfo.available) {
                // 跳过不可用的章节
                continue
            }
            
            val novelResponse = pixivApi.novelApi.getDetail(titleInfo.id.toLong())
            if (!novelResponse.error) {
                val novelBody = novelResponse.body
                if (novelBody != null) {
                    novels.add(novelBody.toNovel(ageLimitDeterminer))
                }
            }
            
            // 更新进度（前50%用于下载章节）
            val progress = (index + 1).toFloat() / titles.size * 0.5f
            downloadDao.updateTaskProgress(task.id, progress, 0L)
        }
        
        // 4. 转换为 EPUB 格式
        val conversionResult = novelConverter.convertSeries(
            novels = novels,
            seriesTitle = seriesBody.title,
            downloadImages = true
        )
        
        // 5. 使用规则系统获取目标路径
        val rule = downloadRulesCache.findMatchingRule(task)
        val baseDownloadPath = rule.targetPath
        val relativePath = rule.buildRelativePath(task)
        val fileName = task.fileName
        
        // 确保目录存在
        platformFileWriter.ensureDirectoryExists(baseDownloadPath, relativePath)
        
        // 6. 创建输出 Sink（使用 PlatformFileWriter 支持 Android SAF）
        val sink = platformFileWriter.createSinkFromUri(baseDownloadPath, relativePath, fileName)
        
        // 7. 使用 buffer() 包装 Sink 并自动关闭
        sink.buffer().use { bufferedSink ->
            // 创建 EPUB 构建器
            val epubBuilder = EpubBuilder(
                fileSystem = fileSystem,
                outputSink = bufferedSink
            )
            
            // 设置元数据
            epubBuilder.setMetadata(
                EpubBuilder.Metadata(
                    title = seriesBody.title,
                    author = task.authorName,
                    language = "ja",
                    identifier = "pixiv-series-${task.resourceId}",
                    publisher = "Pixiv",
                    description = seriesBody.caption
                )
            )
            
            // 添加章节
            conversionResult.chapters.forEach { chapter ->
                epubBuilder.addChapter(
                    EpubBuilder.Chapter(
                        id = chapter.id,
                        title = chapter.title,
                        htmlContent = chapter.htmlContent,
                        order = chapter.order
                    )
                )
            }
            
            // 添加图片
            conversionResult.images.forEach { image ->
                epubBuilder.addImage(
                    EpubBuilder.Image(
                        id = image.id,
                        fileName = image.fileName,
                        data = image.data,
                        mimeType = image.mimeType
                    )
                )
            }
            
            // 构建 EPUB
            downloadDao.updateTaskProgress(task.id, 0.9f, 0L)
            epubBuilder.build()
        }
    }
    
    /**
     * 获取原图URL
     */
    private suspend fun getOriginalImageUrl(illustId: Long, pageIndex: Int): String {
        val response = pixivApi.illustApi.getDetail(illustId)
        if (response.error) {
            throw Exception(response.message ?: "Failed to get illust detail")
        }
        val detail = response.body ?: throw IllegalStateException("Response body is null")
        
        return if (detail.pageCount > 1) {
            // 多图作品 - 需要使用getPages API
            val pagesResponse = pixivApi.illustApi.getPages(illustId)
            if (pagesResponse.error) {
                throw Exception(pagesResponse.message ?: "Failed to get pages")
            }
            val pages = pagesResponse.body ?: throw IllegalStateException("Pages body is null")
            pages.getOrNull(pageIndex)?.urls?.original
                ?: throw IllegalStateException("Page $pageIndex not found")
        } else {
            // 单图作品
            detail.urls.original
        }
    }
    
    /**
     * 下载文件并更新进度
     */
    private suspend fun downloadFileWithProgress(
        url: String,
        targetPath: Path,
        displayName: String,
        taskId: String
    ) = withContext(Dispatchers.IO) {
        httpClient.prepareGet(url) {
            headers.append("Referer", "https://www.pixiv.net/")
        }.execute { response ->
            val contentLength = response.contentLength() ?: -1L
            var downloadedBytes = 0L
            
            val channel = response.bodyAsChannel()
            
            // 使用平台特定的文件写入器
            val sink = platformFileWriter.createSink(targetPath, displayName)
            sink.buffer().use { bufferedSink ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        bufferedSink.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        
                        // 更新进度
                        if (contentLength > 0) {
                            val progress = downloadedBytes.toFloat() / contentLength
                            downloadDao.updateTaskProgress(taskId, progress, downloadedBytes)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 从 URI 下载文件并更新进度（支持 SAF）
     */
    private suspend fun downloadFileWithProgressFromUri(
        url: String,
        baseUri: String,
        relativePath: String,
        fileName: String,
        taskId: String
    ) = withContext(Dispatchers.IO) {
        httpClient.prepareGet(url) {
            headers.append("Referer", "https://www.pixiv.net/")
        }.execute { response ->
            val contentLength = response.contentLength() ?: -1L
            var downloadedBytes = 0L
            
            val channel = response.bodyAsChannel()
            
            // 使用平台特定的文件写入器（支持 URI）
            val sink = platformFileWriter.createSinkFromUri(baseUri, relativePath, fileName)
            sink.buffer().use { bufferedSink ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        bufferedSink.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        
                        // 更新进度
                        if (contentLength > 0) {
                            val progress = downloadedBytes.toFloat() / contentLength
                            downloadDao.updateTaskProgress(taskId, progress, downloadedBytes)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 获取文件扩展名
     */
    private fun getExtension(resourceType: ResourceType): String {
        return when (resourceType) {
            ResourceType.ILLUSTRATION, ResourceType.MANGA -> "jpg"
            ResourceType.UGOIRA -> "gif"
            ResourceType.NOVEL, ResourceType.NOVEL_SERIES -> "epub"
        }
    }
}
