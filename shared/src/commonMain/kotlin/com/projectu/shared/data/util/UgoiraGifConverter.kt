package com.projectu.shared.data.util

import com.projectu.shared.data.cache.UgoiraCache
import com.projectu.shared.domain.model.UgoiraMetadata
import com.shakster.gifkt.GifEncoder
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import kotlin.time.Duration.Companion.milliseconds

/**
 * Ugoira 转 GIF 转换器
 * 
 * 功能：
 * 1. 检查并使用已缓存的帧图片
 * 2. 如需要则下载 Ugoira ZIP 包
 * 3. 解压并提取帧图片（通过 UgoiraCache）
 * 4. 使用 gif.kt 编码为 GIF
 * 
 * 使用示例：
 * ```kotlin
 * val converter = UgoiraGifConverter(httpClient, fileSystem, ugoiraCache)
 * converter.convertToGif(
 *     artworkId = "123456789",
 *     metadata = ugoiraMetadata,
 *     outputPath = "output.gif".toPath(),
 *     onProgress = { current, total -> println("$current / $total") }
 * )
 * ```
 */
class UgoiraGifConverter(
    private val httpClient: HttpClient,
    private val fileSystem: FileSystem,
    val ugoiraCache: UgoiraCache // 公开以便 DownloadManager 访问
) {
    
    /**
     * 将 Ugoira 转换为 GIF 字节数组
     * 
     * @param artworkId 作品ID（用于查找缓存）
     * @param metadata Ugoira 元数据（包含 ZIP URL 和帧信息）
     * @param onProgress 进度回调 (当前步骤, 总步骤)
     * @return GIF 文件的字节数组
     * @throws Exception 如果下载、解压或编码失败
     */
    suspend fun convertToGif(
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
        
        // 步骤 3: 编码为 GIF
        onProgress?.invoke(++currentStep, totalSteps)
        return@withContext encodeToGif(frames, metadata)
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
            decodeImageToArgb(imageBytes, frame.delay)
        }
    }
    
    /**
     * 使用 gif.kt 将帧编码为 GIF
     * 
     * @param frames 帧数据列表
     * @param metadata Ugoira 元数据
     * @return GIF 文件的字节数组
     */
    private suspend fun encodeToGif(
        frames: List<FrameData>,
        metadata: UgoiraMetadata
    ): ByteArray = withContext(Dispatchers.IO) {
        // 使用 kotlinx.io.Buffer 来创建 GIF（gif.kt 使用 kotlinx.io）
        val buffer = Buffer()
        
        // 创建 GIF 编码器
        val encoder = GifEncoder(buffer)
        
        // 写入所有帧
        frames.forEach { frame ->
            encoder.writeFrame(
                argb = frame.argb,
                width = frame.width,
                height = frame.height,
                duration = frame.delay.milliseconds
            )
        }
        
        // 关闭编码器
        encoder.close()
        
        // 返回 GIF 字节数组
        return@withContext buffer.readByteArray()
    }
    
    /**
     * 帧数据封装类
     */
    data class FrameData(
        val argb: IntArray,
        val width: Int,
        val height: Int,
        val delay: Int // 毫秒
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is FrameData) return false
            
            if (!argb.contentEquals(other.argb)) return false
            if (width != other.width) return false
            if (height != other.height) return false
            if (delay != other.delay) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = argb.contentHashCode()
            result = 31 * result + width
            result = 31 * result + height
            result = 31 * result + delay
            return result
        }
    }
}

/**
 * 平台特定的图片解码实现
 * 将图片字节数组解码为 ARGB 数组
 * 
 * @param imageBytes 图片文件的字节数组
 * @param delay 帧延迟（毫秒）
 * @return 解码后的帧数据
 */
expect suspend fun decodeImageToArgb(imageBytes: ByteArray, delay: Int): UgoiraGifConverter.FrameData
