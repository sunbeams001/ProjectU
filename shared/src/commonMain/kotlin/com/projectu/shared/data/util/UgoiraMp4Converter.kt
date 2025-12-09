package com.projectu.shared.data.util

import com.projectu.shared.data.cache.UgoiraCache
import com.projectu.shared.domain.model.UgoiraMetadata
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use

/**
 * Ugoira 转 MP4 转换器（H.264编码，无损质量）
 * 
 * 功能：
 * 1. 检查并使用已缓存的帧图片
 * 2. 如需要则下载 Ugoira ZIP 包
 * 3. 解压并提取帧图片（通过 UgoiraCache）
 * 4. 使用平台特定的编码器编码为 MP4 (H.264)
 * 
 * 质量参数：
 * - Android: MediaCodec with bitrate 8Mbps (near-lossless)
 * - Desktop: FFmpeg with CRF 18 (visually lossless)
 * 
 * 使用示例：
 * ```kotlin
 * val converter = UgoiraMp4Converter(httpClient, fileSystem, ugoiraCache)
 * val mp4Bytes = converter.convertToMp4(
 *     artworkId = "123456789",
 *     metadata = ugoiraMetadata,
 *     onProgress = { current, total -> println("$current / $total") }
 * )
 * ```
 */
class UgoiraMp4Converter(
    private val httpClient: HttpClient,
    private val fileSystem: FileSystem,
    val ugoiraCache: UgoiraCache // 公开以便 DownloadManager 访问
) {
    
    /**
     * 将 Ugoira 转换为 MP4 字节数组
     * 
     * @param artworkId 作品ID（用于查找缓存）
     * @param metadata Ugoira 元数据（包含 ZIP URL 和帧信息）
     * @param onProgress 进度回调 (当前步骤, 总步骤)
     * @return MP4 文件的字节数组
     * @throws Exception 如果下载、解压或编码失败
     */
    suspend fun convertToMp4(
        artworkId: String,
        metadata: UgoiraMetadata,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): ByteArray = withContext(Dispatchers.IO) {
        val totalSteps = 1 + metadata.frames.size + 1 // 检查缓存/下载 + 解压帧 + 编码
        var currentStep = 0
        
        // 步骤 1: 尝试从缓存获取帧，如果不存在则下载
        onProgress?.invoke(++currentStep, totalSteps)
        
        val cachedFramePaths = ugoiraCache.getCachedFrames(artworkId)
        val framePaths: List<Path>
        
        if (cachedFramePaths.isNotEmpty() && cachedFramePaths.size == metadata.frames.size) {
            // 使用缓存的帧
            framePaths = cachedFramePaths
        } else {
            // 需要下载和解压
            val zipData = downloadZip(metadata.zipUrl)
            val zipPath = ugoiraCache.saveZipFile(artworkId, zipData)
            framePaths = ugoiraCache.extractZipToFrames(artworkId, zipPath)
        }
        
        // 步骤 2: 从帧文件加载图片数据
        val frames = loadFramesFromCache(framePaths, metadata) { frameIndex ->
            onProgress?.invoke(currentStep + frameIndex + 1, totalSteps)
        }
        currentStep += metadata.frames.size
        
        // 步骤 3: 编码为 MP4（平台特定实现）
        onProgress?.invoke(++currentStep, totalSteps)
        return@withContext encodeToMp4(frames, metadata)
    }
    
    /**
     * 下载 ZIP 文件到内存
     */
    private suspend fun downloadZip(zipUrl: String): ByteArray = withContext(Dispatchers.IO) {
        val response: HttpResponse = httpClient.get(zipUrl) {
            headers {
                append("Referer", "https://www.pixiv.net/")
            }
        }
        response.readRawBytes()
    }
    
    /**
     * 从缓存的帧文件加载图片数据
     * 
     * @param framePaths 缓存的帧文件路径列表
     * @param metadata Ugoira 元数据（用于确定帧顺序和延迟）
     * @param onFrameLoaded 每加载一帧时的回调
     * @return 按顺序排列的帧数据列表
     */
    private suspend fun loadFramesFromCache(
        framePaths: List<Path>,
        metadata: UgoiraMetadata,
        onFrameLoaded: ((frameIndex: Int) -> Unit)? = null
    ): List<FrameData> = withContext(Dispatchers.IO) {
        // 创建文件名到路径的映射
        val framePathMap = framePaths.associateBy { it.name }
        
        // 按 metadata 中的顺序加载帧
        metadata.frames.mapIndexed { index, frame ->
            onFrameLoaded?.invoke(index)
            
            val framePath = framePathMap[frame.file]
                ?: throw IllegalStateException("Frame ${frame.file} not found in cache")
            
            // 读取文件字节
            val imageBytes = fileSystem.read(framePath) {
                readByteArray()
            }
            
            // 使用平台特定的图片解码
            decodeImageForMp4(imageBytes, frame.delay)
        }
    }
    
    /**
     * 使用平台特定的编码器将帧编码为 MP4
     * 
     * @param frames 帧数据列表
     * @param metadata Ugoira 元数据
     * @return MP4 文件的字节数组
     */
    private suspend fun encodeToMp4(
        frames: List<FrameData>,
        metadata: UgoiraMetadata
    ): ByteArray = withContext(Dispatchers.IO) {
        // 调用平台特定的编码实现
        platformEncodeToMp4(frames, metadata)
    }
    
    /**
     * 帧数据封装类
     */
    data class FrameData(
        val imageData: Any, // Android: Bitmap, Desktop: BufferedImage
        val width: Int,
        val height: Int,
        val delay: Int // 毫秒
    )
}

/**
 * 平台特定的图片解码实现
 * 将图片字节数组解码为平台特定的图片对象
 * 
 * @param imageBytes 图片文件的字节数组
 * @param delay 帧延迟（毫秒）
 * @return 解码后的帧数据
 */
expect suspend fun decodeImageForMp4(imageBytes: ByteArray, delay: Int): UgoiraMp4Converter.FrameData

/**
 * 平台特定的 MP4 编码实现
 * 
 * @param frames 帧数据列表
 * @param metadata Ugoira 元数据
 * @return MP4 文件的字节数组
 */
expect suspend fun platformEncodeToMp4(
    frames: List<UgoiraMp4Converter.FrameData>,
    metadata: UgoiraMetadata
): ByteArray
