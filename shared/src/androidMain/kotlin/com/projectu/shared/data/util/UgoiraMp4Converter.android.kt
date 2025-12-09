package com.projectu.shared.data.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import com.projectu.shared.domain.model.UgoiraMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/**
 * Android 平台：图片解码为 Bitmap
 */
actual suspend fun decodeImageForMp4(imageBytes: ByteArray, delay: Int): UgoiraMp4Converter.FrameData =
    withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: throw IllegalStateException("Failed to decode image")
        
        UgoiraMp4Converter.FrameData(
            imageData = bitmap,
            width = bitmap.width,
            height = bitmap.height,
            delay = delay
        )
    }

/**
 * Android 平台：使用 MediaCodec 编码 MP4 (H.264)
 * 使用 InputSurface + Canvas 绘制方案，支持奇数宽高的图片
 */
actual suspend fun platformEncodeToMp4(
    frames: List<UgoiraMp4Converter.FrameData>,
    metadata: UgoiraMetadata
): ByteArray = withContext(Dispatchers.IO) {
    if (frames.isEmpty()) {
        throw IllegalArgumentException("No frames to encode")
    }
    
    val firstFrame = frames.first()
    val originalWidth = firstFrame.width
    val originalHeight = firstFrame.height
    
    // 编码器使用偶数宽高（向上取偶数）
    val encoderWidth = (originalWidth + 1) and 1.inv()
    val encoderHeight = (originalHeight + 1) and 1.inv()
    
    // 计算平均帧率
    val averageDelay = metadata.frames.map { it.delay }.average()
    val frameRate = (1000 / averageDelay).toInt().coerceIn(10, 60)
    
    val tempFile = java.io.File.createTempFile("ugoira_temp_", ".mp4")
    
    var codec: MediaCodec? = null
    var muxer: MediaMuxer? = null
    var inputSurface: android.view.Surface? = null
    var outputCount = 0
    var encodingSuccessful = false
    
    try {
        // 创建 MediaMuxer
        muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        
        // 配置编码器（使用 Surface 输入，高质量参数）
        val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
        val format = MediaFormat.createVideoFormat(mimeType, encoderWidth, encoderHeight).apply {
            // 极高码率确保质量（根据分辨率动态调整，提高系数 0.15 → 0.25）
            val bitrate = (encoderWidth * encoderHeight * frameRate * 0.25).toInt().coerceIn(15_000_000, 50_000_000)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // 1秒一个I帧，提高质量和快进响应
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            
            // 使用 High Profile 获得更好的压缩效率和质量
            if (android.os.Build.VERSION.SDK_INT >= 25) {
                setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)
                setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel42) // 提升到 Level 4.2
            }
            
            // 质量优先设置
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ) // 恒定质量模式
                setInteger(MediaFormat.KEY_QUALITY, 95) // 质量参数 0-100，95 = 极高质量
            }
            
            // 颜色格式优化
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT709)
                setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_FULL)
            }
        }
        
        codec = MediaCodec.createEncoderByType(mimeType)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        
        // 创建输入 Surface
        inputSurface = codec.createInputSurface()
        codec.start()
        
        val bufferInfo = MediaCodec.BufferInfo()
        var trackIndex = -1
        var muxerStarted = false
        var outputCount = 0
        
        // 创建 Paint 用于高质量绘制
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true // 抗锯齿，提升画质
            isFilterBitmap = true // 双线性过滤
            isDither = true // 抖动处理，减少色带
        }
        
        // 累积的 presentation time（微秒）
        var presentationTimeUs = 0L
        
        // 帧时间戳队列（用于为输出帧分配正确的时间戳）
        val frameTimestamps = mutableListOf<Long>()
        
        // 处理每一帧
        for (frameIndex in frames.indices) {
            val frame = frames[frameIndex]
            val bitmap = frame.imageData as Bitmap
            
            // 1. 绘制到 Surface（无需等待）
            val canvas = inputSurface.lockCanvas(null)
            try {
                val srcRect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
                val dstRect = android.graphics.Rect(0, 0, encoderWidth, encoderHeight)
                canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
            } finally {
                inputSurface.unlockCanvasAndPost(canvas)
            }
            
            // 记录该帧的时间戳
            frameTimestamps.add(presentationTimeUs)
            
            // 2. 提取编码数据（非阻塞，提取所有可用的）
            drainEncoder(codec, muxer, bufferInfo, frameTimestamps, false, 
                trackIndex, muxerStarted, outputCount) { newTrackIndex, newMuxerStarted, newOutputCount ->
                trackIndex = newTrackIndex
                muxerStarted = newMuxerStarted
                outputCount = newOutputCount
            }
            
            // 3. 累加时间戳（下一帧的时间）
            presentationTimeUs += (frame.delay * 1000).toLong()
        }
        
        // 发送 EOS 并提取最后的数据（阻塞等待所有帧输出）
        codec.signalEndOfInputStream()
        drainEncoder(codec, muxer, bufferInfo, frameTimestamps, true,
            trackIndex, muxerStarted, outputCount) { newTrackIndex, newMuxerStarted, newOutputCount ->
            trackIndex = newTrackIndex
            muxerStarted = newMuxerStarted
            outputCount = newOutputCount
        }
        
        encodingSuccessful = true
        
    } catch (e: Exception) {
        Log.e("UgoiraMp4", "MP4 encoding failed: ${e.javaClass.simpleName}: ${e.message}", e)
        
        // 尝试获取编码器状态信息
        try {
            codec?.let { c ->
                Log.e("UgoiraMp4", "Codec name: ${c.name}")
            }
        } catch (ex: Exception) {
            Log.e("UgoiraMp4", "Cannot get codec info: ${ex.message}")
        }
        
        throw RuntimeException("MP4 encoding failed: ${e.message}", e)
    } finally {
        try {
            inputSurface?.release()
        } catch (e: Exception) {
            // Ignore
        }
        
        try {
            codec?.let {
                it.stop()
                it.release()
            }
        } catch (e: Exception) {
            // Ignore
        }
        
        try {
            muxer?.let {
                it.stop()
                it.release()
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    // 等待 muxer 完全停止后再读取文件
    if (!encodingSuccessful) {
        if (tempFile.exists()) {
            tempFile.delete()
        }
        throw IllegalStateException("Encoding was not successful")
    }
    
    return@withContext try {
        val data = tempFile.readBytes()
        data
    } finally {
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }
}

/**
 * 提取编码器输出数据并重写时间戳（离线模式）
 */
private fun drainEncoder(
    codec: MediaCodec,
    muxer: MediaMuxer,
    bufferInfo: MediaCodec.BufferInfo,
    frameTimestamps: MutableList<Long>,
    endOfStream: Boolean,
    currentTrackIndex: Int,
    currentMuxerStarted: Boolean,
    currentOutputCount: Int,
    updateState: (trackIndex: Int, muxerStarted: Boolean, outputCount: Int) -> Unit
) {
    var trackIndex = currentTrackIndex
    var muxerStarted = currentMuxerStarted
    var outputCount = currentOutputCount
    var writtenThisCall = 0
    
    // 阻塞等待至少输出一个可用帧（若 frameTimestamps 不为空）或到达超时
    val timeout = if (endOfStream || frameTimestamps.isNotEmpty()) 10_000L else 0L
    
    while (true) {
        val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, timeout)
        
        when {
            outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                if (!endOfStream) {
                    // 若已经输出了至少一帧，则可以返回
                    if (writtenThisCall > 0 || frameTimestamps.isEmpty()) {
                        break
                    }
                    // 否则继续等待（第一次循环时 timeout 已经阻塞过了）
                    break
                }
                // EOS 模式下继续等待
            }
            outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                if (!muxerStarted) {
                    val outputFormat = codec.outputFormat
                    trackIndex = muxer.addTrack(outputFormat)
                    muxer.start()
                    muxerStarted = true
                }
            }
            outputBufferIndex >= 0 -> {
                val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                
                // 检查是否是配置帧（SPS/PPS 等元数据，不消耗时间戳）
                val isConfigFrame = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                
                // ⭐ 关键：只对真正的视频帧覆盖时间戳
                if (!isConfigFrame && frameTimestamps.isNotEmpty()) {
                    bufferInfo.presentationTimeUs = frameTimestamps.removeAt(0)
                }
                
                if (outputBuffer != null && bufferInfo.size > 0 && muxerStarted && !isConfigFrame) {
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    
                    muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                    outputCount++
                    writtenThisCall++
                }
                
                codec.releaseOutputBuffer(outputBufferIndex, false)
                
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
            else -> {
                break
            }
        }
    }
    
    // 更新状态
    updateState(trackIndex, muxerStarted, outputCount)
}

