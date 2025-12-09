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
 * 使用设备支持的 YUV 颜色格式，确保兼容性
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
    
    // YUV420 格式要求宽高必须是偶数，将奇数尺寸向上对齐
    val width = if (originalWidth % 2 == 1) originalWidth + 1 else originalWidth
    val height = if (originalHeight % 2 == 1) originalHeight + 1 else originalHeight
    
    Log.d("UgoiraMp4", "Starting MP4 encoding: ${frames.size} frames, original=${originalWidth}x${originalHeight}, aligned=${width}x${height}")
    
    // 计算平均帧率
    val averageDelay = metadata.frames.map { it.delay }.average()
    val frameRate = (1000 / averageDelay).toInt().coerceIn(10, 60)
    
    Log.d("UgoiraMp4", "Frame rate: $frameRate fps (avg delay: $averageDelay ms)")
    
    val tempFile = java.io.File.createTempFile("ugoira_temp_", ".mp4")
    
    var codec: MediaCodec? = null
    var muxer: MediaMuxer? = null
    var inputCount = 0
    var outputCount = 0
    var encodingSuccessful = false
    
    try {
        // 创建 MediaMuxer
        muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        
        // 查找支持的颜色格式
        val colorFormat = selectColorFormat(MediaFormat.MIMETYPE_VIDEO_AVC)
        Log.d("UgoiraMp4", "Selected color format: $colorFormat")
        
        // 配置编码器
        val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
        val format = MediaFormat.createVideoFormat(mimeType, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
            setInteger(MediaFormat.KEY_BIT_RATE, 8_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // 每秒一个I帧
            // 确保兼容性
            if (android.os.Build.VERSION.SDK_INT >= 25) {
                setInteger(MediaFormat.KEY_PROFILE, android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline)
                setInteger(MediaFormat.KEY_LEVEL, android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel31)
            }
        }
        
        codec = MediaCodec.createEncoderByType(mimeType)
        
        // 打印编码器详细信息
        val codecInfo = codec.codecInfo
        Log.d("UgoiraMp4", "Codec info: name=${codecInfo.name}, isEncoder=${codecInfo.isEncoder}")
        Log.d("UgoiraMp4", "Codec capabilities: ${codecInfo.getCapabilitiesForType(mimeType).colorFormats.joinToString()}")
        
        // 打印配置参数
        Log.d("UgoiraMp4", "Codec config: width=$width, height=$height, fps=$frameRate, bitrate=8Mbps, colorFormat=$colorFormat")
        
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        
        // 打印配置后的格式
        try {
            val inputFormat = codec.inputFormat
            Log.d("UgoiraMp4", "Input format after configure: $inputFormat")
        } catch (e: Exception) {
            Log.w("UgoiraMp4", "Cannot get input format before start: ${e.message}")
        }
        
        codec.start()
        
        Log.d("UgoiraMp4", "Codec started successfully")
        
        // 打印启动后的格式
        try {
            val inputFormat = codec.inputFormat
            Log.d("UgoiraMp4", "Input format after start: $inputFormat")
        } catch (e: Exception) {
            Log.w("UgoiraMp4", "Cannot get input format: ${e.message}")
        }
        
        val bufferInfo = MediaCodec.BufferInfo()
        var trackIndex = -1
        var muxerStarted = false
        var frameIndex = 0
        var presentationTimeUs = 0L
        var allInputSent = false
        var outputDone = false
        
        while (!outputDone) {
            // 1. 先尝试提交输入（非阻塞），尽快填充编码器
            var inputBufferAvailable = false
            if (!allInputSent) {
                val inputBufferIndex = codec.dequeueInputBuffer(0)
                Log.d("UgoiraMp4", "Loop iteration: frameIndex=$frameIndex, inputCount=$inputCount, outputCount=$outputCount, dequeueInput=$inputBufferIndex")
                if (inputBufferIndex >= 0) {
                    inputBufferAvailable = true
                    if (frameIndex < frames.size) {
                        try {
                            val frame = frames[frameIndex]
                            val bitmap = frame.imageData as Bitmap
                            
                            val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                            if (inputBuffer != null) {
                                val bufferCapacity = inputBuffer.capacity()
                                val expectedSize = width * height * 3 / 2
                                
                                Log.d("UgoiraMp4", "Frame $frameIndex: buffer capacity=$bufferCapacity, expected=$expectedSize")
                                
                                // 转换 YUV（bitmap 是原始尺寸，需要填充到对齐尺寸）
                                val startTime = System.currentTimeMillis()
                                val yuvData = bitmapToYuvWithStride(
                                    bitmap, 
                                    colorFormat, 
                                    originalWidth,  // bitmap 的实际宽度
                                    originalHeight, // bitmap 的实际高度
                                    width,          // 对齐后的宽度
                                    height,         // 对齐后的高度
                                    bufferCapacity
                                )
                                val conversionTime = System.currentTimeMillis() - startTime
                                
                                Log.d("UgoiraMp4", "YUV conversion took ${conversionTime}ms, data size: ${yuvData.size}")
                                
                                // 验证数据大小
                                if (yuvData.size != bufferCapacity) {
                                    val errorMsg = "YUV data size mismatch: ${yuvData.size} != $bufferCapacity"
                                    Log.e("UgoiraMp4", errorMsg)
                                    throw IllegalStateException(errorMsg)
                                }
                                
                                // 验证 YUV 数据的合理性（采样检查）
                                val sampleSize = minOf(100, yuvData.size)
                                val validBytes = yuvData.take(sampleSize).count { b -> b in 0..255.toByte() || b in (-128).toByte()..(-1).toByte() }
                                Log.d("UgoiraMp4", "YUV data sample check: $validBytes/$sampleSize bytes valid")
                                
                                // 打印 YUV 数据的头部和 UV 平面头部（用于调试）
                                val header = yuvData.take(16).joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
                                Log.d("UgoiraMp4", "YUV data header: $header")
                                
                                // 检查 UV 平面数据
                                val calculatedStride = (bufferCapacity * 2) / (height * 3)
                                val yPlaneSize = calculatedStride * height
                                if (yuvData.size > yPlaneSize + 15) {
                                    val uvHeader = yuvData.slice(yPlaneSize until yPlaneSize + 16).joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
                                    Log.d("UgoiraMp4", "UV plane header (at offset $yPlaneSize): $uvHeader")
                                    Log.d("UgoiraMp4", "Y plane size: $yPlaneSize, UV plane size: ${yuvData.size - yPlaneSize}, expected UV size: ${calculatedStride * ((height + 1) / 2)}")
                                }
                                
                                inputBuffer.clear()
                                inputBuffer.put(yuvData)
                                
                                Log.d("UgoiraMp4", "Queuing frame $frameIndex, size=${yuvData.size}, pts=$presentationTimeUs")
                                
                                try {
                                    codec.queueInputBuffer(
                                        inputBufferIndex,
                                        0,
                                        yuvData.size,
                                        presentationTimeUs,
                                        0
                                    )
                                    Log.d("UgoiraMp4", "Successfully queued input buffer for frame $frameIndex")
                                    
                                    // 检查编码器在接收数据后是否还正常
                                    try {
                                        val testIndex = codec.dequeueInputBuffer(0)
                                        Log.d("UgoiraMp4", "Codec state after queue: dequeueInput returned $testIndex")
                                    } catch (e: Exception) {
                                        Log.e("UgoiraMp4", "Codec became invalid after receiving frame $frameIndex!", e)
                                        throw e
                                    }
                                } catch (e: Exception) {
                                    Log.e("UgoiraMp4", "Failed to queue input buffer for frame $frameIndex", e)
                                    throw e
                                }
                                
                                inputCount++
                                presentationTimeUs += (frame.delay * 1000).toLong()
                                frameIndex++
                                
                                Log.d("UgoiraMp4", "Successfully submitted frame $inputCount/${frames.size}")
                            }
                        } catch (e: Exception) {
                            Log.e("UgoiraMp4", "Error processing frame $frameIndex", e)
                            throw e
                        }
                    } else {
                        // EOS - 单独发送，不带数据
                        codec.queueInputBuffer(
                            inputBufferIndex,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        allInputSent = true
                        Log.d("UgoiraMp4", "All $inputCount frames submitted, sent EOS")
                    }
                }
            }
            
            // 2. 然后检查输出，循环消耗所有可用的输出
            // 重要：如果输入缓冲区满了（inputBufferIndex == -1），必须处理输出来释放空间
            // 修复：只在以下情况检查输出
            // - 已经提交了一些帧（inputCount > 0）且输入缓冲区不可用
            // - 所有输入已发送
            // - 已经积累了足够的帧（避免过早检查输出）
            val shouldCheckOutput = allInputSent || 
                                   (!inputBufferAvailable && inputCount > 0) ||
                                   (inputCount >= 10 && !inputBufferAvailable)
            
            Log.d("UgoiraMp4", "Check output decision: shouldCheckOutput=$shouldCheckOutput (inputCount=$inputCount, allInputSent=$allInputSent, inputBufferAvailable=$inputBufferAvailable)")
            
            if (shouldCheckOutput) {
                // 循环处理所有可用的输出，避免输出堆积
                var processedOutputThisIteration = false
                var outputLoopCount = 0
                val maxOutputLoops = 100  // 防止死循环
                
                while (outputLoopCount < maxOutputLoops) {
                    outputLoopCount++
                    
                    // 如果输入缓冲区满了且这是第一次尝试获取输出，使用短暂阻塞等待
                    val timeout = if (!inputBufferAvailable && !processedOutputThisIteration && !allInputSent) {
                        10L  // 输入满了，等待 10ms 让编码器处理
                    } else if (allInputSent && !processedOutputThisIteration) {
                        10_000L  // 所有输入已发送，等待最终输出
                    } else {
                        0L  // 非阻塞快速检查
                    }
                    
                    try {
                        val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, timeout)
                        when {
                            outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                                if (!processedOutputThisIteration && timeout > 0) {
                                    if (inputCount > 0 && outputCount == 0 && !muxerStarted) {
                                        Log.d("UgoiraMp4", "Waiting for output format change, input=$inputCount")
                                    } else {
                                        Log.d("UgoiraMp4", "No output yet, input=$inputCount, output=$outputCount, timeout=${timeout}ms")
                                    }
                                    if (allInputSent && outputCount == 0) {
                                        Log.w("UgoiraMp4", "All input sent but no output received yet!")
                                    }
                                }
                                break  // 退出输出处理循环，继续下一次主循环
                            }
                            outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                val outputFormat = codec.outputFormat
                                Log.d("UgoiraMp4", "Output format changed: $outputFormat")
                                
                                if (!muxerStarted) {
                                    trackIndex = muxer.addTrack(outputFormat)
                                    muxer.start()
                                    muxerStarted = true
                                    Log.d("UgoiraMp4", "Muxer started, track index: $trackIndex")
                                }
                                processedOutputThisIteration = true
                                // 注意：FORMAT_CHANGED 不是真正的输出帧，不要增加 outputCount
                            }
                            outputBufferIndex >= 0 -> {
                                Log.d("UgoiraMp4", "Got output buffer: index=$outputBufferIndex, size=${bufferInfo.size}, pts=${bufferInfo.presentationTimeUs}, flags=${bufferInfo.flags}")
                                
                                val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                                if (outputBuffer != null && bufferInfo.size > 0) {
                                    if (!muxerStarted) {
                                        Log.e("UgoiraMp4", "ERROR: Got output before muxer started! This should not happen.")
                                        Log.e("UgoiraMp4", "Output info: size=${bufferInfo.size}, pts=${bufferInfo.presentationTimeUs}, flags=${bufferInfo.flags}")
                                        // 释放这个 buffer 并继续
                                        codec.releaseOutputBuffer(outputBufferIndex, false)
                                        processedOutputThisIteration = true
                                        continue
                                    }
                                    
                                    outputBuffer.position(bufferInfo.offset)
                                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                    
                                    try {
                                        muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                                        outputCount++
                                        Log.d("UgoiraMp4", "Written output frame $outputCount, size=${bufferInfo.size}, pts=${bufferInfo.presentationTimeUs}")
                                    } catch (e: Exception) {
                                        Log.e("UgoiraMp4", "Failed to write output frame $outputCount", e)
                                        throw e
                                    }
                                }
                                
                                codec.releaseOutputBuffer(outputBufferIndex, false)
                                processedOutputThisIteration = true
                                
                                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                    outputDone = true
                                    Log.d("UgoiraMp4", "Encoding done, output $outputCount frames")
                                    break  // 退出输出处理循环
                                }
                            }
                            else -> {
                                Log.w("UgoiraMp4", "Unexpected output buffer index: $outputBufferIndex")
                                break  // 退出输出处理循环
                            }
                        }
                    } catch (e: IllegalStateException) {
                        Log.e("UgoiraMp4", "Codec state error during dequeue: ${e.message}", e)
                        Log.e("UgoiraMp4", "Codec state info: input=$inputCount, output=$outputCount, muxerStarted=$muxerStarted")
                        throw e
                    } catch (e: Exception) {
                        Log.e("UgoiraMp4", "Unexpected error during output processing", e)
                        throw e
                    }
                }
                
                // 检查是否输出循环达到上限（可能是死循环）
                if (outputLoopCount >= maxOutputLoops) {
                    Log.w("UgoiraMp4", "Output loop reached maximum iterations ($maxOutputLoops), breaking to avoid deadlock")
                }
            }
            
            // 3. 如果既没有输入也没有输出，稍微等待避免忙等
            if (!inputBufferAvailable && !allInputSent) {
                Thread.sleep(1)
            }
        }
        
        Log.d("UgoiraMp4", "MP4 encoding completed successfully, total frames: input=$inputCount, output=$outputCount")
        encodingSuccessful = true
        
    } catch (e: Exception) {
        Log.e("UgoiraMp4", "MP4 encoding failed: ${e.javaClass.simpleName}: ${e.message}", e)
        Log.e("UgoiraMp4", "Encoding state at failure: input=$inputCount, output=$outputCount")
        
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
        Log.d("UgoiraMp4", "Cleaning up resources...")
        
        try {
            codec?.let {
                Log.d("UgoiraMp4", "Stopping codec...")
                it.stop()
                Log.d("UgoiraMp4", "Codec stopped")
                it.release()
                Log.d("UgoiraMp4", "Codec released")
            }
        } catch (e: Exception) {
            Log.w("UgoiraMp4", "Error releasing codec: ${e.javaClass.simpleName}: ${e.message}", e)
        }
        
        try {
            muxer?.let {
                Log.d("UgoiraMp4", "Stopping muxer...")
                it.stop()
                Log.d("UgoiraMp4", "Muxer stopped")
                it.release()
                Log.d("UgoiraMp4", "Muxer released")
            }
        } catch (e: Exception) {
            Log.w("UgoiraMp4", "Error releasing muxer: ${e.javaClass.simpleName}: ${e.message}", e)
        }
    }
    
    // 在 muxer 完全停止后再读取文件
    if (!encodingSuccessful) {
        if (tempFile.exists()) {
            tempFile.delete()
        }
        throw IllegalStateException("Encoding was not successful")
    }
    
    return@withContext try {
        val data = tempFile.readBytes()
        Log.d("UgoiraMp4", "Successfully read MP4 file: ${data.size} bytes")
        data
    } finally {
        if (tempFile.exists()) {
            tempFile.delete()
            Log.d("UgoiraMp4", "Temp file deleted")
        }
    }
}

