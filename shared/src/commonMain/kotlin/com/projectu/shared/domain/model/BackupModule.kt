package com.projectu.shared.domain.model

/**
 * 备份模块枚举
 * P0级别：SETTINGS（应用设置）、CREDENTIALS（登录信息）
 */
enum class BackupModule(
    val displayNameKey: String,
    val descriptionKey: String,
    val priority: Int,
    val defaultSelected: Boolean,
    val estimatedSizeKey: String
) {
    SETTINGS(
        displayNameKey = "backup_module_settings",
        descriptionKey = "backup_module_settings_desc",
        priority = 0,
        defaultSelected = true,
        estimatedSizeKey = "backup_size_10kb"
    ),
    
    CREDENTIALS(
        displayNameKey = "backup_module_credentials",
        descriptionKey = "backup_module_credentials_desc",
        priority = 0,
        defaultSelected = true,
        estimatedSizeKey = "backup_size_5kb"
    );
    
    /**
     * 是否为敏感数据（需要加密）
     */
    val isSensitive: Boolean
        get() = this == CREDENTIALS
    
    companion object {
        /**
         * 获取所有P0模块
         */
        fun getP0Modules(): Set<BackupModule> {
            return setOf(SETTINGS, CREDENTIALS)
        }
    }
}
