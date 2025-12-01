package com.projectu.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import com.projectu.shared.data.cache.UgoiraCache
import com.projectu.shared.domain.model.UgoiraMetadata
import com.projectu.shared.domain.repository.ArtworkRepository
import com.projectu.util.PlatformImageLoader
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Ugoira 加载状态（扩展版）
 */
sealed interface UgoiraLoadingState {
    data object Idle : UgoiraLoadingState
    data object FetchingMetadata : UgoiraLoadingState
    data class Downloading(val progress: Float) : UgoiraLoadingState
    data object Extracting : UgoiraLoadingState
    data class LoadingFrames(val current: Int, val total: Int) : UgoiraLoadingState
    data class Ready(val metadata: UgoiraMetadata, val frames: List<ImageBitmap>) : UgoiraLoadingState
    data class Error(val message: String) : UgoiraLoadingState
}

/**
 * Ugoira 加载管理器
 * 
 * 负责完整的 Ugoira 加载流程：
 * 1. 检查缓存（元数据 + 帧图片）
 * 2. 获取元数据（优先从缓存）
 * 3. 下载 ZIP 文件（如果没有缓存）
 * 4. 解压帧（如果没有缓存）
 * 5. 加载 ImageBitmap
 * 
 * 缓存策略：
 * - 元数据：缓存到本地 JSON 文件
 * - ZIP 文件：下载后保存
 * - 解压后的帧：保存到独立目录
 * - 有完整缓存时直接加载帧图片，跳过下载和解压
 */
class UgoiraLoaderManager(
    private val artworkRepository: ArtworkRepository,
    private val ugoiraCache: UgoiraCache,
    private val httpClient: HttpClient
) {
    private val _loadingState = MutableStateFlow<UgoiraLoadingState>(UgoiraLoadingState.Idle)
    val loadingState: StateFlow<UgoiraLoadingState> = _loadingState.asStateFlow()
    
    private var currentArtworkId: String? = null
    
    /**
     * 加载 Ugoira 动图
     * 
     * @param artworkId 作品ID
     */
    suspend fun load(artworkId: String) {
        if (currentArtworkId == artworkId && _loadingState.value is UgoiraLoadingState.Ready) {
            // 已加载，无需重复
            return
        }
        
        currentArtworkId = artworkId
        _loadingState.value = UgoiraLoadingState.Idle
        
        try {
            val artworkIdLong = artworkId.toLongOrNull()
                ?: throw IllegalArgumentException("无效的作品ID")
            
            // 1. 检查是否有完整缓存（元数据 + 帧图片）
            val hasCachedFrames = ugoiraCache.isCached(artworkId)
            val hasCachedMetadata = ugoiraCache.hasMetadataCached(artworkId)
            
            if (hasCachedFrames && hasCachedMetadata) {
                // 完整缓存命中，直接加载
                val cachedMetadata = ugoiraCache.loadMetadata(artworkId)
                if (cachedMetadata != null) {
                    val cachedFrames = ugoiraCache.getCachedFrames(artworkId)
                    val frames = loadFramesFromPaths(cachedFrames, cachedMetadata)
                    if (frames.isNotEmpty()) {
                        _loadingState.value = UgoiraLoadingState.Ready(cachedMetadata, frames)
                        return
                    }
                }
            }
            
            // 2. 获取元数据（优先从缓存）
            _loadingState.value = UgoiraLoadingState.FetchingMetadata
            val metadata = if (hasCachedMetadata) {
                ugoiraCache.loadMetadata(artworkId) 
                    ?: artworkRepository.getUgoiraMetadata(artworkIdLong).getOrThrow().also {
                        ugoiraCache.saveMetadata(artworkId, it)
                    }
            } else {
                artworkRepository.getUgoiraMetadata(artworkIdLong).getOrThrow().also {
                    ugoiraCache.saveMetadata(artworkId, it)
                }
            }
            
            // 3. 检查是否有缓存的帧图片
            if (hasCachedFrames) {
                val cachedFrames = ugoiraCache.getCachedFrames(artworkId)
                val frames = loadFramesFromPaths(cachedFrames, metadata)
                if (frames.isNotEmpty()) {
                    _loadingState.value = UgoiraLoadingState.Ready(metadata, frames)
                    return
                }
            }
            
            // 4. 下载 ZIP
            _loadingState.value = UgoiraLoadingState.Downloading(0f)
            val zipData = downloadZip(metadata.zipUrl)
            _loadingState.value = UgoiraLoadingState.Downloading(1f)
            
            // 5. 保存并解压
            _loadingState.value = UgoiraLoadingState.Extracting
            val zipPath = ugoiraCache.saveZipFile(artworkId, zipData)
            val framePaths = ugoiraCache.extractZipToFrames(artworkId, zipPath)
            
            // 6. 加载帧图片
            val frames = loadFramesFromPaths(framePaths, metadata)
            
            if (frames.isEmpty()) {
                _loadingState.value = UgoiraLoadingState.Error("无法加载帧图片")
                return
            }
            
            _loadingState.value = UgoiraLoadingState.Ready(metadata, frames)
            
        } catch (e: Exception) {
            e.printStackTrace()
            _loadingState.value = UgoiraLoadingState.Error(e.message ?: "加载失败")
        }
    }
    
    /**
     * 重试加载
     */
    suspend fun retry() {
        currentArtworkId?.let { artworkId ->
            // 清除缓存
            ugoiraCache.clearCache(artworkId)
            load(artworkId)
        }
    }
    
    /**
     * 重置状态
     */
    fun reset() {
        currentArtworkId = null
        _loadingState.value = UgoiraLoadingState.Idle
    }
    
    /**
     * 下载 ZIP 文件
     */
    private suspend fun downloadZip(url: String): ByteArray = withContext(Dispatchers.IO) {
        val response = httpClient.get(url) {
            header("Referer", "https://www.pixiv.net/")
        }
        response.readRawBytes()
    }
    
    /**
     * 从路径列表加载帧图片
     */
    private suspend fun loadFramesFromPaths(
        framePaths: List<okio.Path>,
        metadata: UgoiraMetadata
    ): List<ImageBitmap> = withContext(Dispatchers.IO) {
        val frames = mutableListOf<ImageBitmap>()
        val total = metadata.frames.size
        
        // 根据 metadata 中的帧顺序加载
        metadata.frames.forEachIndexed { index, ugoiraFrame ->
            _loadingState.value = UgoiraLoadingState.LoadingFrames(index + 1, total)
            
            val framePath = framePaths.find { it.name == ugoiraFrame.file }
            if (framePath != null) {
                val bitmap = PlatformImageLoader.loadImageBitmapFromFile(framePath.toString())
                if (bitmap != null) {
                    frames.add(bitmap)
                }
            }
        }
        
        frames
    }
}
