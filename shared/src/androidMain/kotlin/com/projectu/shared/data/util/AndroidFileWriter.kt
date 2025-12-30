package com.projectu.shared.data.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink
import okio.sink
import java.io.File
import java.io.FileOutputStream

/**
 * Android 平台的文件写入器
 * 根据项目配置 (minSdk=24, targetSdk=36) 使用不同的存储策略：
 * - Android 7-9 (API 24-28): 传统外部存储 + WRITE_EXTERNAL_STORAGE 权限
 * - Android 10-12 (API 29-32): MediaStore API (分区存储)
 * - Android 13+ (API 33+): MediaStore API + READ_MEDIA_IMAGES 权限
 */
class AndroidFileWriter(
    private val context: Context
) : PlatformFileWriter {
    
    companion object {
        // 文件系统通常限制文件名（包括扩展名）不超过 255 字节
        // 留一些余量，设置为 200 字节（约 66 个中文字符）
        private const val MAX_FILENAME_BYTES = 200
    }
    
    /**
     * 截断过长的文件名以符合文件系统限制
     * 保留扩展名和作品 ID（如果存在）
     */
    private fun truncateFileName(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "")
        val nameWithoutExt = if (extension.isNotEmpty()) {
            fileName.substringBeforeLast('.')
        } else {
            fileName
        }
        
        // 计算完整文件名的字节长度（UTF-8）
        val fullBytes = fileName.toByteArray(Charsets.UTF_8).size
        if (fullBytes <= MAX_FILENAME_BYTES) {
            return fileName
        }
        
        // 需要截断
        // 保留扩展名的字节数 + 点号
        val extensionBytes = if (extension.isNotEmpty()) {
            extension.toByteArray(Charsets.UTF_8).size + 1 // +1 for '.'
        } else {
            0
        }
        
        // 可用于文件名主体的字节数，预留 3 字节用于省略号
        val availableBytes = MAX_FILENAME_BYTES - extensionBytes - 3
        
        // 尝试保留作品 ID（格式：数字_标题）
        val idMatch = Regex("^(\\d+)_").find(nameWithoutExt)
        val prefixToKeep = idMatch?.value ?: ""
        val prefixBytes = prefixToKeep.toByteArray(Charsets.UTF_8).size
        
        // 截断主体部分
        var truncated = nameWithoutExt
        var currentBytes = nameWithoutExt.toByteArray(Charsets.UTF_8).size
        
        while (currentBytes > availableBytes && truncated.isNotEmpty()) {
            truncated = truncated.dropLast(1)
            currentBytes = truncated.toByteArray(Charsets.UTF_8).size
        }
        
        // 组装最终文件名
        val result = if (extension.isNotEmpty()) {
            "$truncated...$extension"
        } else {
            "$truncated..."
        }
        
        return result
    }
    
    override suspend fun createSink(path: Path, displayName: String): Sink {
        return when {
            // Android 10+ (API 29+): 强制分区存储，使用 MediaStore API
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                createMediaStoreSink(path, displayName)
            }
            // Android 7-9 (API 24-28): 传统存储方式
            else -> {
                createLegacySink(path)
            }
        }
    }
    
    /**
     * 从 SAF URI 创建文件输出流
     * 使用 DocumentFile API 处理 content:// URI
     */
    override suspend fun createSinkFromUri(baseUri: String, relativePath: String, fileName: String): Sink {
        if (!baseUri.startsWith("content://")) {
            // 非 SAF URI (传统路径)
            // Android 10+ 不允许直接访问共享存储，需要使用 MediaStore
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // 构建完整路径用于 MediaStore
                val fullPath = "$baseUri/$relativePath/$fileName"
                return createMediaStoreSink(fullPath.toPath(), fileName)
            } else {
                // Android 7-9: 可以使用传统路径（需要截断文件名）
                val sanitizedFileName = truncateFileName(fileName)
                val fullPath = "$baseUri/$relativePath/$sanitizedFileName"
                return createLegacySink(fullPath.toPath())
            }
        }
        
        val baseDocumentFile = DocumentFile.fromTreeUri(context, Uri.parse(baseUri))
            ?: throw IllegalStateException("Failed to access directory: $baseUri")
        
        // 创建子目录结构
        val targetDir = ensureDocumentDirectoryExists(baseDocumentFile, relativePath)
        
        // 删除可能存在的同名文件（多次尝试以确保删除成功）
        var existingFile = targetDir.findFile(fileName)
        if (existingFile != null) {
            existingFile.delete()
            // 等待一小段时间确保文件系统同步
            kotlinx.coroutines.delay(100)
            // 再次检查是否还存在
            existingFile = targetDir.findFile(fileName)
            if (existingFile != null) {
                existingFile.delete()
            }
        }
        
        // 确定 MIME 类型
        val mimeType = getMimeType(fileName)
        
        // 创建文件，如果失败则尝试强制删除后重新创建
        var file = try {
            targetDir.createFile(mimeType, fileName)
        } catch (e: Exception) {
            // 创建失败，可能是因为文件残留，强制刷新目录并重试
            e.printStackTrace()
            
            // 最后一次尝试：遍历目录查找并删除
            targetDir.listFiles().find { it.name == fileName }?.delete()
            kotlinx.coroutines.delay(100)
            
            // 重试创建
            targetDir.createFile(mimeType, fileName)
        }
        
        if (file == null) {
            throw IllegalStateException("Failed to create file: $fileName in $relativePath (file may already exist)")
        }
        
        val outputStream = context.contentResolver.openOutputStream(file.uri)
            ?: throw IllegalStateException("Failed to open output stream for ${file.uri}")
        
        return outputStream.sink()
    }
    
    /**
     * 确保 DocumentFile 目录存在
     * 递归创建子目录
     */
    private fun ensureDocumentDirectoryExists(baseDir: DocumentFile, relativePath: String): DocumentFile {
        if (relativePath.isEmpty() || relativePath == ".") {
            return baseDir
        }
        
        val parts = relativePath.split("/").filter { it.isNotEmpty() }
        var currentDir = baseDir
        
        for (part in parts) {
            val existing = currentDir.findFile(part)
            currentDir = if (existing != null && existing.isDirectory) {
                existing
            } else {
                currentDir.createDirectory(part)
                    ?: throw IllegalStateException("Failed to create directory: $part")
            }
        }
        
        return currentDir
    }
    
    /**
     * 确保目录存在（支持 URI）
     */
    override suspend fun ensureDirectoryExists(baseUri: String, relativePath: String) {
        if (baseUri.startsWith("content://")) {
            // 使用 DocumentFile API
            val baseDocumentFile = DocumentFile.fromTreeUri(context, Uri.parse(baseUri))
                ?: throw IllegalStateException("Failed to access directory: $baseUri")
            ensureDocumentDirectoryExists(baseDocumentFile, relativePath)
        } else {
            // 传统文件系统
            val dir = File("$baseUri/$relativePath")
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
    }
    
    /**
     * Android 7-9: 传统文件系统访问
     * 需要 WRITE_EXTERNAL_STORAGE 权限
     * 
     * 注意：即使是 legacy 路径，也可能遇到 Scoped Storage 的孤儿文件问题：
     * - 用户从相册删除文件后，MediaStore 记录被删除，但文件系统可能残留
     * - File.exists() 依赖 MediaStore 返回 false，但 open() 能看到文件报 EEXIST
     * 
     * 解决方案：
     * 1. 先通过 MediaStore 删除孤儿记录
     * 2. 使用 FileOutputStream 覆盖模式（O_TRUNC）而不是 Okio.sink()（O_EXCL）
     */
    private fun createLegacySink(path: Path): Sink {
        val file = File(path.toString())
        
        // 确保父目录存在
        file.parentFile?.let { parent ->
            if (!parent.exists()) {
                parent.mkdirs()
            }
        }
        
        // 如果文件可见，先删除
        if (file.exists()) {
            try {
                file.delete()
            } catch (e: Exception) {
                // 忽略删除失败，后续覆盖模式会处理
            }
        }
        
        // 通过 MediaStore 删除可能的孤儿记录（从相册删除后的残留）
        try {
            val fileName = file.name
            val relativePath = file.parentFile?.let { parent ->
                parent.absolutePath.removePrefix("/storage/emulated/0/")
            } ?: ""
            
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
            val selectionArgs = arrayOf(fileName, "$relativePath/")
            
            context.contentResolver.delete(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                selection,
                selectionArgs
            )
        } catch (e: Exception) {
            // MediaStore 删除失败不影响后续流程
        }
        
        try {
            // 使用 FileOutputStream 覆盖模式，避免 EEXIST 错误
            // append=false 使用 O_CREAT|O_TRUNC 标志，允许覆盖现有文件
            return FileOutputStream(file, false).sink()
        } catch (e: Exception) {
            // 最后尝试：强制删除后重试
            try {
                Runtime.getRuntime().exec("rm -f ${file.absolutePath}").waitFor()
                Thread.sleep(100)
                return FileOutputStream(file, false).sink()
            } catch (e2: Exception) {
                throw e // 抛出原始异常
            }
        }
    }
    
    /**
     * Android 10+: MediaStore API
     * 自动处理分区存储，无需 WRITE_EXTERNAL_STORAGE 权限
     */
    private fun createMediaStoreSink(path: Path, displayName: String): Sink {
        val originalRelativePath = extractRelativePath(path)
        val mimeType = getMimeType(displayName)
        
        // 根据文件类型和路径调整 RELATIVE_PATH
        // MediaStore 集合对路径有严格限制：
        // - Images: 只能 Pictures/*
        // - Video: 只能 Movies/*, DCIM/*
        // - Files: 只能 Download/* 或 Documents/*
        val relativePath = adjustRelativePathForMediaStore(originalRelativePath, mimeType)
        
        // 先尝试删除可能存在的同名文件（避免重复记录）
        deleteMediaStoreFileByName(displayName, relativePath)
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            
            // IS_PENDING: 写入过程中对其他应用不可见，写入完成后才显示
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        
        // 根据 MIME 类型选择合适的 MediaStore 集合
        val collection = when {
            mimeType.startsWith("image/") -> {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            mimeType.startsWith("video/") -> {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            else -> {
                // 其他文件类型（如 epub）使用 Files 集合
                // Files 集合允许任意 RELATIVE_PATH，不像 Downloads 只能用 Download 目录
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Files.getContentUri("external")
                }
            }
        }
        
        val uri = context.contentResolver.insert(collection, contentValues)
            ?: throw IllegalStateException("Failed to create MediaStore entry for $displayName")
        
        val outputStream = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("Failed to open output stream for $uri")
        
        return MediaStoreSink(outputStream.sink(), uri, context)
    }
    
    /**
     * 根据 MediaStore 集合限制调整相对路径
     * - Images/Video: 可以用 Pictures/Movies 等标准媒体目录
     * - Files: 只能用 Download 或 Documents
     */
    private fun adjustRelativePathForMediaStore(relativePath: String, mimeType: String): String {
        // 图片和视频可以使用原始路径（通常在 Pictures/Movies）
        if (mimeType.startsWith("image/") || mimeType.startsWith("video/")) {
            return relativePath
        }
        
        // 其他文件类型（如 epub）必须在 Download 或 Documents 下
        // 如果原路径不符合要求，重映射到 Documents
        val topLevelDir = relativePath.substringBefore('/', relativePath)
        
        return when (topLevelDir) {
            "Download", "Documents" -> relativePath // 已经符合要求
            else -> {
                // 将路径重映射到 Documents，保留子目录结构
                // 例如: Pictures/ProjectU/Novels -> Documents/ProjectU/Novels
                val subPath = if (relativePath.contains('/')) {
                    relativePath.substringAfter('/')
                } else {
                    "ProjectU" // 默认子目录
                }
                "Documents/$subPath"
            }
        }
    }
    
    /**
     * 从完整路径提取相对路径
     * 例如: /storage/emulated/0/Pictures/ProjectU/Illustrations/xxx.jpg 
     *      -> Pictures/ProjectU/Illustrations
     */
    private fun extractRelativePath(path: Path): String {
        val pathStr = path.toString()
        val dirPath = pathStr.substringBeforeLast('/')
        
        return when {
            dirPath.contains("/Pictures/") -> {
                "Pictures/" + dirPath.substringAfter("/Pictures/")
            }
            dirPath.contains("/Download/") || dirPath.contains("/Downloads/") -> {
                Environment.DIRECTORY_DOWNLOADS + "/" + 
                    (dirPath.substringAfter("/Download/", "")
                        .ifEmpty { dirPath.substringAfter("/Downloads/") })
            }
            dirPath.contains("/DCIM/") -> {
                Environment.DIRECTORY_DCIM + "/" + dirPath.substringAfter("/DCIM/")
            }
            else -> {
                // 默认使用 Pictures
                "Pictures/" + dirPath.substringAfterLast('/')
            }
        }
    }
    
    private fun getMimeType(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "txt" -> "text/plain"
            "zip" -> "application/zip"
            else -> "application/octet-stream"
        }
    }
    
    override suspend fun moveFile(source: Path, destination: Path) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // MediaStore 不支持直接移动文件
                // 如果需要此功能，应在下载时直接使用最终文件名
                throw UnsupportedOperationException(
                    "MediaStore does not support file move. Use createSink with final name instead."
                )
            }
            else -> {
                val sourceFile = File(source.toString())
                val destFile = File(destination.toString())
                destFile.parentFile?.mkdirs()
                
                if (!sourceFile.renameTo(destFile)) {
                    throw IllegalStateException("Failed to move file from $source to $destination")
                }
            }
        }
    }
    
    override suspend fun deleteFile(path: Path): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                deleteMediaStoreFile(path)
            }
            else -> {
                deleteLegacyFile(path)
            }
        }
    }
    
    /**
     * Android 7-9: 直接删除文件
     */
    private fun deleteLegacyFile(path: Path): Boolean {
        return try {
            val file = File(path.toString())
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Android 10+: 通过 MediaStore 删除
     */
    private fun deleteMediaStoreFile(path: Path): Boolean {
        return try {
            val fileName = path.name
            
            // 尝试从不同的 MediaStore 集合中删除
            val collections = listOf(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                } else null
            ).filterNotNull()
            
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(fileName)
            
            var deleted = 0
            for (collection in collections) {
                deleted += context.contentResolver.delete(collection, selection, selectionArgs)
                if (deleted > 0) break
            }
            
            deleted > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 通过文件名和相对路径删除 MediaStore 记录
     * 用于在创建新文件前清理可能存在的旧记录
     */
    private fun deleteMediaStoreFileByName(displayName: String, relativePath: String) {
        try {
            val collections = listOf(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                } else null
            ).filterNotNull()
            
            // 使用 DISPLAY_NAME 和 RELATIVE_PATH 精确匹配
            val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
            } else {
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
            }
            
            val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf(displayName, relativePath)
            } else {
                arrayOf(displayName)
            }
            
            for (collection in collections) {
                val deleted = context.contentResolver.delete(collection, selection, selectionArgs)
                if (deleted > 0) break
            }
        } catch (e: Exception) {
            // 静默失败，删除失败不影响后续创建
            e.printStackTrace()
        }
    }
    
    /**
     * 自定义 Sink，在关闭时清除 MediaStore 的 IS_PENDING 标记
     */
    private class MediaStoreSink(
        private val delegate: Sink,
        private val uri: android.net.Uri,
        private val context: Context
    ) : Sink by delegate {
        
        override fun close() {
            try {
                delegate.close()
                
                // 清除 IS_PENDING 标记，使文件对其他应用可见
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }
                    context.contentResolver.update(uri, values, null, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
