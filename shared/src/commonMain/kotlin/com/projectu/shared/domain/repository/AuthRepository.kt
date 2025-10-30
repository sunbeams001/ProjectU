package com.projectu.shared.domain.repository

import com.projectu.shared.data.local.PixivConfig
import kotlinx.coroutines.flow.Flow

/**
 * 认证仓储接口
 * 负责管理 Pixiv 登录凭据的存储、验证和清除
 */
interface AuthRepository {
    
    /**
     * 观察登录状态
     * @return Flow<Boolean> 是否已登录
     */
    fun observeLoginState(): Flow<Boolean>
    
    /**
     * 检查是否已登录
     * @return Boolean 是否已登录
     */
    suspend fun isLoggedIn(): Boolean
    
    /**
     * 保存登录凭据 (PHPSESSID 方式)
     * @param phpSessionId PHPSESSID Cookie 值
     * @return Result<Unit> 保存结果
     */
    suspend fun saveCredentials(phpSessionId: String): Result<Unit>
    
    /**
     * 验证登录凭据是否有效
     * 通过调用 API 检查凭据是否仍然有效
     * @return Result<Boolean> 验证结果
     */
    suspend fun validateCredentials(): Result<Boolean>
    
    /**
     * 清除登录凭据（登出）
     * @return Result<Unit> 清除结果
     */
    suspend fun clearCredentials(): Result<Unit>
    
    /**
     * 获取当前 Pixiv 配置
     * @return Flow<PixivConfig> Pixiv 配置流
     */
    fun observePixivConfig(): Flow<PixivConfig>
    
    /**
     * 获取当前 Pixiv 配置（挂起函数）
     * @return PixivConfig 当前配置
     */
    suspend fun getPixivConfig(): PixivConfig
    
    /**
     * 获取当前用户 ID
     * @return Long? 用户 ID，未登录时返回 null
     */
    suspend fun getCurrentUserId(): Long?
}
