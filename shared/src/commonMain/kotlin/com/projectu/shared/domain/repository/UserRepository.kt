package com.projectu.shared.domain.repository

import com.projectu.shared.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * 用户关系列表结果
 */
data class UserListResult(
    val users: List<User>,
    val total: Int,
    val hasMore: Boolean
)

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
    
    /**
     * 获取用户关注列表
     * @param userId 用户ID
     * @param offset 偏移量
     * @param limit 数量限制
     * @param rest 公开状态：show(公开), hide(私人)
     * @return 用户列表结果（包含总数和是否有更多）
     */
    suspend fun getUserFollowing(
        userId: Long,
        offset: Int = 0,
        limit: Int = 24,
        rest: String = "show"
    ): Result<UserListResult>
    
    /**
     * 获取用户粉丝列表
     * @param userId 用户ID
     * @param offset 偏移量
     * @param limit 数量限制
     * @return 用户列表结果
     */
    suspend fun getUserFollowers(
        userId: Long,
        offset: Int = 0,
        limit: Int = 24
    ): Result<UserListResult>
    
    /**
     * 获取好P友列表
     * @param userId 用户ID
     * @param offset 偏移量
     * @param limit 数量限制
     * @return 用户列表结果
     */
    suspend fun getMyPixiv(
        userId: Long,
        offset: Int = 0,
        limit: Int = 24
    ): Result<UserListResult>
}