/**
 * 查找编码器支持的颜色格式
 */
private fun selectColorFormat(mimeType: String): Int {
    val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
    val codecInfos = codecList.codecInfos
    
    for (codecInfo in codecInfos) {
        if (!codecInfo.isEncoder) continue
        if (!codecInfo.supportedTypes.contains(mimeType)) continue
        
        val capabilities = codecInfo.getCapabilitiesForType(mimeType)
        val colorFormats = capabilities.colorFormats
        
        // 优先选择常见格式（NV12 优先，兼容性更好）
        for (format in colorFormats) {
            when (format) {
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar -> {
                    Log.d("UgoiraMp4", "Found supported format: $format (NV12) for codec: ${codecInfo.name}")
                    return format
                }
            }
        }
        
        // 次优选择 I420
        for (format in colorFormats) {
            when (format) {
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar -> {
                    Log.d("UgoiraMp4", "Found supported format: $format (I420) for codec: ${codecInfo.name}")
                    return format
                }
            }
        }
    }
    
    // 回退到默认
    Log.w("UgoiraMp4", "No preferred format found, using COLOR_FormatYUV420SemiPlanar")
    return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
}

/**
 * 将 Bitmap 转换为 YUV 格式（简化版本，不再使用）
 */
private fun bitmapToYuv(bitmap: Bitmap, colorFormat: Int): ByteArray {
    val width = if (bitmap.width % 2 == 1) bitmap.width + 1 else bitmap.width
    val height = if (bitmap.height % 2 == 1) bitmap.height + 1 else bitmap.height
    return bitmapToYuvWithStride(
        bitmap, 
        colorFormat, 
        bitmap.width,
        bitmap.height,
        width,
        height,
        width * height * 3 / 2
    )
}

