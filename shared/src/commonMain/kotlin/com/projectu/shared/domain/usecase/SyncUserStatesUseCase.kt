package com.projectu.shared.domain.usecase

import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.domain.model.User
import kotlinx.coroutines.flow.first

/**
 * 同步用户关注状态用例
 *
 * 将从API获取的用户列表与全局状态缓存中的关注状态进行同步
 */
class SyncUserStatesUseCase(
    private val stateCacheManager: StateCacheManager
) {
    /**
     * 同步单个用户的关注状态
     * @param user 待同步的用户
     * @return 同步后的用户对象
     */
    suspend operator fun invoke(user: User): User {
        val cachedState = stateCacheManager.getUserState(user.id).first()
        return if (cachedState != null) {
            user.copy(followStatus = cachedState.followStatus)
        } else {
            user
        }
    }

    /**
     * 批量同步用户关注状态
     * @param users 待同步的用户列表
     * @return 同步后的用户列表
     */
    suspend operator fun invoke(users: List<User>): List<User> {
        return users.map { user ->
            val cachedState = stateCacheManager.getUserState(user.id).first()
            if (cachedState != null) {
                user.copy(followStatus = cachedState.followStatus)
            } else {
                user
            }
        }
    }
}

