package com.projectu.shared.data.repository

import com.projectu.shared.data.local.PixivConfig
import com.projectu.shared.data.local.PixivConfigStore
import com.projectu.shared.data.remote.api.PixivApi
import com.projectu.shared.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 认证仓储实现
 * 负责管理 Pixiv 登录凭据
 */
class AuthRepositoryImpl(
    private val pixivConfigStore: PixivConfigStore,
    private val pixivApi: PixivApi? = null  // 可选，用于验证凭据
) : AuthRepository {
    
    override fun observeLoginState(): Flow<Boolean> {
        return pixivConfigStore.config.map { it.isValid() }
    }
    
    override suspend fun isLoggedIn(): Boolean {
        return pixivConfigStore.getCurrentConfig().isValid()
    }
    
    override suspend fun saveCredentials(phpSessionId: String): Result<Unit> {
        return try {
            // 验证 PHPSESSID 格式
            if (!isValidPhpSessionId(phpSessionId)) {
                return Result.failure(IllegalArgumentException("PHPSESSID 格式无效，格式应为: userid_xxxxx"))
            }
            
            // 保存凭据
            pixivConfigStore.setPhpSessionId(phpSessionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun validateCredentials(): Result<Boolean> {
        return try {
            // 检查是否已登录
            if (!isLoggedIn()) {
                return Result.success(false)
            }
            
            // 如果有 PixivApi 实例，尝试调用 API 验证
            if (pixivApi != null) {
                try {
                    // 调用一个简单的 API 来验证凭据是否有效
                    // 例如获取当前用户信息
                    val config = pixivConfigStore.getCurrentConfig()
                    val userId = config.getUserId()
                    if (userId != null) {
                        pixivApi.userApi.getUserInfo(userId)
                        Result.success(true)
                    } else {
                        Result.success(false)
                    }
                } catch (e: Exception) {
                    // API 调用失败，可能是凭据过期
                    Result.success(false)
                }
            } else {
                // 没有 PixivApi，只检查格式
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun clearCredentials(): Result<Unit> {
        return try {
            pixivConfigStore.clear()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun observePixivConfig(): Flow<PixivConfig> {
        return pixivConfigStore.config
    }
    
    override suspend fun getPixivConfig(): PixivConfig {
        return pixivConfigStore.config.first()
    }
    
    override suspend fun getCurrentUserId(): Long? {
        return pixivConfigStore.getCurrentConfig().getUserId()
    }
    
    /**
     * 验证 PHPSESSID 格式
     * 格式: userid_xxxxx
     */
    private fun isValidPhpSessionId(phpSessionId: String): Boolean {
        if (phpSessionId.isBlank()) return false
        if (!phpSessionId.contains("_")) return false
        
        val parts = phpSessionId.split("_")
        if (parts.size < 2) return false
        
        // 验证第一部分是数字（用户ID）
        return parts[0].toLongOrNull() != null
    }
}
