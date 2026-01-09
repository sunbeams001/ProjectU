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
import com.projectu.shared.domain.usecase.TranslateTextUseCase
import com.projectu.shared.data.local.SettingsCache
import com.projectu.shared.domain.model.TranslationLanguage
import com.projectu.shared.domain.model.TranslationEngine
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
    private val novelCacheManager: NovelCacheManager,
    private val translateTextUseCase: TranslateTextUseCase,
    private val settingsCache: SettingsCache,
    private val novelTranslationCacheRepository: com.projectu.shared.domain.repository.NovelTranslationCacheRepository
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
        
        _state.update { 
            it.copy(
                currentIndex = newIndex, 
                currentPage = 1,
                translatedDescription = null,
                isTranslating = false
            ) 
        }
        
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
                    
                    // 加载翻译缓存
                    loadTranslationCache(novelId)
                    
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
                
                // 加载翻译缓存
                loadTranslationCache(novelId)
                
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
                val errorMessage = e.message ?: "Load failed"
                
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
            val newPage = currentState.currentPage + 1
            _state.update { it.copy(currentPage = newPage) }
            
            // 翻页后检查新页面的翻译状态
            checkPageTranslationState(newPage)
        }
    }

    /**
     * 翻到上一页
     */
    fun previousPage() {
        val currentState = _state.value
        if (currentState.canGoPrevious) {
            val newPage = currentState.currentPage - 1
            _state.update { it.copy(currentPage = newPage) }
            
            // 翻页后检查新页面的翻译状态
            checkPageTranslationState(newPage)
        }
    }

    /**
     * 跳转到指定页
     */
    fun goToPage(page: Int) {
        val currentState = _state.value
        if (page in 1..currentState.totalPages) {
            _state.update { it.copy(currentPage = page) }
            
            // 翻页后检查新页面的翻译状态
            checkPageTranslationState(page)
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
     * 保存当前页面的滚动位置
     */
    fun saveScrollPosition(page: Int, firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) {
        _state.update { 
            it.copy(
                pageScrollPositions = it.pageScrollPositions + (page to (firstVisibleItemIndex to firstVisibleItemScrollOffset))
            )
        }
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
     * 
     * 根据当前书签状态执行不同操作：
     * - 未添加书签：添加当前页书签
     * - 已添加书签且是当前页：删除书签
     * - 已添加书签但不是当前页：更新书签到当前页（先删除再添加）
     */
    fun toggleMarker() {
        val novel = _state.value.novel ?: return
        val markerStatus = _state.value.markerStatus
        
        when (markerStatus) {
            MarkerStatus.NO_MARKER -> {
                // 未添加书签，直接添加
                addMarker()
            }
            MarkerStatus.MARKER_CURRENT_PAGE -> {
                // 已添加书签且是当前页，删除书签
                deleteMarker()
            }
            MarkerStatus.MARKER_OTHER_PAGE -> {
                // 已添加书签但不是当前页，更新书签（先删除再添加）
                updateMarker()
            }
        }
    }
    
    /**
     * 更新书签到当前页
     * 先删除旧书签，再添加新书签
     */
    private fun updateMarker() {
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
                
                // 先删除旧书签
                novelRepository.deleteNovelMarker(
                    novelId = novel.id.toLong(),
                    userId = userId
                ).onSuccess {
                    // 再添加新书签
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
                }.onFailure {
                    _state.update { it.copy(isMarkerLoading = false) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isMarkerLoading = false) }
            }
        }
    }
    
    /**
     * 获取当前索引（用于返回时定位）
     */
    fun getCurrentIndex(): Int = _state.value.currentIndex
    
    /**
     * 翻译小说简介
     */
    fun translateDescription() {
        val currentNovel = _state.value.novel ?: return
        val description = currentNovel.description
        
        if (description.isBlank()) return
        if (!settingsCache.isTranslationEnabled()) return
        
        screenModelScope.launch {
            _state.update { it.copy(isTranslating = true) }
            
            try {
                val plainText = description.trim()
                
                val result = translateTextUseCase(
                    text = plainText,
                    targetLanguage = settingsCache.getTranslationTargetLanguage(),
                    engine = settingsCache.getTranslationEngine()
                )
                
                result.onSuccess { translation ->
                    _state.update {
                        it.copy(
                            translatedDescription = translation.translatedText,
                            isTranslating = false
                        )
                    }
                }.onFailure { _ ->
                    _state.update { it.copy(isTranslating = false) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isTranslating = false) }
            }
        }
    }
    
    /**
     * 清除翻译结果
     */
    fun clearTranslation() {
        _state.update { 
            it.copy(
                translatedDescription = null,
                isTranslating = false
            ) 
        }
    }
    
    /**
     * 切换显示模式
     * 
     * @param mode 新的显示模式
     */
    fun switchDisplayMode(mode: NovelDisplayMode) {
        _state.update { it.copy(displayMode = mode) }
        
        // 如果切换到翻译或对照模式，触发当前页面的翻译
        if (mode != NovelDisplayMode.ORIGINAL) {
            translateCurrentPageIfNeeded()
        }
    }
    
    /**
     * 检查页面翻译状态，并根据情况调整显示模式
     * 
     * @param pageNumber 页码
     */
    private fun checkPageTranslationState(pageNumber: Int) {
        val currentState = _state.value
        val displayMode = currentState.displayMode
        
        // 如果不在翻译相关模式下，无需检查
        if (displayMode == NovelDisplayMode.ORIGINAL) {
            return
        }
        
        val hasTranslation = currentState.pageTranslations.containsKey(pageNumber)
        val isTranslating = currentState.translatingPages.contains(pageNumber)
        
        when {
            // 如果页面已有翻译，保持当前显示模式
            hasTranslation -> {
                // 保持当前显示模式
            }
            // 如果页面正在翻译，保持当前显示模式
            isTranslating -> {
                // 保持当前显示模式
            }
            // 如果页面没有翻译且不在翻译中，触发翻译
            else -> {
                translateCurrentPageIfNeeded()
            }
        }
    }
    
    /**
     * 翻译当前页面（若需要）
     */
    private fun translateCurrentPageIfNeeded() {
        val state = _state.value
        val currentPage = state.currentPage
        
        // 如果当前页面已有翻译或正在翻译，则不重复翻译
        if (state.pageTranslations.containsKey(currentPage) || 
            state.translatingPages.contains(currentPage)) {
            return
        }
        
        translatePage(currentPage)
    }
    
    /**
     * 翻译指定页面
     * 
     * @param pageNumber 页码（从1开始）
     */
    private fun translatePage(pageNumber: Int) {
        val state = _state.value
        val novel = state.novel ?: return
        val page = state.parsedPages.getOrNull(pageNumber - 1) ?: return
        
        if (!settingsCache.isTranslationEnabled()) {
            return
        }
        if (page.content.isBlank()) {
            return
        }
        
        screenModelScope.launch {
            val targetLanguage = settingsCache.getTranslationTargetLanguage()
            val engine = settingsCache.getTranslationEngine()
            
            // 先检查缓存
            val cachedTranslation = novelTranslationCacheRepository.getTranslation(
                novelId = novel.id,
                pageIndex = pageNumber - 1, // 缓存使用0-based索引
                targetLanguage = targetLanguage.code
            )
            
            if (cachedTranslation != null) {
                // 使用缓存的翻译
                _state.update {
                    it.copy(
                        pageTranslations = it.pageTranslations + (pageNumber to cachedTranslation)
                    )
                }
                return@launch
            }
            
            // 缓存未命中，开始翻译
            _state.update { 
                it.copy(
                    translatingPages = it.translatingPages + pageNumber,
                    pageTranslations = it.pageTranslations - pageNumber // 清空旧翻译（如果有）
                ) 
            }
            
            try {
                // 检查文本长度，决定是否分块
                val maxChunkSize = 4500
                
                if (page.content.length <= maxChunkSize) {
                    // 短文本，直接翻译
                    val result = translateTextUseCase(
                        text = page.content,
                        targetLanguage = targetLanguage,
                        engine = engine
                    )
                    
                    result.onSuccess { translation ->
                        val translatedText = translation.translatedText
                        
                        // 保存到缓存
                        novelTranslationCacheRepository.saveTranslation(
                            novelId = novel.id,
                            pageIndex = pageNumber - 1,
                            originalContent = page.content,
                            translatedContent = translatedText,
                            targetLanguage = targetLanguage.code,
                            engine = engine.name
                        )
                        
                        // 更新状态
                        _state.update {
                            it.copy(
                                pageTranslations = it.pageTranslations + (pageNumber to translatedText),
                                translatingPages = it.translatingPages - pageNumber
                            )
                        }
                    }.onFailure { error ->
                        error.printStackTrace()
                        _state.update {
                            it.copy(translatingPages = it.translatingPages - pageNumber)
                        }
                    }
                } else {
                    // 长文本，分块翻译并增量显示
                    translatePageIncrementally(novel.id, pageNumber, page.content, targetLanguage, engine)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update {
                    it.copy(translatingPages = it.translatingPages - pageNumber)
                }
            }
        }
    }
    
    /**
     * 分块翻译页面，每块翻译完成后立即显示
     * 
     * @param novelId 小说ID
     * @param pageNumber 页码（从1开始）
     * @param content 页面内容
     * @param targetLanguage 目标语言
     * @param engine 翻译引擎
     */
    private suspend fun translatePageIncrementally(
        novelId: String,
        pageNumber: Int,
        content: String,
        targetLanguage: TranslationLanguage,
        engine: TranslationEngine
    ) {
        try {
            // 将文本分块
            val chunks = splitTextIntoChunks(content, 4500)
            
            val translatedChunks = mutableListOf<String>()
            
            // 逐个翻译每个块
            chunks.forEachIndexed { index, chunk ->
                val result = translateTextUseCase(
                    text = chunk,
                    targetLanguage = targetLanguage,
                    engine = engine
                )
                
                result.onSuccess { translation ->
                    val translatedChunk = translation.translatedText
                    translatedChunks.add(translatedChunk)
                    
                    // 每翻译完一块，立即更新UI显示
                    val partialTranslation = translatedChunks.joinToString("")
                    
                    _state.update {
                        it.copy(
                            pageTranslations = it.pageTranslations + (pageNumber to partialTranslation)
                        )
                    }
                }.onFailure { error ->
                    error.printStackTrace()
                    // 出错时停止翻译
                    _state.update {
                        it.copy(translatingPages = it.translatingPages - pageNumber)
                    }
                    return
                }
                
                // 延迟以避免API限流
                if (index < chunks.size - 1) {
                    kotlinx.coroutines.delay(500)
                }
            }
            
            // 所有块翻译完成，保存到缓存
            val fullTranslation = translatedChunks.joinToString("")
            
            novelTranslationCacheRepository.saveTranslation(
                novelId = novelId,
                pageIndex = pageNumber - 1,
                originalContent = content,
                translatedContent = fullTranslation,
                targetLanguage = targetLanguage.code,
                engine = engine.toString()
            )
            
            // 移除翻译中标记
            _state.update {
                it.copy(translatingPages = it.translatingPages - pageNumber)
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            _state.update {
                it.copy(translatingPages = it.translatingPages - pageNumber)
            }
        }
    }
    
    /**
     * 将文本分块
     */
    private fun splitTextIntoChunks(text: String, maxChunkSize: Int): List<String> {
        if (text.length <= maxChunkSize) {
            return listOf(text)
        }
        
        val chunks = mutableListOf<String>()
        var currentChunk = StringBuilder()
        
        // 按句子分割（简单处理：按句号、问号、感叹号等分割）
        val sentences = text.split(Regex("(?<=[。！？\\.\\!\\?\\n])"))
        
        for (sentence in sentences) {
            if (sentence.isBlank()) continue
            
            // 如果单个句子就超过限制，需要强制分割
            if (sentence.length > maxChunkSize) {
                // 先保存当前块
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString())
                    currentChunk.clear()
                }
                
                // 强制按字符数分割长句
                var remaining = sentence
                while (remaining.length > maxChunkSize) {
                    chunks.add(remaining.substring(0, maxChunkSize))
                    remaining = remaining.substring(maxChunkSize)
                }
                if (remaining.isNotEmpty()) {
                    currentChunk.append(remaining)
                }
                continue
            }
            
            // 检查加入这个句子后是否超过限制
            if (currentChunk.length + sentence.length > maxChunkSize) {
                // 保存当前块并开始新块
                chunks.add(currentChunk.toString())
                currentChunk.clear()
            }
            
            currentChunk.append(sentence)
        }
        
        // 添加最后一块
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString())
        }
        
        return chunks
    }
    
    /**
     * 清除当前小说的翻译缓存
     */
    fun clearTranslationCache() {
        val novel = _state.value.novel ?: return
        
        screenModelScope.launch {
            try {
                novelTranslationCacheRepository.clearNovelCache(novel.id)
                _state.update {
                    it.copy(
                        pageTranslations = emptyMap(),
                        displayMode = NovelDisplayMode.ORIGINAL
                    )
                }
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    /**
     * 重新翻译当前页面
     * 清除当前页的翻译缓存并重新翻译
     */
    fun retranslateCurrentPage() {
        val state = _state.value
        val novel = state.novel ?: return
        val currentPage = state.currentPage
        
        screenModelScope.launch {
            try {
                // 清除当前页的缓存
                novelTranslationCacheRepository.clearPageCache(
                    novelId = novel.id,
                    pageIndex = currentPage - 1,
                    targetLanguage = settingsCache.getTranslationTargetLanguage().code
                )
                
                // 从状态中移除当前页的翻译
                _state.update {
                    it.copy(
                        pageTranslations = it.pageTranslations - currentPage
                    )
                }
                
                // 如果当前在翻译模式下，触发重新翻译
                if (state.displayMode != NovelDisplayMode.ORIGINAL) {
                    translatePage(currentPage)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 加载小说的所有翻译缓存
     */
    private suspend fun loadTranslationCache(novelId: String) {
        try {
            val targetLanguage = settingsCache.getTranslationTargetLanguage()
            val translations = novelTranslationCacheRepository.getNovelTranslations(
                novelId = novelId,
                targetLanguage = targetLanguage.code
            )
            
            // 将0-based索引转换为1-based页码
            val pageTranslations = translations.mapKeys { it.key + 1 }
            
            _state.update {
                it.copy(pageTranslations = pageTranslations)
            }
        } catch (e: Exception) {
            // 加载缓存失败，忽略错误
        }
    }
}

