package com.projectu.shared.data.backup.storage

import java.io.File

/**
 * 备份存储接口
 */
interface BackupStorage {
    
    /**
     * 创建临时目录
     */
    fun createTempDirectory(prefix: String): File
    
    /**
     * 保存备份文件到目标位置
     * @param sourceFile 源文件
     * @param fileName 文件名
     * @return 最终文件路径
     */
    fun saveBackupFile(sourceFile: File, fileName: String): String
    
    /**
     * 获取备份目录
     */
    fun getBackupDirectory(): File
    
    /**
     * 列出所有备份文件
     */
    fun listBackupFiles(): List<File>
    
    /**
     * 删除备份文件
     */
    fun deleteBackupFile(fileName: String): Boolean
    
    /**
     * 从URI或路径复制文件到目标位置
     * @param source URI或文件路径
     * @param destination 目标文件
     * @return 是否成功
     */
    fun copyFile(source: String, destination: File): Boolean
    
    /**
     * 设置用户选择的备份目录URI（Android 10+使用SAF）
     * @param treeUri 目录的tree URI
     * @return 是否成功设置
     */
    fun setBackupDirectoryUri(treeUri: String): Boolean
    
    /**
     * 获取当前备份目录的URI（如果已设置）
     */
    fun getBackupDirectoryUri(): String?
    
    /**
     * 检查是否已授权备份目录访问
     */
    fun hasBackupDirectoryAccess(): Boolean
}
