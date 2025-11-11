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
     */
    suspend fun followUser(userId: Long): Result<Unit>
    
    /**
     * 取消关注
     */
    suspend fun unfollowUser(userId: Long): Result<Unit>
    
    /**
     * 观察当前用户（Flow版本）
     */
    fun observeCurrentUser(): Flow<User?>
}

