package com.projectu.shared.data.repository

import com.projectu.shared.data.remote.api.PixivApi
import com.projectu.shared.domain.model.User
import com.projectu.shared.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 用户仓储实现
 * 基于 Pixiv Web API 实现
 */
class UserRepositoryImpl(
    private val pixivApi: PixivApi
) : UserRepository {

    override suspend fun login(username: String, password: String): Result<User> {
        // Pixiv Web API 使用 cookie 认证，不支持直接登录
        // 需要用户在浏览器中登录后获取 PHPSESSID
        return Result.failure(UnsupportedOperationException("Web API 不支持直接登录，请使用 PHPSESSID"))
    }

    override suspend fun logout(): Result<Unit> {
        // Web API 不需要特殊的登出逻辑
        return Result.success(Unit)
    }

    override suspend fun getCurrentUser(): Result<User> = runCatching {
        // 使用当前用户ID获取用户信息
        val userId = pixivApi.client.userId
        val response = pixivApi.userApi.getUserInfo(userId)
        if (response.error) {
            throw IllegalStateException(response.message)
        }
        val body = response.body ?: throw IllegalStateException("用户信息为空")
        
        User(
            id = body.userId,  // userId 现在是 String 类型，无需转换
            name = body.name,
            account = body.userId,  // userId 现在是 String 类型，无需转换
            profileImageUrl = body.imageBig,
            isFollowed = false,
            isMuted = false,
            illusts = emptyList(),
            novels = emptyList()
        )
    }

    override suspend fun getUserById(userId: Long): Result<User> = runCatching {
        val response = pixivApi.userApi.getUserInfo(userId)
        if (response.error) {
            throw IllegalStateException(response.message)
        }
        val body = response.body ?: throw IllegalStateException("用户信息为空")
        
        User(
            id = body.userId,  // userId 现在是 String 类型，无需转换
            name = body.name,
            account = body.userId,  // userId 现在是 String 类型，无需转换
            profileImageUrl = body.imageBig,
            isFollowed = body.isFollowed,
            isMuted = body.isBlocking,
            illusts = emptyList(),
            novels = emptyList()
        )
    }

    override suspend fun followUser(userId: Long): Result<Unit> = runCatching {
        val response = pixivApi.userApi.followUser(userId)
        if (response.error) {
            throw IllegalStateException(response.message)
        }
    }

    override suspend fun unfollowUser(userId: Long): Result<Unit> = runCatching {
        val response = pixivApi.userApi.unfollowUser(userId)
        if (response.error) {
            throw IllegalStateException(response.message)
        }
    }

    override fun observeCurrentUser(): Flow<User?> = flow {
        val result = getCurrentUser()
        emit(result.getOrNull())
    }
}

