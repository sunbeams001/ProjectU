package com.projectu.shared.data.util

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink

/**
 * Desktop 平台的文件写入器
 * 直接使用 Okio FileSystem
 */
class DesktopFileWriter(
    private val fileSystem: FileSystem
) : PlatformFileWriter {
    
    override suspend fun createSink(path: Path, displayName: String): Sink {
        // 确保父目录存在
        path.parent?.let { parent ->
            fileSystem.createDirectories(parent)
        }
        
        return fileSystem.sink(path)
    }
    
    override suspend fun createSinkFromUri(baseUri: String, relativePath: String, fileName: String): Sink {
        // Desktop 不使用 URI，直接拼接路径
        val fullPath = "$baseUri/$relativePath/$fileName".toPath()
        return createSink(fullPath, fileName)
    }
    
    override suspend fun ensureDirectoryExists(baseUri: String, relativePath: String) {
        val dirPath = "$baseUri/$relativePath".toPath()
        if (!fileSystem.exists(dirPath)) {
            fileSystem.createDirectories(dirPath)
        }
    }
    
    override suspend fun moveFile(source: Path, destination: Path) {
        fileSystem.atomicMove(source, destination)
    }
    
    override suspend fun deleteFile(path: Path): Boolean {
        return try {
            fileSystem.delete(path)
            true
        } catch (e: Exception) {
            false
        }
    }
}
