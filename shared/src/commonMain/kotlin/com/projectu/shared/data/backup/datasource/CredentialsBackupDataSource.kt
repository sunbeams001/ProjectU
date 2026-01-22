package com.projectu.shared.data.backup.datasource

import com.projectu.shared.data.local.PixivConfigStore
import kotlinx.serialization.Serializable

/**
 * 登录凭据备份数据源
 */
class CredentialsBackupDataSource(
    private val pixivConfigStore: PixivConfigStore
) {
    
    /**
     * 导出凭据数据
     */
    suspend fun exportData(): CredentialsBackupData {
        val config = pixivConfigStore.getCurrentConfig()
        return CredentialsBackupData(
            phpSessionId = config.phpSessionId,
            csrfToken = config.csrfToken
        )
    }
    
    /**
     * 导入凭据数据
     */
    suspend fun importData(data: CredentialsBackupData) {
        pixivConfigStore.setPhpSessionId(data.phpSessionId)
        data.csrfToken?.let { pixivConfigStore.setCsrfToken(it) }
    }
}

/**
 * 凭据备份数据
 * 注意：该数据包含敏感信息，应当加密存储（未来实现）
 */
@Serializable
data class CredentialsBackupData(
    val phpSessionId: String,
    val csrfToken: String? = null
)