/**
 * 将 Bitmap 转换为 YUV 格式（支持尺寸对齐和 stride）
 */
private fun bitmapToYuvWithStride(
    bitmap: Bitmap, 
    colorFormat: Int,
    bitmapWidth: Int,
    bitmapHeight: Int,
    alignedWidth: Int,
    alignedHeight: Int,
    bufferSize: Int
): ByteArray {
    val pixels = IntArray(bitmapWidth * bitmapHeight)
    bitmap.getPixels(pixels, 0, bitmapWidth, 0, 0, bitmapWidth, bitmapHeight)
    
    // 从 buffer size 反推 stride
    val calculatedStride = (bufferSize * 2) / (alignedHeight * 3)
    val yStride = if (calculatedStride >= alignedWidth) {
        calculatedStride
    } else {
        alignedWidth
    }
    
    Log.d("UgoiraMp4", "YUV conversion: bitmap=${bitmapWidth}x${bitmapHeight}, aligned=${alignedWidth}x${alignedHeight}, buffer=$bufferSize, stride=$yStride")
    
    return when (colorFormat) {
        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar -> {
            encodeYUV420SP(pixels, bitmapWidth, bitmapHeight, alignedWidth, alignedHeight, yStride)
        }
        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar -> {
            encodeYUV420P(pixels, bitmapWidth, bitmapHeight, alignedWidth, alignedHeight, yStride)
        }
        else -> {
            encodeYUV420SP(pixels, bitmapWidth, bitmapHeight, alignedWidth, alignedHeight, yStride)
        }
    }
}

