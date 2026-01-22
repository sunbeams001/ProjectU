package com.projectu.shared.domain.model

/**
 * 备份配置
 */
data class BackupConfig(
    /**
     * 要备份的模块
     */
    val modules: Set<BackupModule>,
    
    /**
     * 是否启用加密（暂不实现，预留）
     */
    val encryptionEnabled: Boolean = false,
    
    /**
     * 加密密码（暂不实现，预留）
     */
    val encryptionPassword: String? = null,
    
    /**
     * 用户备注
     */
    val comment: String? = null
) {
    companion object {
        /**
         * 创建P0模块的默认配置
         */
        fun createP0Config(comment: String? = null): BackupConfig {
            return BackupConfig(
                modules = BackupModule.getP0Modules(),
                encryptionEnabled = false,
                comment = comment
            )
        }
    }
}
