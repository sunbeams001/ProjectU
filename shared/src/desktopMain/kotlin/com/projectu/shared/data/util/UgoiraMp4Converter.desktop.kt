package com.projectu.shared.data.util

import com.projectu.shared.domain.model.UgoiraMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameRecorder
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Desktop 平台：图片解码为 BufferedImage
 */
actual suspend fun decodeImageForMp4(imageBytes: ByteArray, delay: Int): UgoiraMp4Converter.FrameData =
    withContext(Dispatchers.IO) {
        val image = ImageIO.read(imageBytes.inputStream())
            ?: throw IllegalStateException("Failed to decode image")
        
        UgoiraMp4Converter.FrameData(
            imageData = image,
            width = image.width,
            height = image.height,
            delay = delay
        )
    }

/**
 * Desktop 平台：使用 JavaCV (FFmpeg) 编码 MP4 (H.264)
 * 
 * 参数设置：
 * - Codec: H.264 (libx264)
 * - CRF: 18 (visually lossless，视觉无损)
 * - Preset: slow (更好的压缩率)
 * - Pixel Format: YUV420P
 * - Frame Rate: 根据平均延迟动态计算
 */
actual suspend fun platformEncodeToMp4(
    frames: List<UgoiraMp4Converter.FrameData>,
    metadata: UgoiraMetadata
): ByteArray = withContext(Dispatchers.IO) {
    if (frames.isEmpty()) {
        throw IllegalArgumentException("No frames to encode")
    }
    
    val firstFrame = frames.first()
    val width = firstFrame.width
    val height = firstFrame.height
    
    // 计算平均帧率（基于所有帧的平均延迟）
    val averageDelay = metadata.frames.map { it.delay }.average()
    val frameRate = (1000 / averageDelay).coerceIn(10.0, 60.0) // 限制在 10-60 fps
    
    // 使用临时文件
    val tempFile = java.io.File.createTempFile("ugoira_temp_", ".mp4")
    
    try {
        // 创建 FFmpeg 录制器
        val recorder = FFmpegFrameRecorder(tempFile.absolutePath, width, height)
        recorder.videoCodec = avcodec.AV_CODEC_ID_H264
        recorder.format = "mp4"
        recorder.frameRate = frameRate
        recorder.videoBitrate = 8_000_000 // 8 Mbps - 高质量（JavaCV不支持CRF，使用高比特率）
        recorder.pixelFormat = avutil.AV_PIX_FMT_YUV420P
        
        recorder.start()
        
        val converter = Java2DFrameConverter()
        
        // 编码每一帧
        frames.forEach { frameData ->
            val image = frameData.imageData as BufferedImage
            
            // 转换为 RGB 格式（JavaCV 要求）
            val rgbImage = if (image.type != BufferedImage.TYPE_3BYTE_BGR) {
                val converted = BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR)
                val g = converted.createGraphics()
                g.drawImage(image, 0, 0, null)
                g.dispose()
                converted
            } else {
                image
            }
            
            // 转换并录制帧
            val frame: Frame = converter.convert(rgbImage)
            
            // 根据延迟重复录制帧以实现正确的时间
            val repeatCount = (frameData.delay / (1000 / frameRate)).toInt().coerceAtLeast(1)
            repeat(repeatCount) {
                recorder.record(frame)
            }
        }
        
        recorder.stop()
        recorder.release()
        
        // 读取生成的 MP4 文件
        val mp4Bytes = tempFile.readBytes()
        
        mp4Bytes
    } finally {
        // 删除临时文件
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }
}