/**
 * 编码为 YUV420SP (NV12) 格式
 * @param argb 像素数组（原始尺寸）
 * @param bitmapWidth 原始图像宽度
 * @param bitmapHeight 原始图像高度
 * @param alignedWidth 对齐后的宽度（偶数）
 * @param alignedHeight 对齐后的高度（偶数）
 * @param stride Y 平面的 stride
 */
private fun encodeYUV420SP(
    argb: IntArray, 
    bitmapWidth: Int, 
    bitmapHeight: Int, 
    alignedWidth: Int, 
    alignedHeight: Int, 
    stride: Int
): ByteArray {
    val frameSize = stride * alignedHeight
    val uvStride = stride
    val uvWidth = alignedWidth / 2
    val uvHeight = alignedHeight / 2
    val uvPlaneSize = uvStride * uvHeight
    val yuv = ByteArray(frameSize + uvPlaneSize)
    
    Log.d("UgoiraMp4", "NV12 encoding: bitmap=${bitmapWidth}x${bitmapHeight}, aligned=${alignedWidth}x${alignedHeight}, stride=$stride")
    Log.d("UgoiraMp4", "Y plane: $frameSize bytes, UV plane: $uvPlaneSize bytes, total=${yuv.size}")
    
    // 填充 Y 平面
    for (j in 0 until alignedHeight) {
        for (i in 0 until alignedWidth) {
            if (i < bitmapWidth && j < bitmapHeight) {
                // 来自原始图像的像素
                val pixel = argb[j * bitmapWidth + i]
                val r = (pixel shr 16) and 0xff
                val g = (pixel shr 8) and 0xff
                val b = pixel and 0xff
                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                yuv[j * stride + i] = y.coerceIn(0, 255).toByte()
            } else {
                // 对齐填充区域（黑色）
                yuv[j * stride + i] = 16.toByte()
            }
        }
        // Stride padding
        for (i in alignedWidth until stride) {
            yuv[j * stride + i] = 16.toByte()
        }
    }
    
    // 填充 UV 平面
    val uvIndex = frameSize
    for (j in 0 until uvHeight) {
        for (i in 0 until uvWidth) {
            val y0 = j * 2
            val y1 = y0 + 1
            val x0 = i * 2
            val x1 = x0 + 1
            
            if (x1 < bitmapWidth && y1 < bitmapHeight) {
                // 2x2 块完全在原始图像内
                val pixel0 = argb[y0 * bitmapWidth + x0]
                val pixel1 = argb[y0 * bitmapWidth + x1]
                val pixel2 = argb[y1 * bitmapWidth + x0]
                val pixel3 = argb[y1 * bitmapWidth + x1]
                
                val r = ((pixel0 shr 16 and 0xff) + (pixel1 shr 16 and 0xff) + 
                         (pixel2 shr 16 and 0xff) + (pixel3 shr 16 and 0xff)) / 4
                val g = ((pixel0 shr 8 and 0xff) + (pixel1 shr 8 and 0xff) + 
                         (pixel2 shr 8 and 0xff) + (pixel3 shr 8 and 0xff)) / 4
                val b = ((pixel0 and 0xff) + (pixel1 and 0xff) + 
                         (pixel2 and 0xff) + (pixel3 and 0xff)) / 4
                
                val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                
                yuv[uvIndex + j * uvStride + i * 2] = u.coerceIn(0, 255).toByte()
                yuv[uvIndex + j * uvStride + i * 2 + 1] = v.coerceIn(0, 255).toByte()
            } else if (x0 < bitmapWidth && y0 < bitmapHeight) {
                // 部分块在原始图像内（处理边界情况）
                var r = 0
                var g = 0
                var b = 0
                var count = 0
                
                for (dy in 0..1) {
                    for (dx in 0..1) {
                        val x = x0 + dx
                        val y = y0 + dy
                        if (x < bitmapWidth && y < bitmapHeight) {
                            val pixel = argb[y * bitmapWidth + x]
                            r += (pixel shr 16) and 0xff
                            g += (pixel shr 8) and 0xff
                            b += pixel and 0xff
                            count++
                        }
                    }
                }
                
                if (count > 0) {
                    r /= count
                    g /= count
                    b /= count
                    
                    val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    
                    yuv[uvIndex + j * uvStride + i * 2] = u.coerceIn(0, 255).toByte()
                    yuv[uvIndex + j * uvStride + i * 2 + 1] = v.coerceIn(0, 255).toByte()
                } else {
                    yuv[uvIndex + j * uvStride + i * 2] = 128.toByte()
                    yuv[uvIndex + j * uvStride + i * 2 + 1] = 128.toByte()
                }
            } else {
                // 完全在填充区域（灰色：U=V=128）
                yuv[uvIndex + j * uvStride + i * 2] = 128.toByte()
                yuv[uvIndex + j * uvStride + i * 2 + 1] = 128.toByte()
            }
        }
        // UV 平面 stride padding
        for (i in uvWidth * 2 until uvStride) {
            yuv[uvIndex + j * uvStride + i] = 128.toByte()
        }
    }
    
    Log.d("UgoiraMp4", "NV12 encoding completed, total bytes: ${yuv.size}")
    return yuv
}

