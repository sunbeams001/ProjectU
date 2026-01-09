package com.projectu.ui.screens.search

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.projectu.shared.data.local.SearchHistoryStore
import com.projectu.shared.data.remote.api.SearchApi
import com.projectu.shared.data.remote.api.TagApi
import com.projectu.shared.data.remote.dto.illust.IllustSimple
import com.projectu.shared.data.remote.dto.novel.NovelSimple
import com.projectu.shared.data.remote.mapper.toArtworkList
import com.projectu.shared.data.remote.mapper.toNovelList
import com.projectu.shared.data.remote.mapper.toUsersWithArtworks
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.Tag
import com.projectu.shared.domain.model.User
import com.projectu.shared.util.AgeLimitDeterminer
import com.projectu.shared.util.TagTranslationUtil
import com.projectu.ui.navigation.ArtworkListSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 搜索结果页面 ViewModel
 */
class SearchResultViewModel(
    initialKeyword: String,
    private val searchApi: SearchApi,
    private val tagApi: TagApi,
    private val searchHistoryStore: SearchHistoryStore,
    private val tagTranslationUtil: TagTranslationUtil,
    private val ageLimitDeterminer: AgeLimitDeterminer,
    private val userStateManager: com.projectu.shared.domain.manager.UserStateManager
) : ScreenModel {
    
    private val _state = MutableStateFlow(
        SearchResultState(
            searchKeyword = TextFieldValue(
                text = initialKeyword,
                selection = TextRange(initialKeyword.length)
            )
        )
    )
    val state: StateFlow<SearchResultState> = _state.asStateFlow()
    
    private var autocompleteJob: Job? = null
    
    init {
        // 从全局UserStateManager获取用户会员状态（无需查询网络）
        screenModelScope.launch {
            userStateManager.currentUser.collect { user ->
                val isPremium = user?.isPremium == true
                _state.update { currentState ->
                    // 首次设置会员状态时，同时更新默认排序
                    if (currentState.isPremiumUser != isPremium) {
                        currentState.copy(
                            isPremiumUser = isPremium,
                            illustParams = IllustSearchParams.createDefault(isPremium),
                            novelParams = NovelSearchParams.createDefault(isPremium)
                        )
                    } else {
                        currentState.copy(isPremiumUser = isPremium)
                    }
                }
            }
        }
        
        // 执行初始搜索
        searchCurrentCategory()
    }
    
    /**
     * 创建绑定到插画搜索结果的 ArtworkListSource
     * 
     * 用于作品详情页的列表导航功能。当用户点击插画搜索结果中的作品时，
     * 创建一个绑定插画结果的列表源，使详情页可以响应式地获取列表更新。
     * 
     * @return 绑定到插画搜索结果的 ArtworkListSource
     */
    fun createArtworkListSource(): ArtworkListSource {
        return object : ArtworkListSource {
            override val artworkIdsFlow: StateFlow<List<String>> = state.map { currentState ->
                currentState.illustResults.map { it.id }
            }.stateIn(
                scope = screenModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = state.value.illustResults.map { it.id }
            )
            
            override fun loadMoreArtworks() {
                loadMore()
            }
        }
    }
    
    /**
     * 创建绑定到用户搜索结果中所有作品的 ArtworkListSource
     * 
     * 用于用户tab下点击作品时的列表导航。将用户列表中所有用户的作品展开为一个作品列表。
     * 
     * @return 绑定到用户搜索结果中所有作品的 ArtworkListSource
     */
    fun createUserArtworkListSource(): ArtworkListSource {
        return object : ArtworkListSource {
            override val artworkIdsFlow: StateFlow<List<String>> = state.map { currentState ->
                currentState.userResults.flatMap { user ->
                    user.illusts?.map { it.id } ?: emptyList()
                }
            }.stateIn(
                scope = screenModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = state.value.userResults.flatMap { user ->
                    user.illusts?.map { it.id } ?: emptyList()
                }
            )
            
            override fun loadMoreArtworks() {
                loadMore()
            }
        }
    }
    
    /**
     * 搜索关键词变化
     */
    fun onSearchKeywordChange(keywordValue: TextFieldValue) {
        _state.update { it.copy(searchKeyword = keywordValue) }
        
        val keyword = keywordValue.text
        
        // 触发标签自动补全
        autocompleteJob?.cancel()
        
        if (keyword.isNotBlank() && !keyword.endsWith(" ")) {
            val words = keyword.split(" ")
            val lastWord = words.lastOrNull()?.trim()
            
            if (!lastWord.isNullOrBlank() && lastWord.length >= 1) {
                autocompleteJob = screenModelScope.launch {
                    delay(300) // 防抖延迟
                    if (_state.value.searchKeyword.text == keyword) {
                        fetchTagAutocomplete(lastWord)
                    }
                }
            } else {
                _state.update { it.copy(autocompleteSuggestions = emptyList()) }
            }
        } else {
            _state.update { it.copy(autocompleteSuggestions = emptyList()) }
        }
    }
    
    /**
     * 获取标签自动补全
     */
    private suspend fun fetchTagAutocomplete(keyword: String) {
        _state.update { it.copy(isLoadingAutocomplete = true) }
        try {
            val result = tagApi.searchTagAutocomplete(keyword)
            val translatedTags = result.candidates.map { candidate ->
                Tag(
                    name = candidate.tagName,
                    translatedName = candidate.tagTranslation?.takeIf { it.isNotBlank() }
                )
            }
            _state.update { 
                it.copy(
                    autocompleteSuggestions = translatedTags,
                    isLoadingAutocomplete = false
                ) 
            }
        } catch (e: Exception) {
            _state.update { it.copy(isLoadingAutocomplete = false) }
        }
    }
    
    /**
     * 点击自动补全建议
     */
    fun onAutocompleteSuggestionClick(tag: Tag) {
        val currentKeyword = _state.value.searchKeyword.text
        val words = currentKeyword.split(" ").toMutableList()
        
        if (words.isNotEmpty()) {
            words[words.lastIndex] = tag.name
            val newKeyword = words.joinToString(" ") + " "
            _state.update { 
                it.copy(
                    searchKeyword = TextFieldValue(
                        text = newKeyword,
                        selection = TextRange(newKeyword.length)
                    ),
                    autocompleteSuggestions = emptyList()
                )
            }
        }
    }
    
    /**
     * 执行搜索
     */
    fun performSearch() {
        val keyword = _state.value.searchKeyword.text.trim()
        if (keyword.isBlank()) return
        
        // 保存到搜索历史
        screenModelScope.launch {
            searchHistoryStore.addHistory(keyword)
        }
        
        // 重置所有分类的搜索结果
        _state.update { 
            it.copy(
                illustResults = emptyList(),
                novelResults = emptyList(),
                userResults = emptyList(),
                illustPage = 1,
                novelPage = 1,
                userPage = 1,
                hasMoreIllust = true,
                hasMoreNovel = true,
                hasMoreUser = true,
                autocompleteSuggestions = emptyList(),  // 清空自动补全列表
                error = null
            )
        }
        
        // 执行当前分类的搜索
        searchCurrentCategory()
    }
    
    /**
     * 切换分类
     */
    fun onCategoryChange(category: SearchCategory) {
        _state.update { it.copy(currentCategory = category) }
        
        // 如果该分类还没有数据，则加载
        when (category) {
            SearchCategory.ILLUST -> {
                if (_state.value.illustResults.isEmpty()) {
                    searchIllusts()
                }
            }
            SearchCategory.NOVEL -> {
                if (_state.value.novelResults.isEmpty()) {
                    searchNovels()
                }
            }
            SearchCategory.USER -> {
                if (_state.value.userResults.isEmpty()) {
                    searchUsers()
                }
            }
        }
    }
    
    /**
     * 搜索当前分类
     */
    private fun searchCurrentCategory() {
        when (_state.value.currentCategory) {
            SearchCategory.ILLUST -> searchIllusts()
            SearchCategory.NOVEL -> searchNovels()
            SearchCategory.USER -> searchUsers()
        }
    }
    
    /**
     * 搜索插画
     */
    private fun searchIllusts() {
        val state = _state.value
        if (!state.hasMoreIllust || state.isLoadingMore) return
        
        val keyword = state.searchKeyword.text.trim()
        if (keyword.isBlank()) return
        
        screenModelScope.launch {
            _state.update { it.copy(isLoadingMore = true, error = null) }
            
            try {
                // 构建完整的搜索关键词，附加收藏人数Tag
                val fullKeyword = buildString {
                    append(keyword)
                    // 如果有收藏人数筛选，附加到关键词后
                    if (state.illustParams.bookmarkCount.tag.isNotEmpty()) {
                        append(" ")
                        append(state.illustParams.bookmarkCount.tag)
                    }
                }
                
                val result = searchApi.searchIllust(
                    keyword = fullKeyword,
                    searchMode = state.illustParams.searchMode.value,
                    order = state.illustParams.order.value,
                    mode = state.illustParams.contentMode.value,
                    page = state.illustPage,
                    aiType = if (state.illustParams.hideAi) 1 else null,
                    scd = state.illustParams.dateRange?.startDate,
                    ecd = state.illustParams.dateRange?.endDate
                )
                
                if (!result.error && result.body != null) {
                    val body = result.body!!
                    val artworks = body.illustManga.data.toArtworkList(
                        tagTranslationUtil = tagTranslationUtil,
                        tagTranslation = body.tagTranslation,
                        ageLimitDeterminer = ageLimitDeterminer
                    )
                    
                    _state.update { current ->
                        current.copy(
                            illustResults = current.illustResults + artworks,
                            illustPage = current.illustPage + 1,
                            hasMoreIllust = artworks.isNotEmpty(),
                            isLoadingMore = false
                        )
                    }
                } else {
                    _state.update { 
                        it.copy(
                            isLoadingMore = false,
                            error = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoadingMore = false,
                        error = e.message ?: "Search failed"
                    )
                }
            }
        }
    }
    
    /**
     * 搜索小说
     */
    private fun searchNovels() {
        val state = _state.value
        if (!state.hasMoreNovel || state.isLoadingMore) return
        
        val keyword = state.searchKeyword.text.trim()
        if (keyword.isBlank()) return
        
        screenModelScope.launch {
            _state.update { it.copy(isLoadingMore = true, error = null) }
            
            try {
                // 构建完整的搜索关键词，附加收藏人数Tag
                val fullKeyword = buildString {
                    append(keyword)
                    // 如果有收藏人数筛选，附加到关键词后
                    if (state.novelParams.bookmarkCount.tag.isNotEmpty()) {
                        append(" ")
                        append(state.novelParams.bookmarkCount.tag)
                    }
                }
                
                val result = searchApi.searchNovel(
                    keyword = fullKeyword,
                    searchMode = state.novelParams.searchMode.value,
                    order = state.novelParams.order.value,
                    mode = state.novelParams.contentMode.value,
                    page = state.novelPage,
                    aiType = if (state.novelParams.hideAi) 1 else null,
                    scd = state.novelParams.dateRange?.startDate,
                    ecd = state.novelParams.dateRange?.endDate
                )
                
                if (!result.error && result.body != null) {
                    val body = result.body!!
                    val novels = body.novel.data.toNovelList(
                        tagTranslation = body.tagTranslation,
                        ageLimitDeterminer = ageLimitDeterminer
                    )
                    
                    _state.update { current ->
                        current.copy(
                            novelResults = current.novelResults + novels,
                            novelPage = current.novelPage + 1,
                            hasMoreNovel = novels.isNotEmpty(),
                            isLoadingMore = false
                        )
                    }
                } else {
                    _state.update { 
                        it.copy(
                            isLoadingMore = false,
                            error = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoadingMore = false,
                        error = e.message ?: "Search failed"
                    )
                }
            }
        }
    }
    
    /**
     * 搜索用户
     */
    private fun searchUsers() {
        val state = _state.value
        if (!state.hasMoreUser || state.isLoadingMore) return
        
        val keyword = state.searchKeyword.text.trim()
        if (keyword.isBlank()) return
        
        screenModelScope.launch {
            _state.update { it.copy(isLoadingMore = true, error = null) }
            
            try {
                val result = searchApi.searchUser(
                    keyword = keyword,
                    searchMode = state.userParams.searchMode.value,
                    hasWork = if (state.userParams.onlyWithWork) 1 else 0,
                    page = state.userPage
                )
                
                if (!result.error && result.body != null) {
                    val body = result.body!!
                    val users = body.toUsersWithArtworks(ageLimitDeterminer)
                    
                    _state.update { current ->
                        current.copy(
                            userResults = current.userResults + users,
                            userPage = current.userPage + 1,
                            hasMoreUser = users.isNotEmpty(),
                            isLoadingMore = false
                        )
                    }
                } else {
                    _state.update { 
                        it.copy(
                            isLoadingMore = false,
                            error = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update { 
                    it.copy(
                        isLoadingMore = false,
                        error = e.message ?: "Search failed"
                    )
                }
            }
        }
    }
    
    /**
     * 加载更多
     */
    fun loadMore() {
        when (_state.value.currentCategory) {
            SearchCategory.ILLUST -> searchIllusts()
            SearchCategory.NOVEL -> searchNovels()
            SearchCategory.USER -> searchUsers()
        }
    }
    
    /**
     * 刷新当前分类
     */
    fun refresh() {
        when (_state.value.currentCategory) {
            SearchCategory.ILLUST -> {
                _state.update {
                    it.copy(
                        illustResults = emptyList(),
                        illustPage = 1,
                        hasMoreIllust = true
                    )
                }
                searchIllusts()
            }
            SearchCategory.NOVEL -> {
                _state.update {
                    it.copy(
                        novelResults = emptyList(),
                        novelPage = 1,
                        hasMoreNovel = true
                    )
                }
                searchNovels()
            }
            SearchCategory.USER -> {
                _state.update {
                    it.copy(
                        userResults = emptyList(),
                        userPage = 1,
                        hasMoreUser = true
                    )
                }
                searchUsers()
            }
        }
    }
    
    /**
     * 打开/关闭筛选抽屉
     */
    fun toggleFilterDrawer() {
        val currentState = _state.value
        
        if (currentState.isFilterDrawerOpen) {
            // 关闭抽屉：检查筛选条件是否真正变更
            val illustChanged = currentState.illustParamsSnapshot != null && 
                               currentState.illustParams != currentState.illustParamsSnapshot
            val novelChanged = currentState.novelParamsSnapshot != null && 
                              currentState.novelParams != currentState.novelParamsSnapshot
            val userChanged = currentState.userParamsSnapshot != null && 
                             currentState.userParams != currentState.userParamsSnapshot
            
            if (illustChanged || novelChanged || userChanged) {
                // 筛选条件变更，清空所有tab的数据和重置滚动位置
                _state.update {
                    it.copy(
                        isFilterDrawerOpen = false,
                        illustParamsSnapshot = null,
                        novelParamsSnapshot = null,
                        userParamsSnapshot = null,
                        // 清空所有分类的数据
                        illustResults = emptyList(),
                        novelResults = emptyList(),
                        userResults = emptyList(),
                        illustPage = 1,
                        novelPage = 1,
                        userPage = 1,
                        hasMoreIllust = true,
                        hasMoreNovel = true,
                        hasMoreUser = true
                    )
                }
                // 重新搜索当前分类
                searchCurrentCategory()
            } else {
                // 筛选条件未变更，只关闭抽屉
                _state.update {
                    it.copy(
                        isFilterDrawerOpen = false,
                        illustParamsSnapshot = null,
                        novelParamsSnapshot = null,
                        userParamsSnapshot = null
                    )
                }
            }
        } else {
            // 打开抽屉：保存当前筛选条件快照
            _state.update {
                it.copy(
                    isFilterDrawerOpen = true,
                    illustParamsSnapshot = it.illustParams,
                    novelParamsSnapshot = it.novelParams,
                    userParamsSnapshot = it.userParams
                )
            }
        }
    }
    
    /**
     * 更新插画筛选参数（临时更新，不触发搜索）
     */
    fun updateIllustParams(params: IllustSearchParams) {
        _state.update { 
            it.copy(illustParams = params)
        }
    }
    
    /**
     * 更新小说筛选参数（临时更新，不触发搜索）
     */
    fun updateNovelParams(params: NovelSearchParams) {
        _state.update { 
            it.copy(novelParams = params)
        }
    }
    
    /**
     * 更新用户筛选参数（临时更新，不触发搜索）
     */
    fun updateUserParams(params: UserSearchParams) {
        _state.update { 
            it.copy(userParams = params)
        }
    }
}
