package com.projectu.shared.domain.usecase

import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.domain.repository.UserRepository

/**
 * 取消关注用户用例
 *
 * 执行取消关注操作并自动更新全局状态缓存
 */
class UnfollowUserUseCase(
    private val userRepository: UserRepository,
    private val stateCacheManager: StateCacheManager
) {
    /**
     * 执行取消关注用户
     *
     * @param userId 用户ID
     * @return Result<Unit> 成功返回Unit，失败返回异常
     */
    suspend operator fun invoke(userId: Long): Result<Unit> {
        val userIdStr = userId.toString()

        val result = userRepository.unfollowUser(userId)

        return result.onSuccess {
            // 自动更新全局缓存
            stateCacheManager.unfollowUser(userIdStr)
        }
    }
}

