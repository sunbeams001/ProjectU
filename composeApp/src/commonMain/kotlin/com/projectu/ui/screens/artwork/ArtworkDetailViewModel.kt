package com.projectu.ui.screens.artwork

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.FollowStatus
import com.projectu.shared.domain.repository.ArtworkRepository
import com.projectu.shared.domain.repository.UserRepository
import com.projectu.shared.domain.usecase.SyncArtworkStatesUseCase
import com.projectu.shared.data.cache.ArtworkCacheManager
import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.data.cache.StateCacheEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 作品详情页 ViewModel
 * 
 * 支持两种模式：
 * 1. 单个作品模式：只展示一个作品
 * 2. 列表导航模式：支持左右滑动浏览列表中的多个作品
 * 
 * 使用全局 ArtworkCacheManager 缓存作品详情，避免重复加载
 */
class ArtworkDetailViewModel(
    private val artworkRepository: ArtworkRepository,
    private val userRepository: UserRepository,
    private val syncArtworkStatesUseCase: SyncArtworkStatesUseCase,
    private val stateCacheManager: StateCacheManager,
    private val artworkCacheManager: ArtworkCacheManager
) : ScreenModel {

    private val _state = MutableStateFlow(ArtworkDetailState())
    val state: StateFlow<ArtworkDetailState> = _state.asStateFlow()
    
    // 本地会话缓存（用于当前详情页会话的快速访问）
    private val sessionCache = mutableMapOf<String, Artwork>()
    
    // 失败作品的错误信息缓存：artworkId -> errorMessage
    private val failedArtworkErrors = mutableMapOf<String, String>()
    
    // 加载更多回调
    private var onLoadMoreCallback: (() -> Unit)? = null

    init {
        // 监听全局状态变更
        screenModelScope.launch {
            stateCacheManager.stateChangeEvents.collect { event ->
                when (event) {
                    is StateCacheEvent.ArtworkBookmarkChanged -> {
                        // 如果是当前作品，更新状态
                        val currentArtwork = _state.value.artwork
                        if (currentArtwork?.id == event.artworkId) {
                            val updatedArtwork = currentArtwork.copy(
                                bookmarkStatus = event.status,
                                bookmarkId = event.bookmarkId
                            )
                            _state.update {
                                it.copy(artwork = updatedArtwork)
                            }
                            // 同步更新全局缓存
                            artworkCacheManager.updateArtwork(event.artworkId) {
                                it.copy(
                                    bookmarkStatus = event.status,
                                    bookmarkId = event.bookmarkId
                                )
                            }
                        }
                    }
                    is StateCacheEvent.UserFollowChanged -> {
                        // 如果是当前作品的作者，更新关注状态
                        val currentArtwork = _state.value.artwork
                        if (currentArtwork?.userId == event.userId) {
                            _state.update { it.copy(authorFollowStatus = event.status) }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * 初始化：列表导航模式
     */
    fun initWithArtworkList(
        artworkIds: List<String>, 
        initialIndex: Int,
        onLoadMore: (() -> Unit)? = null
    ) {
        onLoadMoreCallback = onLoadMore
        
        _state.update {
            it.copy(
                artworkIds = artworkIds,
                currentIndex = initialIndex.coerceIn(0, artworkIds.size - 1)
            )
        }
        
        // 加载初始作品
        if (initialIndex in artworkIds.indices) {
            loadArtworkDetail(artworkIds[initialIndex])
            // 预加载相邻作品
            preloadAdjacentArtworks(initialIndex)
        }
    }

    /**
     * 更新作品列表（用于动态更新，例如加载更多后）
     */
    fun updateArtworkList(newArtworkIds: List<String>) {
        val currentIds = _state.value.artworkIds
        // 只在列表真正增长时更新
        if (newArtworkIds.size > currentIds.size) {
            _state.update { it.copy(artworkIds = newArtworkIds) }
            // 预加载新增的相邻作品
            preloadAdjacentArtworks(_state.value.currentIndex)
        }
    }

    /**
     * 页面切换回调
     */
    fun onPageChanged(newIndex: Int) {
        val artworkIds = _state.value.artworkIds
        if (artworkIds.isEmpty() || newIndex !in artworkIds.indices) {
            return
        }
        
        _state.update { it.copy(currentIndex = newIndex) }
        
        // 从缓存加载或异步加载
        val artworkId = artworkIds[newIndex]
        val cachedError = failedArtworkErrors[artworkId]
        
        // 首先检查会话缓存
        val sessionCachedArtwork = sessionCache[artworkId]
        if (sessionCachedArtwork != null) {
            // 立即显示缓存的作品
            screenModelScope.launch {
                val followStatus = getAuthorFollowStatus(sessionCachedArtwork.userId)
                _state.update {
                    it.copy(
                        artwork = sessionCachedArtwork,
                        authorFollowStatus = followStatus,
                        isLoading = false,
                        error = null
                    )
                }
            }
        } else if (cachedError != null) {
            // 显示缓存的错误信息
            _state.update {
                it.copy(
                    artwork = null,
                    isLoading = false,
                    error = cachedError
                )
            }
        } else {
            // 尝试从全局缓存加载，或从网络加载
            loadArtworkDetail(artworkId)
        }
        
        // 预加载相邻作品
        preloadAdjacentArtworks(newIndex)
        
        // 检查是否需要加载更多（距离末尾还有5个时触发）
        if (newIndex >= artworkIds.size - 5) {
            onLoadMoreCallback?.invoke()
        }
    }

    /**
     * 加载作品详情
     * 
     * 加载策略：
     * 1. 首先检查全局缓存（ArtworkCacheManager）是否有已加载的详情
     * 2. 如果全局缓存命中且已加载详情，直接使用缓存数据
     * 3. 否则调用API加载详情，并缓存到全局缓存
     * 
     * @param artworkId 作品ID
     * @param silent 静默加载（预加载时使用，不显示加载状态）
     */
    fun loadArtworkDetail(artworkId: String, silent: Boolean = false) {
        // 检查会话缓存
        if (sessionCache.containsKey(artworkId)) {
            return
        }
        
        val currentArtwork = _state.value.artwork
        val isLoadingNow = _state.value.isLoading
        
        // 如果已经在加载，跳过
        if (isLoadingNow && !silent) {
            return
        }
        
        // 如果已有数据且ID相同，跳过
        if (currentArtwork?.id == artworkId && !silent) {
            return
        }
        
        screenModelScope.launch {
            // 先检查全局缓存是否有完整详情
            val globalCachedArtwork = artworkCacheManager.getDetailedArtwork(artworkId)
            if (globalCachedArtwork != null) {
                // 全局缓存命中，直接使用
                sessionCache[artworkId] = globalCachedArtwork
                
                if (!silent) {
                    val followStatus = getAuthorFollowStatus(globalCachedArtwork.userId)
                    _state.update {
                        it.copy(
                            artwork = globalCachedArtwork,
                            authorFollowStatus = followStatus,
                            isLoading = false,
                            error = null,
                            artworkCache = sessionCache.toMap()
                        )
                    }
                } else {
                    _state.update {
                        it.copy(artworkCache = sessionCache.toMap())
                    }
                }
                return@launch
            }
            
            // 全局缓存未命中，需要从网络加载
            if (!silent) {
                _state.update { it.copy(isLoading = true, error = null) }
            }

            try {
                val artworkIdLong = artworkId.toLongOrNull()
                if (artworkIdLong == null) {
                    if (!silent) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = "无效的作品ID"
                            )
                        }
                    }
                    return@launch
                }
                
                // 1. 获取作品详情
                var artwork = artworkRepository.getArtworkDetail(artworkIdLong).getOrThrow()

                // 2. 如果是多页作品，获取所有页面信息
                if (artwork.pageCount > 1) {
                    artwork = artworkRepository.getArtworkPages(artwork).getOrThrow()
                }

                // 3. 获取用户信息（包含关注状态和头像）
                var followStatus = FollowStatus.NOT_FOLLOWING
                try {
                    val userId = artwork.userId.toLongOrNull()
                    if (userId != null) {
                        val userInfo = userRepository.getUserById(userId).getOrNull()
                        if (userInfo != null) {
                            // 更新头像（如果为空）
                            if (artwork.userProfileImageUrl.isEmpty() && userInfo.profileImageUrl.isNotEmpty()) {
                                artwork = artwork.copy(userProfileImageUrl = userInfo.profileImageUrl)
                            }
                            // 获取关注状态并同步到缓存
                            followStatus = userInfo.followStatus
                            stateCacheManager.updateUserFollowStatus(artwork.userId, followStatus)
                        }
                    }
                } catch (e: Exception) {
                    // 不影响主流程，继续执行，使用缓存的关注状态
                    val userStates = stateCacheManager.getUserStates(listOf(artwork.userId))
                    followStatus = userStates[artwork.userId]?.followStatus ?: FollowStatus.NOT_FOLLOWING
                }

                // 4. 同步全局状态缓存（作品收藏状态）
                syncArtworkStatesUseCase(listOf(artwork))

                // 5. 添加到全局缓存（标记为已加载详情）
                artworkCacheManager.cacheArtworkDetail(artwork)
                
                // 6. 添加到会话缓存
                sessionCache[artworkId] = artwork

                // 7. 更新状态（仅当不是静默加载时）
                if (!silent) {
                    _state.update {
                        it.copy(
                            artwork = artwork,
                            authorFollowStatus = followStatus,
                            isLoading = false,
                            artworkCache = sessionCache.toMap()  // 更新缓存
                        )
                    }
                } else {
                    // 静默加载时也要更新缓存
                    _state.update {
                        it.copy(
                            artworkCache = sessionCache.toMap()
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val errorMessage = e.message ?: "加载失败"
                
                // 缓存错误信息（即使是静默加载也要缓存）
                failedArtworkErrors[artworkId] = errorMessage
                
                if (!silent) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                    }
                }
            }
        }
    }
    
    /**
     * 预加载相邻作品
     * 
     * 优化策略：
     * 1. 先检查会话缓存
     * 2. 再检查全局缓存是否有详情
     * 3. 都没有才发起网络请求
     */
    private fun preloadAdjacentArtworks(currentIndex: Int) {
        val artworkIds = _state.value.artworkIds
        
        screenModelScope.launch {
            // 预加载前一个
            if (currentIndex > 0) {
                val prevId = artworkIds[currentIndex - 1]
                if (!sessionCache.containsKey(prevId) && !artworkCacheManager.hasDetailLoaded(prevId)) {
                    loadArtworkDetail(prevId, silent = true)
                }
            }
            
            // 预加载后一个
            if (currentIndex < artworkIds.size - 1) {
                val nextId = artworkIds[currentIndex + 1]
                if (!sessionCache.containsKey(nextId) && !artworkCacheManager.hasDetailLoaded(nextId)) {
                    loadArtworkDetail(nextId, silent = true)
                }
            }
        }
    }
    
    /**
     * 获取作者关注状态
     */
    private suspend fun getAuthorFollowStatus(userId: String): FollowStatus {
        val userStates = stateCacheManager.getUserStates(listOf(userId))
        return userStates[userId]?.followStatus ?: FollowStatus.NOT_FOLLOWING
    }
    
    /**
     * 重试加载当前作品
     */
    fun retry() {
        val artworkIds = _state.value.artworkIds
        if (artworkIds.isNotEmpty()) {
            val currentId = artworkIds.getOrNull(_state.value.currentIndex)
            if (currentId != null) {
                // 清除缓存中的失败数据
                sessionCache.remove(currentId)
                failedArtworkErrors.remove(currentId)
                // 同时从全局缓存中移除，强制重新加载
                screenModelScope.launch {
                    artworkCacheManager.removeArtwork(currentId)
                    loadArtworkDetail(currentId, silent = false)
                }
            }
        } else {
            // 单个作品模式，从 state 中获取
            _state.value.artwork?.id?.let { artworkId ->
                sessionCache.remove(artworkId)
                failedArtworkErrors.remove(artworkId)
                // 同时从全局缓存中移除，强制重新加载
                screenModelScope.launch {
                    artworkCacheManager.removeArtwork(artworkId)
                    loadArtworkDetail(artworkId, silent = false)
                }
            }
        }
    }
    
    /**
     * 获取当前索引（用于返回时定位）
     */
    fun getCurrentIndex(): Int = _state.value.currentIndex
}

/**
 * 作品详情页状态
 * 
 * @param artwork 当前展示的作品
 * @param authorFollowStatus 作者关注状态
 * @param isLoading 是否正在加载
 * @param error 错误信息
 * @param artworkIds 作品ID列表（列表导航模式）
 * @param currentIndex 当前作品在列表中的索引
 * @param artworkCache 已加载的作品缓存（artworkId -> Artwork）
 */
data class ArtworkDetailState(
    val artwork: Artwork? = null,
    val authorFollowStatus: FollowStatus = FollowStatus.NOT_FOLLOWING,
    val isLoading: Boolean = false,
    val error: String? = null,
    val artworkIds: List<String> = emptyList(),
    val currentIndex: Int = 0,
    val artworkCache: Map<String, Artwork> = emptyMap()
)
