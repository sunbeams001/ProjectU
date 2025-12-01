package com.projectu.ui.screens.user

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.projectu.shared.data.cache.StateCacheEvent
import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.data.remote.api.PixivApi
import com.projectu.shared.data.remote.mapper.toArtwork
import com.projectu.shared.data.remote.mapper.toMangaSeries
import com.projectu.shared.data.remote.mapper.toNovel
import com.projectu.shared.data.remote.mapper.toNovelSeries
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.BookmarkStatus
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.usecase.SyncArtworkStatesUseCase
import com.projectu.shared.domain.usecase.SyncNovelStatesUseCase
import com.projectu.shared.util.AgeLimitDeterminer
import com.projectu.shared.util.TagTranslationUtil
import com.projectu.ui.navigation.ArtworkListSource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 用户页面 ViewModel
 * 
 * 负责加载用户信息和作品列表
 */
class UserViewModel(
    private val pixivApi: PixivApi,
    private val ageLimitDeterminer: AgeLimitDeterminer,
    private val tagTranslationUtil: TagTranslationUtil,
    private val syncArtworkStatesUseCase: SyncArtworkStatesUseCase,
    private val syncNovelStatesUseCase: SyncNovelStatesUseCase,
    private val stateCacheManager: StateCacheManager
) : ScreenModel {
    
    private val _state = MutableStateFlow(UserScreenState())
    val state: StateFlow<UserScreenState> = _state.asStateFlow()
    
    // 插画/漫画每页加载数量
    private val ILLUST_PAGE_SIZE = 48
    // 小说每页加载数量
    private val NOVEL_PAGE_SIZE = 30
    
    // 当前用户ID
    private var currentUserId: Long = 0
    
    init {
        // 监听全局状态变更事件
        screenModelScope.launch {
            stateCacheManager.stateChangeEvents.collect { event ->
                when (event) {
                    is StateCacheEvent.ArtworkBookmarkChanged -> {
                        updateArtworkBookmarkStatus(event.artworkId, event.status, event.bookmarkId)
                    }
                    is StateCacheEvent.NovelBookmarkChanged -> {
                        updateNovelBookmarkStatus(event.novelId, event.status, event.bookmarkId)
                    }
                    else -> {}
                }
            }
        }
    }
    
    /**
     * 创建绑定到指定 Tab 的 ArtworkListSource
     * 
     * 用于用户作品列表页面的列表导航功能。当用户点击某个 Tab 下的作品时，
     * 创建一个绑定该 Tab 的列表源，使详情页可以响应式地获取列表更新。
     * 
     * @param tab 用户页面的 Tab
     * @return 绑定到指定 Tab 的 ArtworkListSource
     */
    fun createArtworkListSource(tab: UserProfileTab): ArtworkListSource {
        return object : ArtworkListSource {
            override val artworkIdsFlow: StateFlow<List<String>> = state.map { currentState ->
                currentState.tabDataCache[tab]?.artworks?.map { it.id } ?: emptyList()
            }.stateIn(
                scope = screenModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = state.value.tabDataCache[tab]?.artworks?.map { it.id } ?: emptyList()
            )
            
            override fun loadMoreArtworks() {
                loadMore()
            }
        }
    }
    
    /**
     * 加载用户信息
     */
    fun loadUser(userId: Long) {
        if (userId == currentUserId && _state.value.userProfile.userId.isNotEmpty()) {
            return // 已加载，不重复加载
        }
        
        currentUserId = userId
        
        screenModelScope.launch {
            _state.update { it.copy(isLoadingProfile = true, profileError = null) }
            
            try {
                // 1. 加载用户基本信息
                val userInfoResponse = pixivApi.userApi.getUserInfo(userId, full = 1)
                if (userInfoResponse.error) {
                    _state.update { 
                        it.copy(
                            isLoadingProfile = false, 
                            profileError = userInfoResponse.message ?: "加载用户信息失败"
                        ) 
                    }
                    return@launch
                }
                
                val userInfo = userInfoResponse.body ?: run {
                    _state.update { 
                        it.copy(isLoadingProfile = false, profileError = "用户信息为空") 
                    }
                    return@launch
                }
                
                // 2. 加载用户作品概况
                val profileAllResponse = pixivApi.userApi.getProfileAll(userId)
                if (profileAllResponse.error) {
                    _state.update { 
                        it.copy(
                            isLoadingProfile = false, 
                            profileError = profileAllResponse.message ?: "加载作品列表失败"
                        ) 
                    }
                    return@launch
                }
                
                val profileAll = profileAllResponse.body ?: run {
                    _state.update { 
                        it.copy(isLoadingProfile = false, profileError = "作品概况为空") 
                    }
                    return@launch
                }
                
                // 3. 解析作品ID列表
                val illustIds = profileAll.illusts?.keys?.toList() ?: emptyList()
                val mangaIds = profileAll.manga?.keys?.toList() ?: emptyList()
                val novelIds = profileAll.novels?.keys?.toList() ?: emptyList()
                val mangaSeriesList = profileAll.mangaSeries ?: emptyList()
                val novelSeriesList = profileAll.novelSeries ?: emptyList()
                
                // 4. 构建可用的Tab列表
                val availableTabs = mutableListOf<UserProfileTab>()
                val tabDataCache = mutableMapOf<UserProfileTab, TabData>()
                
                if (illustIds.isNotEmpty()) {
                    availableTabs.add(UserProfileTab.ILLUSTS)
                    tabDataCache[UserProfileTab.ILLUSTS] = TabData(allIds = illustIds)
                }
                if (mangaIds.isNotEmpty()) {
                    availableTabs.add(UserProfileTab.MANGA)
                    tabDataCache[UserProfileTab.MANGA] = TabData(allIds = mangaIds)
                }
                if (novelIds.isNotEmpty()) {
                    availableTabs.add(UserProfileTab.NOVELS)
                    tabDataCache[UserProfileTab.NOVELS] = TabData(allIds = novelIds)
                }
                if (mangaSeriesList.isNotEmpty()) {
                    availableTabs.add(UserProfileTab.MANGA_SERIES)
                }
                if (novelSeriesList.isNotEmpty()) {
                    availableTabs.add(UserProfileTab.NOVEL_SERIES)
                }
                
                // 5. 更新状�?
                _state.update { 
                    it.copy(
                        userProfile = UserProfile(
                            userId = userInfo.userId,
                            name = userInfo.name,
                            image = userInfo.image,
                            imageBig = userInfo.imageBig,
                            premium = userInfo.premium,
                            isFollowed = userInfo.isFollowed,
                            following = userInfo.following,
                            comment = userInfo.comment,
                            backgroundUrl = userInfo.background?.url
                        ),
                        availableTabs = availableTabs,
                        currentTab = availableTabs.firstOrNull() ?: UserProfileTab.ILLUSTS,
                        tabDataCache = tabDataCache,
                        mangaSeries = mangaSeriesList.map { series ->
                            series.toMangaSeries()
                        },
                        novelSeries = novelSeriesList.map { series ->
                            series.toNovelSeries()
                        },
                        isLoadingProfile = false,
                        profileError = null
                    )
                }
                
                // 6. 自动加载第一个Tab的数�?
                if (availableTabs.isNotEmpty()) {
                    loadTabData(availableTabs.first())
                }
                
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoadingProfile = false, 
                        profileError = e.message ?: "网络错误"
                    ) 
                }
            }
        }
    }
    
    /**
     * 切换Tab
     */
    fun switchTab(tab: UserProfileTab) {
        if (tab == _state.value.currentTab) return
        
        _state.update { it.copy(currentTab = tab) }
        
        // 如果该Tab还没有加载数据，则加�?
        val tabData = _state.value.tabDataCache[tab]
        if (tabData != null && tabData.artworks.isEmpty() && tabData.novels.isEmpty() && !tabData.isLoading) {
            loadTabData(tab)
        }
    }
    
    /**
     * 加载Tab数据（也用于重试）
     */
    fun loadTabData(tab: UserProfileTab) {
        val tabData = _state.value.tabDataCache[tab] ?: return
        // 移除 allIds.isEmpty() 检查，允许在有数据时重试
        if (tabData.isLoading) return
        if (tabData.allIds.isEmpty()) return // 没有ID则无法加载
        
        screenModelScope.launch {
            updateTabData(tab) { it.copy(isLoading = true, error = null) }
            
            try {
                when (tab) {
                    UserProfileTab.ILLUSTS, UserProfileTab.MANGA -> {
                        loadIllustOrManga(tab)
                    }
                    UserProfileTab.NOVELS -> {
                        loadNovels(tab)
                    }
                    else -> {
                        // 系列类型的Tab不需要分页加�?
                        updateTabData(tab) { it.copy(isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                updateTabData(tab) { 
                    it.copy(isLoading = false, error = e.message ?: "加载失败") 
                }
            }
        }
    }
    
    /**
     * 加载更多（当前Tab�?
     */
    fun loadMore() {
        val currentTab = _state.value.currentTab
        val tabData = _state.value.tabDataCache[currentTab] ?: return
        
        if (tabData.isLoading || !tabData.hasMore) return
        
        screenModelScope.launch {
            updateTabData(currentTab) { it.copy(isLoading = true, error = null) }
            
            try {
                when (currentTab) {
                    UserProfileTab.ILLUSTS, UserProfileTab.MANGA -> {
                        loadIllustOrManga(currentTab)
                    }
                    UserProfileTab.NOVELS -> {
                        loadNovels(currentTab)
                    }
                    else -> {
                        updateTabData(currentTab) { it.copy(isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                updateTabData(currentTab) { 
                    it.copy(isLoading = false, error = e.message ?: "加载更多失败") 
                }
            }
        }
    }
    
    /**
     * 加载插画或漫�?
     */
    private suspend fun loadIllustOrManga(tab: UserProfileTab) {
        val tabData = _state.value.tabDataCache[tab] ?: return
        val allIds = tabData.allIds
        val loadedIds = tabData.loadedIds
        
        // 计算下一批要加载的ID
        val remainingIds = allIds.filter { it !in loadedIds }
        val nextIds = remainingIds.take(ILLUST_PAGE_SIZE)
        
        if (nextIds.isEmpty()) {
            updateTabData(tab) { it.copy(isLoading = false, hasMore = false) }
            return
        }
        
        val workCategory = when (tab) {
            UserProfileTab.ILLUSTS -> "illust"
            UserProfileTab.MANGA -> "manga"
            else -> "illustManga"
        }
        
        val response = pixivApi.userApi.getProfileIllusts(
            uid = currentUserId,
            ids = nextIds,
            workCategory = workCategory,
            isFirstPage = if (loadedIds.isEmpty()) 1 else 0
        )
        
        if (response.error) {
            updateTabData(tab) { 
                it.copy(isLoading = false, error = response.message ?: "加载失败") 
            }
            return
        }
        
        val works = response.body?.works ?: emptyMap()
        val newArtworks = works.values.filterNotNull().map { illust ->
            illust.toArtwork(
                tagTranslationUtil = tagTranslationUtil,
                tagTranslation = null,
                ageLimitDeterminer = ageLimitDeterminer
            )
        }
        
        // 同步全局状态缓存（收藏状态）
        val syncedArtworks = syncArtworkStatesUseCase(newArtworks)
        
        updateTabData(tab) {
            it.copy(
                loadedIds = loadedIds + nextIds,
                artworks = it.artworks + syncedArtworks,
                isLoading = false,
                hasMore = remainingIds.size > nextIds.size
            )
        }
    }
    
    /**
     * 加载小说
     */
    private suspend fun loadNovels(tab: UserProfileTab) {
        val tabData = _state.value.tabDataCache[tab] ?: return
        val allIds = tabData.allIds
        val loadedIds = tabData.loadedIds
        
        // 计算下一批要加载的ID
        val remainingIds = allIds.filter { it !in loadedIds }
        val nextIds = remainingIds.take(NOVEL_PAGE_SIZE)
        
        if (nextIds.isEmpty()) {
            updateTabData(tab) { it.copy(isLoading = false, hasMore = false) }
            return
        }
        
        val response = pixivApi.userApi.getProfileNovels(
            uid = currentUserId,
            ids = nextIds
        )
        
        if (response.error) {
            updateTabData(tab) { 
                it.copy(isLoading = false, error = response.message ?: "加载失败") 
            }
            return
        }
        
        val works = response.body?.works ?: emptyMap()
        val newNovels = works.values.filterNotNull().map { novel ->
            novel.toNovel(
                tagTranslation = null,
                ageLimitDeterminer = ageLimitDeterminer
            )
        }
        
        // 同步全局状态缓存（收藏状态）
        val syncedNovels = syncNovelStatesUseCase(newNovels)
        
        updateTabData(tab) {
            it.copy(
                loadedIds = loadedIds + nextIds,
                novels = it.novels + syncedNovels,
                isLoading = false,
                hasMore = remainingIds.size > nextIds.size
            )
        }
    }
    
    /**
     * 更新列表中作品的收藏状态
     * 由全局状态变更事件触发
     */
    private fun updateArtworkBookmarkStatus(
        artworkId: String,
        status: BookmarkStatus,
        bookmarkId: String?
    ) {
        _state.update { state ->
            val updatedCache = state.tabDataCache.mapValues { (_, tabData) ->
                val updatedArtworks = tabData.artworks.map { artwork ->
                    if (artwork.id == artworkId) {
                        artwork.copy(bookmarkStatus = status, bookmarkId = bookmarkId)
                    } else {
                        artwork
                    }
                }
                tabData.copy(artworks = updatedArtworks)
            }
            state.copy(tabDataCache = updatedCache)
        }
    }
    
    /**
     * 更新列表中小说的收藏状态
     * 由全局状态变更事件触发
     */
    private fun updateNovelBookmarkStatus(
        novelId: String,
        status: BookmarkStatus,
        bookmarkId: String?
    ) {
        _state.update { state ->
            val updatedCache = state.tabDataCache.mapValues { (_, tabData) ->
                val updatedNovels = tabData.novels.map { novel ->
                    if (novel.id == novelId) {
                        novel.copy(bookmarkStatus = status, bookmarkId = bookmarkId)
                    } else {
                        novel
                    }
                }
                tabData.copy(novels = updatedNovels)
            }
            state.copy(tabDataCache = updatedCache)
        }
    }
    
    /**
     * 刷新当前Tab
     */
    fun refresh() {
        // 如果用户信息加载失败，重新加载用户信息
        if (_state.value.profileError != null && _state.value.userProfile.userId.isEmpty()) {
            retryLoadUser()
            return
        }
        
        val currentTab = _state.value.currentTab
        
        // 重置Tab数据
        updateTabData(currentTab) {
            it.copy(
                loadedIds = emptyList(),
                artworks = emptyList(),
                novels = emptyList(),
                hasMore = true,
                error = null
            )
        }
        
        // 重新加载
        loadTabData(currentTab)
    }
    
    /**
     * 重试加载用户信息（用于初始加载失败后的重试）
     */
    fun retryLoadUser() {
        val userId = currentUserId
        if (userId != 0L) {
            // 重置状态以允许重新加载
            currentUserId = 0L
            loadUser(userId)
        }
    }
    
    /**
     * 更新Tab数据
     */
    private fun updateTabData(tab: UserProfileTab, update: (TabData) -> TabData) {
        _state.update { state ->
            val currentData = state.tabDataCache[tab] ?: TabData()
            val newData = update(currentData)
            state.copy(
                tabDataCache = state.tabDataCache + (tab to newData)
            )
        }
    }
}
