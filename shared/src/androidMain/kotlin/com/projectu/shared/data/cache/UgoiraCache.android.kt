package com.projectu.shared.data.cache

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Path
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * Android平台的ZIP解压实现
 */
actual suspend fun extractZipPlatform(zipPath: Path, targetDir: Path) = withContext(Dispatchers.IO) {
    val zipFile = zipPath.toFile()
    val targetFile = targetDir.toFile()
    
    ZipInputStream(zipFile.inputStream()).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                val outputFile = java.io.File(targetFile, entry.name)
                outputFile.parentFile?.mkdirs()
                
                FileOutputStream(outputFile).use { fos ->
                    zis.copyTo(fos)
                }
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
    }
}

