package com.projectu.shared.data.backup.storage

import java.io.File

/**
 * Desktop平台备份存储实现
 */
class DesktopBackupStorage : BackupStorage {
    
    override fun createTempDirectory(prefix: String): File {
        val tempDir = File(System.getProperty("java.io.tmpdir"), prefix)
        tempDir.mkdirs()
        return tempDir
    }
    
    override fun saveBackupFile(sourceFile: File, fileName: String): String {
        val backupDir = getBackupDirectory()
        backupDir.mkdirs()
        
        val destFile = File(backupDir, fileName)
        sourceFile.copyTo(destFile, overwrite = true)
        
        return destFile.absolutePath
    }
    
    override fun getBackupDirectory(): File {
        // 使用用户文档目录下的 ProjectU/Backups
        val documentsDir = File(System.getProperty("user.home"), "Documents")
        return File(documentsDir, "ProjectU/Backups")
    }
    
    override fun listBackupFiles(): List<File> {
        val backupDir = getBackupDirectory()
        if (!backupDir.exists()) return emptyList()
        
        return backupDir.listFiles { file ->
            file.extension == "pbu"
        }?.toList() ?: emptyList()
    }
    
    override fun deleteBackupFile(fileName: String): Boolean {
        val file = File(getBackupDirectory(), fileName)
        return file.delete()
    }
    
    override fun copyFile(source: String, destination: File): Boolean {
        return try {
            val sourceFile = File(source)
            if (sourceFile.exists()) {
                destination.parentFile?.mkdirs()
                sourceFile.copyTo(destination, overwrite = true)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
