package com.projectu.shared.data.cache

import com.projectu.shared.domain.model.UgoiraFrame
import com.projectu.shared.domain.model.UgoiraMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * Ugoira动图缓存管理器
 * 
 * 缓存内容：
 * - 元数据（metadata.json）
 * - ZIP 文件（{artworkId}.zip）
 * - 解压后的帧图片（{artworkId}/目录下）
 */
class UgoiraCache(
    private val fileSystem: FileSystem,
    private val cacheDir: Path
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * 获取作品的Ugoira缓存目录
     */
    fun getArtworkCacheDir(artworkId: String): Path {
        return cacheDir / "ugoira" / artworkId
    }
    
    /**
     * 检查Ugoira帧是否已缓存
     */
    fun isCached(artworkId: String): Boolean {
        val cacheDir = getArtworkCacheDir(artworkId)
        return fileSystem.exists(cacheDir) && 
               fileSystem.list(cacheDir).isNotEmpty()
    }
    
    /**
     * 检查元数据是否已缓存
     */
    fun hasMetadataCached(artworkId: String): Boolean {
        val metadataPath = cacheDir / "ugoira" / "${artworkId}_meta.json"
        return fileSystem.exists(metadataPath)
    }
    
    /**
     * 保存元数据到缓存
     */
    suspend fun saveMetadata(artworkId: String, metadata: UgoiraMetadata) = withContext(Dispatchers.IO) {
        val metadataPath = cacheDir / "ugoira" / "${artworkId}_meta.json"
        fileSystem.createDirectories(metadataPath.parent!!)
        
        // 序列化为 JSON
        val metadataJson = json.encodeToString(UgoiraMetadataCache(
            zipUrl = metadata.zipUrl,
            frames = metadata.frames.map { FrameCache(it.file, it.delay) }
        ))
        
        fileSystem.write(metadataPath) {
            writeUtf8(metadataJson)
        }
    }
    
    /**
     * 从缓存加载元数据
     */
    suspend fun loadMetadata(artworkId: String): UgoiraMetadata? = withContext(Dispatchers.IO) {
        val metadataPath = cacheDir / "ugoira" / "${artworkId}_meta.json"
        if (!fileSystem.exists(metadataPath)) {
            return@withContext null
        }
        
        try {
            val metadataJson = fileSystem.read(metadataPath) {
                readUtf8()
            }
            val cache = json.decodeFromString<UgoiraMetadataCache>(metadataJson)
            UgoiraMetadata(
                zipUrl = cache.zipUrl,
                frames = cache.frames.map { UgoiraFrame(it.file, it.delay) }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 获取缓存的帧文件列表
     */
    fun getCachedFrames(artworkId: String): List<Path> {
        val cacheDir = getArtworkCacheDir(artworkId)
        if (!fileSystem.exists(cacheDir)) {
            return emptyList()
        }
        
        return fileSystem.list(cacheDir)
            .filter { it.name.endsWith(".jpg") || it.name.endsWith(".png") }
            .sortedBy { it.name }
    }
    
    /**
     * 保存ZIP文件
     */
    suspend fun saveZipFile(artworkId: String, data: ByteArray): Path = withContext(Dispatchers.IO) {
        val zipPath = cacheDir / "ugoira" / "$artworkId.zip"
        fileSystem.createDirectories(zipPath.parent!!)
        fileSystem.write(zipPath) {
            write(data)
        }
        zipPath
    }
    
    /**
     * 解压ZIP文件到帧目录
     */
    suspend fun extractZipToFrames(artworkId: String, zipPath: Path): List<Path> = withContext(Dispatchers.IO) {
        val framesDir = getArtworkCacheDir(artworkId)
        fileSystem.createDirectories(framesDir)
        
        // 使用平台特定的ZIP解压实现
        extractZipPlatform(zipPath, framesDir)
        
        getCachedFrames(artworkId)
    }
    
    /**
     * 清除特定作品的缓存
     */
    fun clearCache(artworkId: String) {
        val cacheDir = getArtworkCacheDir(artworkId)
        if (fileSystem.exists(cacheDir)) {
            fileSystem.deleteRecursively(cacheDir)
        }
        
        val zipFile = cacheDir.parent!! / "$artworkId.zip"
        if (fileSystem.exists(zipFile)) {
            fileSystem.delete(zipFile)
        }
        
        val metadataFile = cacheDir.parent!! / "${artworkId}_meta.json"
        if (fileSystem.exists(metadataFile)) {
            fileSystem.delete(metadataFile)
        }
    }
    
    /**
     * 清除所有Ugoira缓存
     */
    fun clearAllCache() {
        val ugoiraDir = cacheDir / "ugoira"
        if (fileSystem.exists(ugoiraDir)) {
            fileSystem.deleteRecursively(ugoiraDir)
        }
    }
    
    /**
     * 获取缓存大小（字节）
     */
    fun getCacheSize(): Long {
        val ugoiraDir = cacheDir / "ugoira"
        if (!fileSystem.exists(ugoiraDir)) {
            return 0L
        }
        
        var totalSize = 0L
        fileSystem.listRecursively(ugoiraDir).forEach { path ->
            if (fileSystem.metadata(path).isRegularFile) {
                totalSize += fileSystem.metadata(path).size ?: 0L
            }
        }
        return totalSize
    }
}

/**
 * 元数据缓存数据类
 */
@kotlinx.serialization.Serializable
private data class UgoiraMetadataCache(
    val zipUrl: String,
    val frames: List<FrameCache>
)

@kotlinx.serialization.Serializable
private data class FrameCache(
    val file: String,
    val delay: Int
)

/**
 * 平台特定的ZIP解压实现
 */
expect suspend fun extractZipPlatform(zipPath: Path, targetDir: Path)

