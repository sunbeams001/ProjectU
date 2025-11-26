package com.projectu.shared.domain.usecase

import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.domain.model.FollowStatus
import com.projectu.shared.domain.repository.UserRepository

/**
 * 关注用户用例
 *
 * 执行关注操作并自动更新全局状态缓存
 */
class FollowUserUseCase(
    private val userRepository: UserRepository,
    private val stateCacheManager: StateCacheManager
) {
    /**
     * 执行关注用户
     *
     * @param userId 用户ID
     * @param isPrivate 是否悄悄关注（true=悄悄关注，false=公开关注）
     * @return Result<Unit> 成功返回Unit，失败返回异常
     */
    suspend operator fun invoke(userId: Long, isPrivate: Boolean): Result<Unit> {
        val userIdStr = userId.toString()
        val restrict = if (isPrivate) "private" else "public"

        val result = userRepository.followUser(userId, restrict)

        return result.onSuccess {
            // 自动更新全局缓存
            stateCacheManager.followUser(userIdStr, isPrivate)
        }
    }
}

