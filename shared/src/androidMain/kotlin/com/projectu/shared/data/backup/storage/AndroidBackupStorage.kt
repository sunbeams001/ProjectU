package com.projectu.shared.data.backup.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream

/**
 * Android平台备份存储实现
 * 
 * 存储策略：
 * - Android 10+ (API 29+): 优先使用SAF授权的目录，否则使用MediaStore.Downloads API
 * - Android 7-9 (API 24-28): 使用传统文件系统
 * 
 * 权限要求：
 * - Android 10+: SAF目录授权（推荐）或无需权限（MediaStore）
 * - Android 7-9: 需要 WRITE_EXTERNAL_STORAGE 权限
 */
class AndroidBackupStorage(
    private val context: Context
) : BackupStorage {
    
    companion object {
        private const val TAG = "AndroidBackupStorage"
        private const val PREFS_NAME = "backup_storage_prefs"
        private const val KEY_BACKUP_DIR_URI = "backup_directory_uri"
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    override fun createTempDirectory(prefix: String): File {
        val tempDir = File(context.cacheDir, prefix)
        tempDir.mkdirs()
        return tempDir
    }
    
    override fun saveBackupFile(sourceFile: File, fileName: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveBackupFileMediaStore(sourceFile, fileName)
        } else {
            saveBackupFileLegacy(sourceFile, fileName)
        }
    }
    
    /**
     * Android 10+: 使用 MediaStore.Downloads API
     */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun saveBackupFileMediaStore(sourceFile: File, fileName: String): String {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/ProjectU/Backups")
        }
        
        val uri = context.contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: throw IllegalStateException("Failed to create MediaStore entry for backup")
        
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            sourceFile.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw IllegalStateException("Failed to open output stream")
        
        // 返回用户友好的路径显示
        return "Downloads/ProjectU/Backups/$fileName"
    }
    
    /**
     * Android 7-9: 使用传统文件系统
     * 需要 WRITE_EXTERNAL_STORAGE 权限
     */
    private fun saveBackupFileLegacy(sourceFile: File, fileName: String): String {
        val backupDir = getBackupDirectoryLegacy()
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        
        val destFile = File(backupDir, fileName)
        sourceFile.copyTo(destFile, overwrite = true)
        
        return destFile.absolutePath
    }
    
    override fun getBackupDirectory(): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 返回虚拟路径（实际文件通过MediaStore访问）
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ProjectU/Backups")
        } else {
            getBackupDirectoryLegacy()
        }
    }
    
    private fun getBackupDirectoryLegacy(): File {
        // Android 7-9: 使用 Downloads 目录（与Android 10+保持一致）
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "ProjectU/Backups"
        )
    }
    
    override fun listBackupFiles(): List<File> {
        Log.d(TAG, "listBackupFiles: Android SDK=${Build.VERSION.SDK_INT}")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+: 优先使用SAF授权的目录
            val backupDirUri = getBackupDirectoryUri()
            if (backupDirUri != null) {
                Log.d(TAG, "listBackupFiles: Using SAF directory")
                return listBackupFilesFromSAF(backupDirUri)
            } else {
                Log.d(TAG, "listBackupFiles: No SAF directory set, using MediaStore")
                return listBackupFilesMediaStore()
            }
        } else {
            // Android 7-9: 直接读取文件系统
            Log.d(TAG, "listBackupFiles: Using legacy file system for Android <10")
            return listBackupFilesLegacy()
        }
    }
    
    /**
     * 从SAF授权的目录列出备份文件
     */
    private fun listBackupFilesFromSAF(treeUriString: String): List<File> {
        Log.d(TAG, "listBackupFilesFromSAF: treeUri=$treeUriString")
        val backupFiles = mutableListOf<File>()
        
        try {
            val treeUri = Uri.parse(treeUriString)
            val docFile = DocumentFile.fromTreeUri(context, treeUri)
            
            if (docFile == null || !docFile.exists() || !docFile.isDirectory) {
                Log.w(TAG, "listBackupFilesFromSAF: Invalid directory")
                return emptyList()
            }
            
            Log.d(TAG, "listBackupFilesFromSAF: Directory is valid, listing files")
            val files = docFile.listFiles()
            Log.d(TAG, "listBackupFilesFromSAF: Found ${files.size} items")
            
            for (file in files) {
                if (file.isFile && file.name?.endsWith(".pbu.zip", ignoreCase = true) == true) {
                    Log.d(TAG, "listBackupFilesFromSAF: Found backup: ${file.name} (size=${file.length()})")
                    // 创建一个特殊的File对象，absolutePath存储content URI
                    val backupFile = SAFBackupFile(file.name ?: "unknown", file.uri.toString(), file.length())
                    backupFiles.add(backupFile)
                }
            }
            
            Log.d(TAG, "listBackupFilesFromSAF: Returning ${backupFiles.size} backup files")
        } catch (e: Exception) {
            Log.e(TAG, "listBackupFilesFromSAF: Error", e)
        }
        
        return backupFiles
    }
    
    /**
     * 特殊的File子类，用于表示SAF DocumentFile
     */
    private class SAFBackupFile(
        private val fileName: String,
        private val contentUri: String,
        private val size: Long
    ) : File(fileName) {
        override fun getAbsolutePath(): String = contentUri
        override fun exists(): Boolean = true
        override fun length(): Long = size
        override fun getName(): String = fileName
    }
    
    /**
     * Android 10+: 通过 MediaStore 查询备份文件
     * 返回的File对象包含可用于访问文件的content URI
     */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun listBackupFilesMediaStore(): List<File> {
        Log.d(TAG, "listBackupFilesMediaStore: Starting MediaStore query")
        
        // 首先尝试查询所有.pbu文件来调试路径格式
        debugQueryAllPbuFiles()
        
        val backupFiles = mutableListOf<File>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.RELATIVE_PATH
        )
        
        // 尝试多种路径格式
        val pathVariants = listOf(
            "${Environment.DIRECTORY_DOWNLOADS}/ProjectU/Backups/",  // Download/ProjectU/Backups/
            "Download/ProjectU/Backups/",                             // 直接使用Download
            "Download/ProjectU/Backups",                              // 无尾部斜杠
            "${Environment.DIRECTORY_DOWNLOADS}/ProjectU/Backups"     // Download/ProjectU/Backups
        )
        
        Log.d(TAG, "listBackupFilesMediaStore: Trying ${pathVariants.size} path variants")
        
        for (pathVariant in pathVariants) {
            val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf(pathVariant, "%.pbu")
            
            Log.d(TAG, "listBackupFilesMediaStore: Query selection=${selection}")
            Log.d(TAG, "listBackupFilesMediaStore: Query args=${selectionArgs.joinToString()}")
            
            try {
                context.contentResolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
                )?.use { cursor ->
                    Log.d(TAG, "listBackupFilesMediaStore: Query with path='$pathVariant' returned ${cursor.count} rows")
                    
                    if (cursor.count > 0) {
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                        val dataColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                        val pathColumn = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                        
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idColumn)
                            val fileName = cursor.getString(nameColumn)
                            val fileSize = cursor.getLong(sizeColumn)
                            
                            // 尝试获取真实路径（在某些设备上可能可用）
                            val realPath = if (dataColumn >= 0) {
                                cursor.getString(dataColumn)
                            } else {
                                null
                            }
                            
                            val relativePath = if (pathColumn >= 0) {
                                cursor.getString(pathColumn)
                            } else {
                                null
                            }
                            
                            Log.d(TAG, "listBackupFilesMediaStore: Found backup file: $fileName (size=$fileSize, realPath=$realPath, relativePath=$relativePath)")
                            
                            // 如果有真实路径且文件存在，使用真实路径
                            if (realPath != null && File(realPath).exists()) {
                                Log.d(TAG, "listBackupFilesMediaStore: Using real path for $fileName")
                                backupFiles.add(File(realPath))
                            } else {
                                // 否则创建一个带有content URI的虚拟File对象
                                // 文件名格式: content://media/external/downloads/[id]
                                val contentUri = android.content.ContentUris.withAppendedId(
                                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                                    id
                                )
                                Log.d(TAG, "listBackupFilesMediaStore: Using content URI for $fileName: $contentUri")
                                // 创建一个特殊的File对象，其absolutePath包含content URI
                                val file = MediaStoreBackupFile(fileName, contentUri.toString(), fileSize)
                                backupFiles.add(file)
                            }
                        }
                        // 找到文件后就退出循环
                        break
                    }
                } ?: run {
                    Log.w(TAG, "listBackupFilesMediaStore: Query with path='$pathVariant' returned null cursor")
                }
            } catch (e: Exception) {
                Log.e(TAG, "listBackupFilesMediaStore: Error querying MediaStore with path='$pathVariant'", e)
            }
        }
        
        // 如果MediaStore查询失败，尝试直接访问文件系统（作为后备方案）
        if (backupFiles.isEmpty()) {
            Log.w(TAG, "listBackupFilesMediaStore: MediaStore returned no files, trying direct file access as fallback")
            backupFiles.addAll(tryDirectFileAccess())
        }
        
        Log.d(TAG, "listBackupFilesMediaStore: Returning ${backupFiles.size} backup files")
        return backupFiles
    }
    
    /**
     * 调试方法：查询所有.pbu.zip文件并打印路径信息
     */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun debugQueryAllPbuFiles() {
        try {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.RELATIVE_PATH,
                MediaStore.MediaColumns.DATA
            )
            
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%.pbu.zip")
            
            Log.d(TAG, "debugQueryAllPbuFiles: Searching for all .pbu.zip files in MediaStore")
            
            context.contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                Log.d(TAG, "debugQueryAllPbuFiles: Found ${cursor.count} .pbu files in MediaStore")
                
                if (cursor.count > 0) {
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val pathColumn = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                    val dataColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    
                    while (cursor.moveToNext()) {
                        val fileName = cursor.getString(nameColumn)
                        val relativePath = if (pathColumn >= 0) cursor.getString(pathColumn) else "N/A"
                        val dataPath = if (dataColumn >= 0) cursor.getString(dataColumn) else "N/A"
                        Log.d(TAG, "debugQueryAllPbuFiles: - File: $fileName")
                        Log.d(TAG, "debugQueryAllPbuFiles:   Relative Path: '$relativePath'")
                        Log.d(TAG, "debugQueryAllPbuFiles:   Data Path: '$dataPath'")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "debugQueryAllPbuFiles: Error", e)
        }
    }
    
    /**
     * 后备方案：尝试直接访问文件系统（即使在Android 10+上）
     * 注意：这可能在某些设备上失败或需要特殊权限
     */
    private fun tryDirectFileAccess(): List<File> {
        Log.d(TAG, "tryDirectFileAccess: Attempting direct file system access")
        
        // 记录Environment常量的实际值
        Log.d(TAG, "tryDirectFileAccess: DIRECTORY_DOWNLOADS constant = '${Environment.DIRECTORY_DOWNLOADS}'")
        Log.d(TAG, "tryDirectFileAccess: ExternalStorageDirectory = '${Environment.getExternalStorageDirectory().absolutePath}'")
        Log.d(TAG, "tryDirectFileAccess: ExternalStoragePublicDirectory(DOWNLOADS) = '${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath}'")
        
        val backupDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "ProjectU/Backups"
        )
        
        Log.d(TAG, "tryDirectFileAccess: Backup directory path: ${backupDir.absolutePath}")
        Log.d(TAG, "tryDirectFileAccess: Directory exists: ${backupDir.exists()}")
        Log.d(TAG, "tryDirectFileAccess: Directory canRead: ${backupDir.canRead()}")
        Log.d(TAG, "tryDirectFileAccess: Directory isDirectory: ${backupDir.isDirectory}")
        
        if (!backupDir.exists() || !backupDir.canRead()) {
            Log.w(TAG, "tryDirectFileAccess: Cannot access backup directory")
            return emptyList()
        }
        
        // 检查父目录
        val projectUDir = backupDir.parentFile
        if (projectUDir != null) {
            Log.d(TAG, "tryDirectFileAccess: Parent directory (ProjectU): ${projectUDir.absolutePath}")
            Log.d(TAG, "tryDirectFileAccess: Parent exists: ${projectUDir.exists()}")
            val parentFiles = projectUDir.listFiles()
            Log.d(TAG, "tryDirectFileAccess: Parent directory contents: ${if (parentFiles == null) "null" else "${parentFiles.size} items"}")
            if (parentFiles != null) {
                parentFiles.forEach { f ->
                    Log.d(TAG, "tryDirectFileAccess:   Parent item: ${f.name} (isDir=${f.isDirectory})")
                }
            }
        }
        
        // 首先列出目录中的所有文件（用于调试）
        val allFiles = backupDir.listFiles()
        Log.d(TAG, "tryDirectFileAccess: listFiles() returned: ${if (allFiles == null) "null" else "array with ${allFiles.size} items"}")
        
        if (allFiles != null && allFiles.isNotEmpty()) {
            Log.d(TAG, "tryDirectFileAccess: All files in directory:")
            allFiles.forEach { file ->
                Log.d(TAG, "tryDirectFileAccess:   - ${file.name} (isFile=${file.isFile}, extension='${file.extension}', size=${file.length()})")
            }
        }
        
        // 尝试直接访问我们知道应该存在的文件
        val testFile = File(backupDir, "backup_20260121_145500.pbu")
        Log.d(TAG, "tryDirectFileAccess: Test specific file: ${testFile.absolutePath}")
        Log.d(TAG, "tryDirectFileAccess: Test file exists: ${testFile.exists()}")
        Log.d(TAG, "tryDirectFileAccess: Test file canRead: ${testFile.canRead()}")
        if (testFile.exists()) {
            Log.d(TAG, "tryDirectFileAccess: Test file size: ${testFile.length()}")
        }
        
        // 然后过滤.pbu文件（使用不同的方式）
        val pbuFiles = if (allFiles != null) {
            allFiles.filter { file ->
                file.isFile && file.extension.equals("pbu", ignoreCase = true)
            }
        } else {
            emptyList()
        }
        
        Log.d(TAG, "tryDirectFileAccess: Found ${pbuFiles.size} .pbu files via direct access")
        pbuFiles.forEach { file ->
            Log.d(TAG, "tryDirectFileAccess: - ${file.name} (size=${file.length()})")
        }
        
        // 如果找不到文件但测试文件存在，说明是权限问题
        if (pbuFiles.isEmpty() && testFile.exists() && !testFile.canRead()) {
            Log.w(TAG, "tryDirectFileAccess: Files exist but cannot be read due to Scoped Storage restrictions")
            Log.w(TAG, "tryDirectFileAccess: These files were likely created by another app or file manager")
            Log.w(TAG, "tryDirectFileAccess: Solution: Use backup creation through this app, or use file picker to grant access")
        }
        
        return pbuFiles
    }
    
    /**
     * 特殊的File子类，用于表示MediaStore中的文件
     * absolutePath返回content URI以便后续处理
     */
    private class MediaStoreBackupFile(
        private val fileName: String,
        private val contentUri: String,
        private val size: Long
    ) : File(fileName) {
        override fun getAbsolutePath(): String = contentUri
        override fun exists(): Boolean = true
        override fun length(): Long = size
    }
    
    /**
     * Android 7-9: 直接读取文件系统
     */
    private fun listBackupFilesLegacy(): List<File> {
        val backupDir = getBackupDirectoryLegacy()
        Log.d(TAG, "listBackupFilesLegacy: Backup directory=${backupDir.absolutePath}")
        Log.d(TAG, "listBackupFilesLegacy: Directory exists=${backupDir.exists()}")
        
        if (!backupDir.exists()) {
            Log.w(TAG, "listBackupFilesLegacy: Backup directory does not exist")
            return emptyList()
        }
        
        val files = backupDir.listFiles { file ->
            file.extension == "pbu"
        }?.toList() ?: emptyList()
        
        Log.d(TAG, "listBackupFilesLegacy: Found ${files.size} backup files")
        files.forEach { file ->
            Log.d(TAG, "listBackupFilesLegacy: - ${file.name} (size=${file.length()})")
        }
        
        return files
    }
    
    override fun deleteBackupFile(fileName: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            deleteBackupFileMediaStore(fileName)
        } else {
            deleteBackupFileLegacy(fileName)
        }
    }
    
    /**
     * 从URI复制文件到临时目录
     * 支持content:// URI (文件选择器选择的文件)
     */
    override fun copyFile(source: String, destination: File): Boolean {
        return copyFromUri(source, destination)
    }
    
    /**
     * 内部方法：从URI复制文件
     */
    private fun copyFromUri(uri: String, destinationFile: File): Boolean {
        return try {
            if (uri.startsWith("content://")) {
                // 从content URI读取
                val contentUri = Uri.parse(uri)
                context.contentResolver.openInputStream(contentUri)?.use { inputStream ->
                    destinationFile.parentFile?.mkdirs()
                    FileOutputStream(destinationFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                true
            } else {
                // 普通文件路径
                val sourceFile = File(uri)
                if (sourceFile.exists()) {
                    sourceFile.copyTo(destinationFile, overwrite = true)
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Android 10+: 通过 MediaStore 删除
     */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun deleteBackupFileMediaStore(fileName: String): Boolean {
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(
            "${Environment.DIRECTORY_DOWNLOADS}/ProjectU/Backups/",
            fileName
        )
        
        val deleted = context.contentResolver.delete(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            selection,
            selectionArgs
        )
        
        return deleted > 0
    }
    
    /**
     * Android 7-9: 直接删除文件
     */
    private fun deleteBackupFileLegacy(fileName: String): Boolean {
        val file = File(getBackupDirectoryLegacy(), fileName)
        return file.delete()
    }
    
    override fun setBackupDirectoryUri(treeUri: String): Boolean {
        return try {
            Log.d(TAG, "setBackupDirectoryUri: $treeUri")
            val uri = Uri.parse(treeUri)
            
            // 持久化URI权限
            val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            
            // 保存URI到SharedPreferences
            prefs.edit().putString(KEY_BACKUP_DIR_URI, treeUri).apply()
            Log.d(TAG, "setBackupDirectoryUri: Success")
            true
        } catch (e: Exception) {
            Log.e(TAG, "setBackupDirectoryUri: Error", e)
            false
        }
    }
    
    override fun getBackupDirectoryUri(): String? {
        return prefs.getString(KEY_BACKUP_DIR_URI, null)
    }
    
    override fun hasBackupDirectoryAccess(): Boolean {
        val uriString = getBackupDirectoryUri() ?: return false
        
        return try {
            val uri = Uri.parse(uriString)
            val docFile = DocumentFile.fromTreeUri(context, uri)
            docFile != null && docFile.exists() && docFile.canRead()
        } catch (e: Exception) {
            Log.e(TAG, "hasBackupDirectoryAccess: Error", e)
            false
        }
    }
}
