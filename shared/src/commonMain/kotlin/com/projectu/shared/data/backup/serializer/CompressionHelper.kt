package com.projectu.shared.data.backup.serializer

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * 压缩助手
 */
object CompressionHelper {
    
    /**
     * 将目录压缩为ZIP文件
     * @param sourceDir 源目录
     * @param zipFile 输出ZIP文件
     */
    fun compressToZip(sourceDir: File, zipFile: File) {
        ZipOutputStream(zipFile.outputStream()).use { zos ->
            sourceDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val relativePath = file.relativeTo(sourceDir).path
                    val entry = ZipEntry(relativePath)
                    zos.putNextEntry(entry)
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
    }
    
    /**
     * 解压ZIP文件到目录
     * @param zipFile ZIP文件
     * @param destDir 目标目录
     */
    fun decompressZip(zipFile: File, destDir: File) {
        destDir.mkdirs()
        
        ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val destFile = File(destDir, entry.name)
                
                if (entry.isDirectory) {
                    destFile.mkdirs()
                } else {
                    destFile.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 验证ZIP文件是否有效
     */
    fun isValidZip(file: File): Boolean {
        return try {
            ZipFile(file).use { true }
        } catch (e: Exception) {
            false
        }
    }
}
