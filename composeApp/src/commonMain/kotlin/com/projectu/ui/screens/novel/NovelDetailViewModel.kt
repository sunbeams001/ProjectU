package com.projectu.ui.screens.novel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.projectu.shared.data.cache.NovelCacheManager
import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.data.cache.StateCacheEvent
import com.projectu.shared.domain.model.FollowStatus
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.repository.AuthRepository
import com.projectu.shared.domain.repository.NovelRepository
import com.projectu.shared.domain.repository.UserRepository
import com.projectu.shared.domain.usecase.SyncNovelStatesUseCase
import com.projectu.ui.util.NovelContentParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 小说详情页 ViewModel
 * 
 * 支持两种模式：
 * 1. 单个小说模式：只展示一部小说
 * 2. 列表导航模式：支持左右滑动浏览列表中的多部小说
 * 
 * 使用全局 NovelCacheManager 缓存小说详情，避免重复加载
 */
class NovelDetailViewModel(
    private val novelRepository: NovelRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val syncNovelStatesUseCase: SyncNovelStatesUseCase,
    private val stateCacheManager: StateCacheManager,
    private val novelCacheManager: NovelCacheManager
) : ScreenModel {

    private val _state = MutableStateFlow(NovelDetailState())
    val state: StateFlow<NovelDetailState> = _state.asStateFlow()
    
    // 本地会话缓存（用于当前详情页会话的快速访问）
    private val sessionCache = mutableMapOf<String, Novel>()
    
    // 失败小说的错误信息缓存
    private val failedNovelErrors = mutableMapOf<String, String>()
    
    // 加载更多回调
    private var onLoadMoreCallback: (() -> Unit)? = null

    init {
        // 监听全局状态变更
        screenModelScope.launch {
            stateCacheManager.stateChangeEvents.collect { event ->
                when (event) {
                    is StateCacheEvent.NovelBookmarkChanged -> {
                        val currentNovel = _state.value.novel
                        if (currentNovel?.id == event.novelId) {
                            val updatedNovel = currentNovel.copy(
                                bookmarkStatus = event.status,
                                bookmarkId = event.bookmarkId
                            )
                            _state.update { it.copy(novel = updatedNovel) }
                            // 同步更新全局缓存
                            novelCacheManager.updateNovel(event.novelId) {
                                it.copy(
                                    bookmarkStatus = event.status,
                                    bookmarkId = event.bookmarkId
                                )
                            }
                        }
                    }
                    is StateCacheEvent.UserFollowChanged -> {
                        val currentNovel = _state.value.novel
                        if (currentNovel?.userId == event.userId) {
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
    fun initWithNovelList(
        novelIds: List<String>,
        initialIndex: Int,
        onLoadMore: (() -> Unit)? = null
    ) {
        onLoadMoreCallback = onLoadMore
        
        _state.update {
            it.copy(
                novelIds = novelIds,
                currentIndex = initialIndex.coerceIn(0, novelIds.size - 1)
            )
        }
        
        // 加载初始小说
        if (initialIndex in novelIds.indices) {
            loadNovelDetail(novelIds[initialIndex])
            // 预加载相邻小说
            preloadAdjacentNovels(initialIndex)
        }
    }

    /**
     * 更新小说列表（用于动态更新）
     */
    fun updateNovelList(newNovelIds: List<String>) {
        val currentIds = _state.value.novelIds
        if (newNovelIds.size > currentIds.size) {
            _state.update { it.copy(novelIds = newNovelIds) }
            preloadAdjacentNovels(_state.value.currentIndex)
        }
    }

    /**
     * 页面切换回调（列表导航模式）
     */
    fun onListIndexChanged(newIndex: Int) {
        val novelIds = _state.value.novelIds
        if (novelIds.isEmpty() || newIndex !in novelIds.indices) {
            return
        }
        
        _state.update { it.copy(currentIndex = newIndex, currentPage = 1) }
        
        val novelId = novelIds[newIndex]
        val cachedError = failedNovelErrors[novelId]
        
        val sessionCachedNovel = sessionCache[novelId]
        if (sessionCachedNovel != null) {
            screenModelScope.launch {
                val followStatus = getAuthorFollowStatus(sessionCachedNovel.userId)
                val pages = parseNovelContent(sessionCachedNovel)
                _state.update {
                    it.copy(
                        novel = sessionCachedNovel,
                        authorFollowStatus = followStatus,
                        parsedPages = pages,
                        isLoading = false,
                        error = null
                    )
                }
            }
        } else if (cachedError != null) {
            _state.update {
                it.copy(
                    novel = null,
                    isLoading = false,
                    error = cachedError
                )
            }
        } else {
            loadNovelDetail(novelId)
        }
        
        preloadAdjacentNovels(newIndex)
        
        // 检查是否需要加载更多
        if (newIndex >= novelIds.size - 5) {
            onLoadMoreCallback?.invoke()
        }
    }

    /**
     * 加载小说详情
     * 
     * 加载策略：
     * 1. 首先检查全局缓存（NovelCacheManager）是否有已加载的详情
     * 2. 如果全局缓存命中且已加载详情，直接使用缓存数据
     * 3. 否则调用API加载详情，并缓存到全局缓存
     * 
     * @param novelId 小说ID
     * @param silent 静默加载（预加载时使用，不显示加载状态）
     */
    fun loadNovelDetail(novelId: String, silent: Boolean = false) {
        // 检查会话缓存
        if (sessionCache.containsKey(novelId)) {
            return
        }
        
        val currentNovel = _state.value.novel
        val isLoadingNow = _state.value.isLoading
        
        if (isLoadingNow && !silent) {
            return
        }
        
        if (currentNovel?.id == novelId && !silent) {
            return
        }
        
        screenModelScope.launch {
            // 先检查全局缓存是否有完整详情
            val globalCachedNovel = novelCacheManager.getDetailedNovel(novelId)
            if (globalCachedNovel != null) {
                // 全局缓存命中，直接使用
                sessionCache[novelId] = globalCachedNovel
                
                if (!silent) {
                    val followStatus = getAuthorFollowStatus(globalCachedNovel.userId)
                    val pages = parseNovelContent(globalCachedNovel)
                    _state.update {
                        it.copy(
                            novel = globalCachedNovel,
                            authorFollowStatus = followStatus,
                            parsedPages = pages,
                            currentPage = 1,
                            isLoading = false,
                            error = null,
                            currentNovelId = novelId,
                            novelCache = sessionCache.toMap()
                        )
                    }
                } else {
                    _state.update {
                        it.copy(novelCache = sessionCache.toMap())
                    }
                }
                return@launch
            }
            
            // 全局缓存未命中，需要从网络加载
            if (!silent) {
                _state.update { it.copy(isLoading = true, error = null, currentNovelId = novelId) }
            } else {
                _state.update { it.copy(currentNovelId = novelId) }
            }

            try {
                // 获取小说详情
                var novel = novelRepository.getNovelDetail(novelId).getOrThrow()
                
                // 同步全局状态缓存
                syncNovelStatesUseCase(listOf(novel))
                
                // 获取作者信息（关注状态和头像）
                var followStatus = FollowStatus.NOT_FOLLOWING
                try {
                    val userId = novel.userId.toLongOrNull()
                    if (userId != null) {
                        val userInfo = userRepository.getUserById(userId).getOrNull()
                        if (userInfo != null) {
                            followStatus = userInfo.followStatus
                            stateCacheManager.updateUserFollowStatus(novel.userId, followStatus)
                            // 更新用户头像URL（API详情接口不返回）
                            if (novel.userProfileImageUrl.isEmpty() && userInfo.profileImageUrl.isNotEmpty()) {
                                novel = novel.copy(userProfileImageUrl = userInfo.profileImageUrl)
                            }
                        }
                    }
                } catch (e: Exception) {
                    val userStates = stateCacheManager.getUserStates(listOf(novel.userId))
                    followStatus = userStates[novel.userId]?.followStatus ?: FollowStatus.NOT_FOLLOWING
                }
                
                // 解析内容
                val pages = parseNovelContent(novel)
                
                // 添加到会话缓存
                sessionCache[novelId] = novel
                
                // 缓存到全局缓存
                novelCacheManager.cacheNovelDetail(novel)
                
                if (!silent) {
                    _state.update {
                        it.copy(
                            novel = novel,
                            authorFollowStatus = followStatus,
                            parsedPages = pages,
                            currentPage = 1,
                            isLoading = false,
                            novelCache = sessionCache.toMap()
                        )
                    }
                } else {
                    _state.update {
                        it.copy(novelCache = sessionCache.toMap())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val errorMessage = e.message ?: "加载失败"
                
                failedNovelErrors[novelId] = errorMessage
                
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
     * 解析小说内容
     */
    private fun parseNovelContent(novel: Novel): List<NovelContentParser.NovelPage> {
        val content = novel.content ?: return listOf(
            NovelContentParser.NovelPage(1, "", emptyList())
        )
        return NovelContentParser.parsePages(content)
    }
    
    /**
     * 预加载相邻小说
     */
    private fun preloadAdjacentNovels(currentIndex: Int) {
        val novelIds = _state.value.novelIds
        
        screenModelScope.launch {
            // 预加载前一个
            if (currentIndex > 0) {
                val prevId = novelIds[currentIndex - 1]
                if (!sessionCache.containsKey(prevId)) {
                    loadNovelDetail(prevId, silent = true)
                }
            }
            
            // 预加载后一个
            if (currentIndex < novelIds.size - 1) {
                val nextId = novelIds[currentIndex + 1]
                if (!sessionCache.containsKey(nextId)) {
                    loadNovelDetail(nextId, silent = true)
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
     * 翻到下一页
     */
    fun nextPage() {
        val currentState = _state.value
        if (currentState.canGoNext) {
            _state.update { it.copy(currentPage = it.currentPage + 1) }
        }
    }

    /**
     * 翻到上一页
     */
    fun previousPage() {
        val currentState = _state.value
        if (currentState.canGoPrevious) {
            _state.update { it.copy(currentPage = it.currentPage - 1) }
        }
    }

    /**
     * 跳转到指定页
     */
    fun goToPage(page: Int) {
        val currentState = _state.value
        if (page in 1..currentState.totalPages) {
            _state.update { it.copy(currentPage = page) }
        }
    }

    /**
     * 切换信息区域展开/收起
     */
    fun toggleInfoExpanded() {
        _state.update { it.copy(isInfoExpanded = !it.isInfoExpanded) }
    }

    /**
     * 设置信息区域展开状态
     */
    fun setInfoExpanded(expanded: Boolean) {
        _state.update { it.copy(isInfoExpanded = expanded) }
    }
    
    /**
     * 重试加载当前小说
     */
    fun retry() {
        val state = _state.value
        val novelIds = state.novelIds
        
        // 优先从列表模式获取当前ID
        val currentId = if (novelIds.isNotEmpty()) {
            novelIds.getOrNull(state.currentIndex)
        } else {
            // 单个小说模式，使用 currentNovelId 或 novel.id
            state.currentNovelId ?: state.novel?.id
        }
        
        if (currentId != null) {
            sessionCache.remove(currentId)
            failedNovelErrors.remove(currentId)
            loadNovelDetail(currentId, silent = false)
        }
    }
    
    /**
     * 添加阅读书签（稍后再读标记）
     * 保存当前阅读位置
     */
    fun addMarker() {
        val novel = _state.value.novel ?: return
        val currentPage = _state.value.currentPage
        
        screenModelScope.launch {
            _state.update { it.copy(isMarkerLoading = true) }
            
            try {
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _state.update { it.copy(isMarkerLoading = false) }
                    return@launch
                }
                
                novelRepository.addNovelMarker(
                    novelId = novel.id.toLong(),
                    userId = userId,
                    page = currentPage
                ).onSuccess {
                    // 更新本地状态
                    val updatedNovel = novel.copy(marker = currentPage)
                    _state.update { 
                        it.copy(
                            novel = updatedNovel,
                            isMarkerLoading = false
                        )
                    }
                    sessionCache[novel.id] = updatedNovel
                    // 更新全局缓存
                    novelCacheManager.updateNovel(novel.id) { it.copy(marker = currentPage) }
                }.onFailure {
                    _state.update { it.copy(isMarkerLoading = false) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isMarkerLoading = false) }
            }
        }
    }
    
    /**
     * 删除阅读书签
     */
    fun deleteMarker() {
        val novel = _state.value.novel ?: return
        
        screenModelScope.launch {
            _state.update { it.copy(isMarkerLoading = true) }
            
            try {
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _state.update { it.copy(isMarkerLoading = false) }
                    return@launch
                }
                
                novelRepository.deleteNovelMarker(
                    novelId = novel.id.toLong(),
                    userId = userId
                ).onSuccess {
                    // 更新本地状态
                    val updatedNovel = novel.copy(marker = null)
                    _state.update { 
                        it.copy(
                            novel = updatedNovel,
                            isMarkerLoading = false
                        )
                    }
                    sessionCache[novel.id] = updatedNovel
                    // 更新全局缓存
                    novelCacheManager.updateNovel(novel.id) { it.copy(marker = null) }
                }.onFailure {
                    _state.update { it.copy(isMarkerLoading = false) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isMarkerLoading = false) }
            }
        }
    }
    
    /**
     * 切换书签状态
     */
    fun toggleMarker() {
        val novel = _state.value.novel ?: return
        if (novel.marker != null) {
            deleteMarker()
        } else {
            addMarker()
        }
    }
    
    /**
     * 获取当前索引（用于返回时定位）
     */
    fun getCurrentIndex(): Int = _state.value.currentIndex
}