/**
 * 编码为 YUV420P (I420) 格式
 */
private fun encodeYUV420P(
    argb: IntArray, 
    bitmapWidth: Int, 
    bitmapHeight: Int, 
    alignedWidth: Int, 
    alignedHeight: Int, 
    stride: Int
): ByteArray {
    val frameSize = stride * alignedHeight
    val uvWidth = alignedWidth / 2
    val uvHeight = alignedHeight / 2
    val uvStride = stride / 2
    val uvPlaneSize = uvStride * uvHeight
    val yuv = ByteArray(frameSize + uvPlaneSize * 2)
    
    // 填充 Y 平面
    for (j in 0 until alignedHeight) {
        for (i in 0 until alignedWidth) {
            if (i < bitmapWidth && j < bitmapHeight) {
                val pixel = argb[j * bitmapWidth + i]
                val r = (pixel shr 16) and 0xff
                val g = (pixel shr 8) and 0xff
                val b = pixel and 0xff
                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                yuv[j * stride + i] = y.coerceIn(0, 255).toByte()
            } else {
                yuv[j * stride + i] = 16.toByte()
            }
        }
        for (i in alignedWidth until stride) {
            yuv[j * stride + i] = 16.toByte()
        }
    }
    
    // 填充 U 和 V 平面
    val uIndex = frameSize
    val vIndex = frameSize + uvPlaneSize
    
    for (j in 0 until uvHeight) {
        for (i in 0 until uvWidth) {
            val y0 = j * 2
            val y1 = y0 + 1
            val x0 = i * 2
            val x1 = x0 + 1
            
            if (x1 < bitmapWidth && y1 < bitmapHeight) {
                // 2x2 块完全在原始图像内
                val pixel0 = argb[y0 * bitmapWidth + x0]
                val pixel1 = argb[y0 * bitmapWidth + x1]
                val pixel2 = argb[y1 * bitmapWidth + x0]
                val pixel3 = argb[y1 * bitmapWidth + x1]
                
                val r = ((pixel0 shr 16 and 0xff) + (pixel1 shr 16 and 0xff) + 
                         (pixel2 shr 16 and 0xff) + (pixel3 shr 16 and 0xff)) / 4
                val g = ((pixel0 shr 8 and 0xff) + (pixel1 shr 8 and 0xff) + 
                         (pixel2 shr 8 and 0xff) + (pixel3 shr 8 and 0xff)) / 4
                val b = ((pixel0 and 0xff) + (pixel1 and 0xff) + 
                         (pixel2 and 0xff) + (pixel3 and 0xff)) / 4
                
                val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                
                yuv[uIndex + j * uvStride + i] = u.coerceIn(0, 255).toByte()
                yuv[vIndex + j * uvStride + i] = v.coerceIn(0, 255).toByte()
            } else if (x0 < bitmapWidth && y0 < bitmapHeight) {
                // 部分块在原始图像内（处理边界情况）
                var r = 0
                var g = 0
                var b = 0
                var count = 0
                
                for (dy in 0..1) {
                    for (dx in 0..1) {
                        val x = x0 + dx
                        val y = y0 + dy
                        if (x < bitmapWidth && y < bitmapHeight) {
                            val pixel = argb[y * bitmapWidth + x]
                            r += (pixel shr 16) and 0xff
                            g += (pixel shr 8) and 0xff
                            b += pixel and 0xff
                            count++
                        }
                    }
                }
                
                if (count > 0) {
                    r /= count
                    g /= count
                    b /= count
                    
                    val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    
                    yuv[uIndex + j * uvStride + i] = u.coerceIn(0, 255).toByte()
                    yuv[vIndex + j * uvStride + i] = v.coerceIn(0, 255).toByte()
                } else {
                    yuv[uIndex + j * uvStride + i] = 128.toByte()
                    yuv[vIndex + j * uvStride + i] = 128.toByte()
                }
            } else {
                // 完全在填充区域（灰色：U=V=128）
                yuv[uIndex + j * uvStride + i] = 128.toByte()
                yuv[vIndex + j * uvStride + i] = 128.toByte()
            }
        }
        for (i in uvWidth until uvStride) {
            yuv[uIndex + j * uvStride + i] = 128.toByte()
            yuv[vIndex + j * uvStride + i] = 128.toByte()
        }
    }
    
    return yuv
}
