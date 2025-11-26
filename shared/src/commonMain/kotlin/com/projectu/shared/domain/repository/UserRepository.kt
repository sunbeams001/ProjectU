package com.projectu.shared.domain.repository

import com.projectu.shared.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * 用户仓储接口
 */
interface UserRepository {
    
    /**
     * 登录
     */
    suspend fun login(username: String, password: String): Result<User>
    
    /**
     * 登出
     */
    suspend fun logout(): Result<Unit>
    
    /**
     * 获取当前登录用户信息
     */
    suspend fun getCurrentUser(): Result<User>
    
    /**
     * 根据ID获取用户信息
     */
    suspend fun getUserById(userId: Long): Result<User>
    
    /**
     * 关注用户
     * @param userId 用户ID
     * @param restrict 关注类型："public"=公开关注, "private"=悄悄关注
     */
    suspend fun followUser(userId: Long, restrict: String = "public"): Result<Unit>
    
    /**
     * 取消关注
     */
    suspend fun unfollowUser(userId: Long): Result<Unit>
    
    /**
     * 观察当前用户（Flow版本）
     */
    fun observeCurrentUser(): Flow<User?>
    
    /**
     * 获取发现推荐用户列表
     * @param limit 返回数量（默认20）
     * @return 推荐用户列表
     */
    suspend fun getDiscoveryUsers(limit: Int = 20): Result<List<User>>
}

