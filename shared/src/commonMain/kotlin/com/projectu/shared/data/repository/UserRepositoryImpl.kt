package com.projectu.shared.data.repository

import com.projectu.shared.data.remote.api.PixivApi
import com.projectu.shared.data.remote.mapper.toUser
import com.projectu.shared.data.remote.mapper.toUsersWithArtworks
import com.projectu.shared.domain.model.User
import com.projectu.shared.domain.repository.UserRepository
import com.projectu.shared.util.AgeLimitDeterminer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 用户仓储实现
 * 基于 Pixiv Web API 实现
 */
class UserRepositoryImpl(
    private val pixivApi: PixivApi,
    private val ageLimitDeterminer: AgeLimitDeterminer
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
        body.toUser()
    }

    override suspend fun getUserById(userId: Long): Result<User> = runCatching {
        val response = pixivApi.userApi.getUserInfo(userId)
        if (response.error) {
            throw IllegalStateException(response.message)
        }
        val body = response.body ?: throw IllegalStateException("用户信息为空")
        body.toUser()
    }

    override suspend fun followUser(userId: Long, restrict: String): Result<Unit> = runCatching {
        // 将字符串 restrict 转换为数字：public=0, private=1
        val restrictValue = if (restrict == "private") 1 else 0
        // 关注用户API返回空数组 [] 表示成功
        pixivApi.userApi.followUser(userId, restrict = restrictValue)
        // 如果没有抛出异常，说明成功
    }

    override suspend fun unfollowUser(userId: Long): Result<Unit> = runCatching {
        // 取消关注API返回对象表示成功
        pixivApi.userApi.unfollowUser(userId)
        // 如果没有抛出异常，说明成功
    }

    override fun observeCurrentUser(): Flow<User?> = flow {
        val result = getCurrentUser()
        emit(result.getOrNull())
    }
    
    override suspend fun getDiscoveryUsers(limit: Int): Result<List<User>> = runCatching {
        val response = pixivApi.userApi.getDiscoveryUsers(limit)
        if (response.error) {
            throw IllegalStateException(response.message)
        }
        val body = response.body ?: throw IllegalStateException("发现用户数据为空")
        body.toUsersWithArtworks(ageLimitDeterminer)
    }
}

