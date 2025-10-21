package com.projectu.shared.data.cache

import com.projectu.shared.domain.model.UgoiraMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * Ugoira动图缓存管理器
 */
class UgoiraCache(
    private val fileSystem: FileSystem,
    private val cacheDir: Path
) {
    
    /**
     * 获取作品的Ugoira缓存目录
     */
    fun getArtworkCacheDir(artworkId: String): Path {
        return cacheDir / "ugoira" / artworkId
    }
    
    /**
     * 检查Ugoira是否已缓存
     */
    fun isCached(artworkId: String): Boolean {
        val cacheDir = getArtworkCacheDir(artworkId)
        return fileSystem.exists(cacheDir) && 
               fileSystem.list(cacheDir).isNotEmpty()
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
        
        // TODO: 使用平台特定的ZIP解压实现
        // 这里需要根据平台使用不同的解压方式
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
 * 平台特定的ZIP解压实现
 */
expect suspend fun extractZipPlatform(zipPath: Path, targetDir: Path)

