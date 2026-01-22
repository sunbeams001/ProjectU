package com.projectu.shared.domain.model

/**
 * 备份模块枚举
 * P0级别：SETTINGS（应用设置）、CREDENTIALS（登录信息）
 * P1级别：BLOCK_RULES（屏蔽列表）、BROWSE_HISTORY（浏览历史）、DOWNLOAD_RULES（下载路径规则）
 * P2级别：DOWNLOAD_TASKS（下载任务记录）、SEARCH_HISTORY（搜索历史）
 */
enum class BackupModule(
    val displayNameKey: String,
    val descriptionKey: String,
    val priority: Int,
    val defaultSelected: Boolean,
    val estimatedSizeKey: String
) {
    // P0模块
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
    ),
    
    // P1模块
    BLOCK_RULES(
        displayNameKey = "backup_module_block_rules",
        descriptionKey = "backup_module_block_rules_desc",
        priority = 1,
        defaultSelected = true,
        estimatedSizeKey = "backup_size_50kb"
    ),
    
    BROWSE_HISTORY(
        displayNameKey = "backup_module_browse_history",
        descriptionKey = "backup_module_browse_history_desc",
        priority = 1,
        defaultSelected = false,  // 占用空间大，默认不选中
        estimatedSizeKey = "backup_size_10mb"
    ),
    
    DOWNLOAD_RULES(
        displayNameKey = "backup_module_download_rules",
        descriptionKey = "backup_module_download_rules_desc",
        priority = 1,
        defaultSelected = true,
        estimatedSizeKey = "backup_size_20kb"
    ),
    
    // P2模块
    DOWNLOAD_TASKS(
        displayNameKey = "backup_module_download_tasks",
        descriptionKey = "backup_module_download_tasks_desc",
        priority = 2,
        defaultSelected = false,
        estimatedSizeKey = "backup_size_50mb"
    ),
    
    SEARCH_HISTORY(
        displayNameKey = "backup_module_search_history",
        descriptionKey = "backup_module_search_history_desc",
        priority = 2,
        defaultSelected = true,
        estimatedSizeKey = "backup_size_10kb"
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
        
        /**
         * 获取所有P1模块
         */
        fun getP1Modules(): Set<BackupModule> {
            return setOf(BLOCK_RULES, BROWSE_HISTORY, DOWNLOAD_RULES)
        }
        
        /**
         * 获取所有P2模块
         */
        fun getP2Modules(): Set<BackupModule> {
            return setOf(DOWNLOAD_TASKS, SEARCH_HISTORY)
        }
        
        /**
         * 获取P0+P1所有模块
         */
        fun getP0AndP1Modules(): Set<BackupModule> {
            return getP0Modules() + getP1Modules()
        }
        
        /**
         * 获取所有模块（P0+P1+P2）
         */
        fun getAllModules(): Set<BackupModule> {
            return entries.toSet()
        }
        
        /**
         * 获取默认选中的模块
         */
        fun getDefaultSelectedModules(): Set<BackupModule> {
            return entries.filter { it.defaultSelected }.toSet()
        }
    }
}
