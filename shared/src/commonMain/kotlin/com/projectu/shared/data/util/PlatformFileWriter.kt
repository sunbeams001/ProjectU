package com.projectu.shared.data.util

import okio.Path
import okio.Sink

/**
 * 平台特定的文件写入器
 * 用于处理不同平台的文件系统权限和API差异
 */
interface PlatformFileWriter {
    /**
     * 创建文件的输出流
     * Android 平台会使用 MediaStore API
     * Desktop 平台直接使用文件系统
     * 
     * @param path 文件路径
     * @param displayName 显示文件名（Android MediaStore 需要）
     * @return Okio Sink
     */
    suspend fun createSink(path: Path, displayName: String): Sink
    
    /**
     * 从 URI 创建文件的输出流（支持 SAF content:// URI）
     * 
     * @param baseUri 基础 URI（例如通过 SAF 选择的目录 URI）
     * @param relativePath 相对路径（例如 "Illustrations/AuthorName"）
     * @param fileName 文件名
     * @return Okio Sink
     */
    suspend fun createSinkFromUri(baseUri: String, relativePath: String, fileName: String): Sink
    
    /**
     * 移动/重命名文件
     * 
     * @param source 源路径
     * @param destination 目标路径
     */
    suspend fun moveFile(source: Path, destination: Path)
    
    /**
     * 确保目录存在（支持 URI）
     * 
     * @param baseUri 基础 URI 或文件路径
     * @param relativePath 相对路径
     */
    suspend fun ensureDirectoryExists(baseUri: String, relativePath: String)
    
    /**
     * 删除文件
     * 
     * @param path 文件路径
     */
    suspend fun deleteFile(path: Path): Boolean
}
