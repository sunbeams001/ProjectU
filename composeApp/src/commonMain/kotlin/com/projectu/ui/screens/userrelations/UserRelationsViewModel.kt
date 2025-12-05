package com.projectu.ui.screens.userrelations

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.projectu.shared.data.cache.StateCacheEvent
import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.data.local.PixivConfigStore
import com.projectu.shared.domain.model.FollowStatus
import com.projectu.shared.domain.model.User
import com.projectu.shared.domain.repository.UserRepository
import com.projectu.shared.domain.usecase.SyncUserFollowDetailsUseCase
import com.projectu.shared.domain.usecase.SyncUserStatesUseCase
import com.projectu.ui.navigation.ArtworkListSource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 用户关系页面 ViewModel
 * 
 * 支持显示用户的关注列表、好P友列表和粉丝列表
 * 使用双层导航：一级导航（关注/好P友/粉丝）+ 二级导航（公开/私人，仅关注有）
 */
class UserRelationsViewModel(
    private val userRepository: UserRepository,
    private val pixivConfigStore: PixivConfigStore,
    private val syncUserStatesUseCase: SyncUserStatesUseCase,
    private val syncUserFollowDetailsUseCase: SyncUserFollowDetailsUseCase,
    private val stateCacheManager: StateCacheManager
) : ScreenModel {
    
    private val _state = MutableStateFlow(UserRelationsScreenState())
    val state: StateFlow<UserRelationsScreenState> = _state.asStateFlow()
    
    /**
     * 设置滚动位置（从详情页返回时调用）
     * 通过 State 更新来驱动 UI 滚动
     */
    fun setScrollPosition(pageKey: String, artworkIndex: Int) {
        // 计算目标用户索引
        val pageData = _state.value.pageDataCache[pageKey]
        if (pageData != null && pageData.users.isNotEmpty() && artworkIndex > 0) {
            var accumulatedArtworks = 0
            var targetUserIndex = 0
            for ((i, user) in pageData.users.withIndex()) {
                val userArtworkCount = user.illusts.size
                if (accumulatedArtworks + userArtworkCount > artworkIndex) {
                    targetUserIndex = i
                    break
                }
                accumulatedArtworks += userArtworkCount
                targetUserIndex = i + 1
            }
            val finalIndex = targetUserIndex.coerceAtMost(pageData.users.size - 1).coerceAtLeast(0)
            
            // 通过 State 更新触发滚动
            val newScrollTargets = _state.value.scrollTargets.toMutableMap()
            newScrollTargets[pageKey] = finalIndex
            
            _state.update {
                it.copy(scrollTargets = newScrollTargets)
            }
        }
    }
    
    /**
     * 清除滚动目标（滚动完成后调用）
     */
    fun clearScrollTarget(pageKey: String) {
        val newScrollTargets = _state.value.scrollTargets.toMutableMap()
        newScrollTargets.remove(pageKey)
        _state.update {
            it.copy(scrollTargets = newScrollTargets)
        }
    }
    
    init {
        // 监听全局状态变更事件
        screenModelScope.launch {
            stateCacheManager.stateChangeEvents.collect { event ->
                when (event) {
                    is StateCacheEvent.UserFollowChanged -> {
                        updateUserFollowStatus(event.userId, event.status)
                    }
                    else -> {}
                }
            }
        }
    }
    
    /**
     * 初始化页面（惰性初始化，已初始化则跳过）
     * @param userId 目标用户ID
     * @param userName 目标用户名
     * @param initialPage 初始页面
     */
    fun initialize(userId: String, userName: String, initialPage: RelationPage = RelationPage.FollowingPublic) {
        // 如果已经初始化（userId 相同），则跳过
        if (_state.value.userId == userId && _state.value.availablePages.isNotEmpty()) {
            return
        }
        
        screenModelScope.launch {
            val currentUserId = pixivConfigStore.getCurrentConfig().getUserId()?.toString()
            val isSelf = userId == currentUserId
            
            val availablePages = if (isSelf) {
                RelationPage.getAllPagesForSelf()
            } else {
                RelationPage.getAllPagesForOther()
            }
            
            _state.update {
                it.copy(
                    userId = userId,
                    userName = userName,
                    isSelf = isSelf,
                    currentUserId = currentUserId ?: "",
                    availablePages = availablePages,
                    currentPage = if (availablePages.contains(initialPage)) initialPage else availablePages.first()
                )
            }
            
            // 加载初始页面数据
            loadPageData(initialPage)
        }
    }
    
    /**
     * 切换页面
     */
    fun switchPage(page: RelationPage) {
        if (page == _state.value.currentPage) return
        
        _state.update { it.copy(currentPage = page) }
        
        // 如果该页面没有数据，则加载
        val pageData = _state.value.pageDataCache[page.key]
        if (pageData == null || (pageData.users.isEmpty() && !pageData.isLoading && pageData.error == null)) {
            loadPageData(page)
        }
    }
    
    /**
     * 加载更多
     */
    fun loadMore() {
        val currentPage = _state.value.currentPage
        val pageData = _state.value.pageDataCache[currentPage.key] ?: return
        
        if (pageData.isLoading || !pageData.hasMore) return
        
        loadPageData(currentPage, append = true)
    }
    
    /**
     * 刷新当前页面
     */
    fun refresh() {
        val currentPage = _state.value.currentPage
        _state.update { state ->
            state.copy(
                pageDataCache = state.pageDataCache + (currentPage.key to RelationPageData(isRefreshing = true))
            )
        }
        loadPageData(currentPage, refresh = true)
    }
    
    /**
     * 加载页面数据
     */
    private fun loadPageData(page: RelationPage, append: Boolean = false, refresh: Boolean = false) {
        val userId = _state.value.userId.toLongOrNull() ?: return
        val currentPageData = _state.value.pageDataCache[page.key] ?: RelationPageData()
        val offset = if (append) currentPageData.offset + currentPageData.users.size else 0
        
        // 更新加载状态
        _state.update { state ->
            state.copy(
                pageDataCache = state.pageDataCache + (page.key to currentPageData.copy(
                    isLoading = !append && !refresh,
                    isRefreshing = refresh,
                    error = null
                ))
            )
        }
        
        screenModelScope.launch {
            val result = when (page) {
                is RelationPage.FollowingPublic -> {
                    userRepository.getUserFollowing(userId, offset, 24, "show")
                }
                is RelationPage.FollowingPrivate -> {
                    userRepository.getUserFollowing(userId, offset, 24, "hide")
                }
                is RelationPage.MyPixiv -> {
                    userRepository.getMyPixiv(userId, offset, 24)
                }
                is RelationPage.Followers -> {
                    userRepository.getUserFollowers(userId, offset, 24)
                }
            }
            
            result
                .onSuccess { listResult ->
                    // 同步全局状态缓存
                    val syncedUsers = syncUserStatesUseCase(listResult.users)
                    // 精确同步关注状态
                    val detailedUsers = syncUserFollowDetailsUseCase(syncedUsers)
                    
                    _state.update { state ->
                        val existingData = state.pageDataCache[page.key] ?: RelationPageData()
                        val updatedUsers = if (append) {
                            val existingIds = existingData.users.map { it.id }.toSet()
                            val uniqueNewUsers = detailedUsers.filter { it.id !in existingIds }
                            existingData.users + uniqueNewUsers
                        } else {
                            detailedUsers
                        }
                        
                        state.copy(
                            pageDataCache = state.pageDataCache + (page.key to RelationPageData(
                                users = updatedUsers,
                                isLoading = false,
                                isRefreshing = false,
                                hasMore = listResult.hasMore,
                                error = null,
                                offset = if (append) existingData.offset else 0,
                                total = listResult.total
                            ))
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { state ->
                        val existingData = state.pageDataCache[page.key] ?: RelationPageData()
                        state.copy(
                            pageDataCache = state.pageDataCache + (page.key to existingData.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = error.message ?: "Unknown error"
                            ))
                        )
                    }
                }
        }
    }
    
    /**
     * 更新用户关注状态
     */
    private fun updateUserFollowStatus(userId: String, status: FollowStatus) {
        _state.update { state ->
            val updatedCache = state.pageDataCache.mapValues { (_, pageData) ->
                pageData.copy(
                    users = pageData.users.map { user ->
                        if (user.id == userId) {
                            user.copy(followStatus = status)
                        } else {
                            user
                        }
                    }
                )
            }
            state.copy(pageDataCache = updatedCache)
        }
    }
    
    /**
     * 创建指定页面的作品列表源（用于详情页列表导航）
     */
    fun createArtworkListSource(pageKey: String): ArtworkListSource {
        return object : ArtworkListSource {
            override val artworkIdsFlow: StateFlow<List<String>> = _state
                .map { state ->
                    val pageData = state.pageDataCache[pageKey] ?: RelationPageData()
                    pageData.users.flatMap { user -> user.illusts.map { it.id } }
                }
                .stateIn(
                    scope = screenModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = emptyList()
                )
            
            override fun loadMoreArtworks() {
                // 在用户关系页面，加载更多用户时会自动带来更多作品
                loadMore()
            }
        }
    }
}
