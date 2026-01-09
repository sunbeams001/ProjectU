package com.projectu.shared.domain.usecase

import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.data.remote.api.PixivApi
import com.projectu.shared.domain.model.FollowStatus
import com.projectu.shared.domain.model.User
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * 同步用户关注详情用例
 * 
 * 用于精确获取已关注用户的关注状态（公开/悄悄关注）
 * 
 * 背景：
 * - Discovery接口返回的用户信息中只有isFollowed布尔值
 * - 无法区分公开关注和悄悄关注
 * - 使用getUserFollowDetail接口可以获取精确的关注类型
 * 
 * 性能优化：
 * - 只对已关注的用户调用接口（isFollowed=true）
 * - 未关注的用户不调用，减少性能开销
 * - 并发调用多个接口，提高效率
 */
class SyncUserFollowDetailsUseCase(
    private val pixivApi: PixivApi,
    private val stateCacheManager: StateCacheManager
) {
    /**
     * 批量同步用户关注详情
     * 
     * @param users 待同步的用户列表
     * @return 同步后的用户列表
     */
    suspend operator fun invoke(users: List<User>): List<User> = coroutineScope {
        // 1. 筛选出已关注的用户
        val followedUsers = users.filter { 
            it.followStatus != FollowStatus.NOT_FOLLOWING 
        }
        
        if (followedUsers.isEmpty()) {
            return@coroutineScope users
        }
        
        // 2. 并发调用getUserFollowDetail接口
        val detailResults = followedUsers.map { user ->
            async {
                try {
                    val response = pixivApi.userApi.getUserFollowDetail(user.id.toLong())
                    if (!response.error && response.body != null) {
                        val followStatus = when (response.body.restrict) {
                            "0" -> FollowStatus.PUBLIC
                            "1" -> FollowStatus.PRIVATE
                            else -> user.followStatus // 保持原状态
                        }
                        
                        // 更新全局缓存
                        stateCacheManager.updateUserFollowStatus(user.id, followStatus)
                        
                        user.id to followStatus
                    } else {
                        // 接口失败，保持原状态
                        user.id to user.followStatus
                    }
                } catch (e: Exception) {
                    // 异常情况，保持原状态
                    user.id to user.followStatus
                }
            }
        }.awaitAll()
        
        // 3. 构建userId到FollowStatus的映射
        val followStatusMap = detailResults.toMap()
        
        // 4. 更新用户列表
        users.map { user ->
            val updatedStatus = followStatusMap[user.id]
            if (updatedStatus != null && updatedStatus != user.followStatus) {
                user.copy(followStatus = updatedStatus)
            } else {
                user
            }
        }
    }
    
    /**
     * 同步单个用户的关注详情
     * 
     * @param user 待同步的用户
     * @return 同步后的用户对象
     */
    suspend operator fun invoke(user: User): User {
        // 未关注的用户直接返回
        if (user.followStatus == FollowStatus.NOT_FOLLOWING) {
            return user
        }
        
        return try {
            val response = pixivApi.userApi.getUserFollowDetail(user.id.toLong())
            if (!response.error && response.body != null) {
                val followStatus = when (response.body.restrict) {
                    "0" -> FollowStatus.PUBLIC
                    "1" -> FollowStatus.PRIVATE
                    else -> user.followStatus
                }
                
                // 更新全局缓存
                stateCacheManager.updateUserFollowStatus(user.id, followStatus)
                
                user.copy(followStatus = followStatus)
            } else {
                user
            }
        } catch (e: Exception) {
            user
        }
    }
}

