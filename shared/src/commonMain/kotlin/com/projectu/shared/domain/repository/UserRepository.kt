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
    suspend fun login(username: String, password: String): Result<String>
    
    /**
     * 登出
     */
    suspend fun logout(): Result<Unit>
    
    /**
     * 获取当前登录用户信息
     */
    suspend fun getCurrentUser(): Result<User?>
    
    /**
     * 根据ID获取用户信息
     */
    suspend fun getUserById(userId: String): Result<User>
    
    /**
     * 关注用户
     */
    suspend fun followUser(userId: String): Result<Unit>
    
    /**
     * 取消关注
     */
    suspend fun unfollowUser(userId: String): Result<Unit>
    
    /**
     * 获取关注列表
     */
    fun getFollowingUsers(): Flow<Result<List<User>>>
    
    /**
     * 检查是否已登录
     */
    suspend fun isLoggedIn(): Boolean
}

