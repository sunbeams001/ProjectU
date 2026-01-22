package com.projectu.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * 备份元数据
 */
@Serializable
data class BackupMetadata(
    /**
     * 备份格式版本
     */
    val version: String,
    
    /**
     * 应用版本
     */
    val appVersion: String,
    
    /**
     * 数据库版本号
     */
    val databaseVersion: Int,
    
    /**
     * 备份时间戳
     */
    val timestamp: Long,
    
    /**
     * 平台
     */
    val platform: String,
    
    /**
     * 设备信息（可选）
     */
    val deviceInfo: DeviceInfo? = null,
    
    /**
     * 已备份的模块
     */
    val modules: List<String>,
    
    /**
     * 各模块数据大小（字节）
     */
    val moduleSizes: Map<String, Long>,
    
    /**
     * 加密信息
     */
    val encryption: EncryptionInfo,
    
    /**
     * 统计信息
     */
    val statistics: BackupStatistics,
    
    /**
     * 整体校验和
     */
    val checksum: String,
    
    /**
     * 用户备注
     */
    val comment: String? = null
)

@Serializable
data class DeviceInfo(
    val os: String,
    val model: String
)

@Serializable
data class EncryptionInfo(
    val enabled: Boolean,
    val algorithm: String? = null,
    val encryptedModules: List<String> = emptyList()
)

@Serializable
data class BackupStatistics(
    val totalRecords: Int
)
