package com.projectu.ui.screens.user

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.projectu.shared.data.cache.StateCacheEvent
import com.projectu.ui.util.AppLogger
import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.data.remote.api.PixivApi
import com.projectu.shared.data.remote.mapper.toArtwork
import com.projectu.shared.data.remote.mapper.toMangaSeries
import com.projectu.shared.data.remote.mapper.toNovel
import com.projectu.shared.data.remote.mapper.toNovelSeries
import com.projectu.shared.data.remote.mapper.toUser
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
    // 收藏插画每页加载数量
    private val BOOKMARK_ILLUST_PAGE_SIZE = 48
    // 收藏小说每页加载数量
    private val BOOKMARK_NOVEL_PAGE_SIZE = 30
    
    // 当前用户ID
    private var currentUserId: String = ""
    
    // 推荐用户Tab的滚动位置（作品索引）
    private var recommendUsersScrollIndex: Int = 0
    
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
     * 创建推荐用户Tab的ArtworkListSource
     * 用于推荐用户作品预览的列表导航功能
     */
    fun createRecommendUsersArtworkListSource(): ArtworkListSource {
        return object : ArtworkListSource {
            override val artworkIdsFlow: StateFlow<List<String>> = state.map { currentState ->
                currentState.tabDataCache[UserProfileTab.RECOMMEND_USERS]?.users?.flatMap { user ->
                    user.illusts.map { it.id }
                } ?: emptyList()
            }.stateIn(
                scope = screenModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = state.value.tabDataCache[UserProfileTab.RECOMMEND_USERS]?.users?.flatMap { user ->
                    user.illusts.map { it.id }
                } ?: emptyList()
            )
            
            override fun loadMoreArtworks() {
                // 推荐用户Tab暂不支持分页加载更多
                // 未来可以根据需要实现
            }
        }
    }
    
    /**
     * 设置推荐用户Tab的滚动位置
     * 用于从作品详情页返回时恢复滚动位置
     */
    fun setRecommendUsersScrollIndex(index: Int) {
        recommendUsersScrollIndex = index
    }
    
    /**
     * 获取推荐用户Tab的滚动位置
     */
    fun getRecommendUsersScrollIndex(): Int {
        return recommendUsersScrollIndex
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
    fun loadUser(userId: String) {
        if (userId == currentUserId && _state.value.userProfile.userId.isNotEmpty()) {
            return // 已加载，不重复加载
        }
        
        currentUserId = userId
        val userIdLong = userId.toLongOrNull() ?: return
        
        screenModelScope.launch {
            _state.update { it.copy(isLoadingProfile = true, profileError = null) }
            
            try {
                // 1. 加载用户基本信息
                val userInfoResponse = pixivApi.userApi.getUserInfo(userIdLong, full = 1)
                if (userInfoResponse.error) {
                    _state.update { 
                        it.copy(
                            isLoadingProfile = false, 
                            profileError = userInfoResponse.message ?: "Failed to load user info"
                        ) 
                    }
                    return@launch
                }
                
                val userInfo = userInfoResponse.body ?: run {
                    _state.update { 
                        it.copy(isLoadingProfile = false, profileError = "User info is empty") 
                    }
                    return@launch
                }
                
                // 2. 加载用户作品概况
                val profileAllResponse = pixivApi.userApi.getProfileAll(userIdLong)
                if (profileAllResponse.error) {
                    _state.update { 
                        it.copy(
                            isLoadingProfile = false, 
                            profileError = profileAllResponse.message ?: "Failed to load works list"
                        ) 
                    }
                    return@launch
                }
                
                val profileAll = profileAllResponse.body ?: run {
                    _state.update { 
                        it.copy(isLoadingProfile = false, profileError = "Works overview is empty") 
                    }
                    return@launch
                }
                
                // 3. 解析作品ID列表
                val illustIds = profileAll.illusts?.keys?.toList() ?: emptyList()
                val mangaIds = profileAll.manga?.keys?.toList() ?: emptyList()
                val novelIds = profileAll.novels?.keys?.toList() ?: emptyList()
                val mangaSeriesList = profileAll.mangaSeries ?: emptyList()
                val novelSeriesList = profileAll.novelSeries ?: emptyList()
                
                // 解析收藏数量
                val bookmarkCount = profileAll.bookmarkCount
                
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
                
                // 添加收藏Tab（根据收藏数量动态生成）
                if (bookmarkCount != null) {
                    // 公开收藏·插画
                    if (bookmarkCount.public.illust > 0) {
                        availableTabs.add(UserProfileTab.BOOKMARK_ILLUSTS_PUBLIC)
                        tabDataCache[UserProfileTab.BOOKMARK_ILLUSTS_PUBLIC] = TabData(
                            total = bookmarkCount.public.illust
                        )
                    }
                    // 私人收藏·插画
                    if (bookmarkCount.private.illust > 0) {
                        availableTabs.add(UserProfileTab.BOOKMARK_ILLUSTS_PRIVATE)
                        tabDataCache[UserProfileTab.BOOKMARK_ILLUSTS_PRIVATE] = TabData(
                            total = bookmarkCount.private.illust
                        )
                    }
                    // 公开收藏·小说
                    if (bookmarkCount.public.novel > 0) {
                        availableTabs.add(UserProfileTab.BOOKMARK_NOVELS_PUBLIC)
                        tabDataCache[UserProfileTab.BOOKMARK_NOVELS_PUBLIC] = TabData(
                            total = bookmarkCount.public.novel
                        )
                    }
                    // 私人收藏·小说
                    if (bookmarkCount.private.novel > 0) {
                        availableTabs.add(UserProfileTab.BOOKMARK_NOVELS_PRIVATE)
                        tabDataCache[UserProfileTab.BOOKMARK_NOVELS_PRIVATE] = TabData(
                            total = bookmarkCount.private.novel
                        )
                    }
                }
                
                // 推荐用户Tab（放在详情前）
                availableTabs.add(UserProfileTab.RECOMMEND_USERS)
                tabDataCache[UserProfileTab.RECOMMEND_USERS] = TabData()
                
                // 用户信息Tab（放在最后）
                availableTabs.add(UserProfileTab.USER_INFO)
                
                // 5. 构建用户详细信息
                val userDetailInfo = UserDetailInfo(
                    userId = userInfo.userId,
                    name = userInfo.name,
                    image = userInfo.image,
                    imageBig = userInfo.imageBig,
                    premium = userInfo.premium,
                    isFollowed = userInfo.isFollowed,
                    isMypixiv = userInfo.isMypixiv,
                    isBlocking = userInfo.isBlocking,
                    backgroundUrl = userInfo.background?.url,
                    official = userInfo.official,
                    following = userInfo.following,
                    mypixivCount = userInfo.mypixivCount,
                    followedBack = userInfo.followedBack,
                    canSendMessage = userInfo.canSendMessage,
                    comment = userInfo.comment,
                    commentHtml = userInfo.commentHtml,
                    webpage = userInfo.webpage,
                    twitterUrl = userInfo.social?.twitter?.url,
                    facebookUrl = userInfo.social?.facebook?.url,
                    instagramUrl = userInfo.social?.instagram?.url,
                    tumblrUrl = userInfo.social?.tumblr?.url,
                    pawooUrl = userInfo.social?.pawoo?.url,
                    circlemsUrl = userInfo.social?.circlems?.url,
                    region = userInfo.region?.name,
                    age = userInfo.age?.name,
                    birthDay = userInfo.birthDay?.name,
                    gender = userInfo.gender?.name,
                    job = userInfo.job?.name,
                    workspacePc = userInfo.workspace?.pc,
                    workspaceMonitor = userInfo.workspace?.monitor,
                    workspaceTool = userInfo.workspace?.tool,
                    workspaceScanner = userInfo.workspace?.scanner,
                    workspaceTablet = userInfo.workspace?.tablet,
                    workspaceMouse = userInfo.workspace?.mouse,
                    workspacePrinter = userInfo.workspace?.printer,
                    workspaceDesktop = userInfo.workspace?.desktop,
                    workspaceMusic = userInfo.workspace?.music,
                    workspaceDesk = userInfo.workspace?.desk,
                    workspaceChair = userInfo.workspace?.chair,
                    workspaceComment = userInfo.workspace?.comment,
                    workspaceImageUrl = userInfo.workspace?.imageUrl,
                    workspaceImageBigUrl = userInfo.workspace?.imageBigUrl,
                    commissionRequestStatus = userInfo.commission?.requestStatus,
                    commissionFanRequestStatus = userInfo.commission?.fanRequestStatus,
                    groups = userInfo.group?.map { group ->
                        UserGroupInfo(
                            id = group.id,
                            title = group.title,
                            iconUrl = group.iconUrl
                        )
                    } ?: emptyList()
                )
                
                // 6. 更新状态
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
                        userDetailInfo = userDetailInfo,
                        availableTabs = availableTabs,
                        currentTab = availableTabs.firstOrNull() ?: UserProfileTab.ILLUSTS,
                        tabDataCache = tabDataCache,
                        mangaSeries = mangaSeriesList.map { series ->
                            series.toMangaSeries(
                                userName = userInfo.name,
                                profileImageUrl = userInfo.image,
                                isFollowed = userInfo.isFollowed
                            )
                        },
                        novelSeries = novelSeriesList.map { series ->
                            series.toNovelSeries()
                        },
                        isLoadingProfile = false,
                        profileError = null
                    )
                }
                
                // 7. 自动加载第一个Tab的数据
                val firstDataTab = availableTabs.firstOrNull()
                if (firstDataTab != null && firstDataTab != UserProfileTab.USER_INFO) {
                    loadTabData(firstDataTab)
                }
                
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoadingProfile = false, 
                        profileError = e.message ?: "Network error"
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
        
        // 如果该Tab还没有加载数据，则加载
        val tabData = _state.value.tabDataCache[tab]
        if (tabData != null && 
            tabData.artworks.isEmpty() && 
            tabData.novels.isEmpty() && 
            tabData.users.isEmpty() && 
            !tabData.isLoading) {
            loadTabData(tab)
        }
    }
    
    /**
     * 加载Tab数据（也用于重试）
     */
    fun loadTabData(tab: UserProfileTab) {
        val tabData = _state.value.tabDataCache[tab]
        if (tabData == null) {
            // 收藏Tab在切换时才创建缓存条目
            if (tab.isBookmarkTab()) {
                screenModelScope.launch {
                    updateTabData(tab) { TabData(isLoading = true) }
                    try {
                        when (tab) {
                            UserProfileTab.BOOKMARK_ILLUSTS_PUBLIC,
                            UserProfileTab.BOOKMARK_ILLUSTS_PRIVATE -> {
                                loadBookmarkIllusts(tab)
                            }
                            UserProfileTab.BOOKMARK_NOVELS_PUBLIC,
                            UserProfileTab.BOOKMARK_NOVELS_PRIVATE -> {
                                loadBookmarkNovels(tab)
                            }
                            else -> {}
                        }
                    } catch (e: Exception) {
                        updateTabData(tab) { 
                            it.copy(isLoading = false, error = e.message ?: "Failed to load") 
                        }
                    }
                }
            }
            return
        }
        
        if (tabData.isLoading) return
        // 对于收藏Tab和推荐用户Tab，不检查 allIds
        if (!tab.isBookmarkTab() && tab != UserProfileTab.RECOMMEND_USERS && tabData.allIds.isEmpty()) return
        
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
                    UserProfileTab.BOOKMARK_ILLUSTS_PUBLIC,
                    UserProfileTab.BOOKMARK_ILLUSTS_PRIVATE -> {
                        loadBookmarkIllusts(tab)
                    }
                    UserProfileTab.BOOKMARK_NOVELS_PUBLIC,
                    UserProfileTab.BOOKMARK_NOVELS_PRIVATE -> {
                        loadBookmarkNovels(tab)
                    }
                    UserProfileTab.RECOMMEND_USERS -> {
                        loadRecommendUsers(tab)
                    }
                    else -> {
                        // 系列类型的Tab不需要分页加载
                        updateTabData(tab) { it.copy(isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                updateTabData(tab) { 
                    it.copy(isLoading = false, error = e.message ?: "Failed to load") 
                }
            }
        }
    }
    
    /**
     * 加载更多（当前Tab）
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
                    UserProfileTab.BOOKMARK_ILLUSTS_PUBLIC,
                    UserProfileTab.BOOKMARK_ILLUSTS_PRIVATE -> {
                        loadBookmarkIllusts(currentTab)
                    }
                    UserProfileTab.BOOKMARK_NOVELS_PUBLIC,
                    UserProfileTab.BOOKMARK_NOVELS_PRIVATE -> {
                        loadBookmarkNovels(currentTab)
                    }
                    else -> {
                        updateTabData(currentTab) { it.copy(isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                updateTabData(currentTab) { 
                    it.copy(isLoading = false, error = e.message ?: "Failed to load more") 
                }
            }
        }
    }
    
    /**
     * 加载插画或漫画
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
        
        val userIdLong = currentUserId.toLongOrNull() ?: return
        val response = pixivApi.userApi.getProfileIllusts(
            uid = userIdLong,
            ids = nextIds,
            workCategory = workCategory,
            isFirstPage = if (loadedIds.isEmpty()) 1 else 0
        )
        
        if (response.error) {
            updateTabData(tab) { 
                it.copy(isLoading = false, error = response.message ?: "Failed to load") 
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
        
        val userIdLong = currentUserId.toLongOrNull() ?: return
        val response = pixivApi.userApi.getProfileNovels(
            uid = userIdLong,
            ids = nextIds
        )
        
        if (response.error) {
            updateTabData(tab) { 
                it.copy(isLoading = false, error = response.message ?: "Failed to load") 
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
     * 加载收藏的插画·漫画
     */
    private suspend fun loadBookmarkIllusts(tab: UserProfileTab) {
        val tabData = _state.value.tabDataCache[tab] ?: TabData()
        val currentOffset = tabData.offset
        val selectedTag = tabData.selectedTag
        
        val rest = when (tab) {
            UserProfileTab.BOOKMARK_ILLUSTS_PUBLIC -> "show"
            UserProfileTab.BOOKMARK_ILLUSTS_PRIVATE -> "hide"
            else -> return
        }
        
        val userIdLong = currentUserId.toLongOrNull() ?: return
        val response = pixivApi.bookmarkApi.getUserBookmarkIllusts(
            uid = userIdLong,
            tag = selectedTag ?: "",
            offset = currentOffset,
            limit = BOOKMARK_ILLUST_PAGE_SIZE,
            rest = rest
        )
        
        if (response.error) {
            updateTabData(tab) { 
                it.copy(isLoading = false, error = response.message ?: "Failed to load") 
            }
            return
        }
        
        val body = response.body ?: run {
            updateTabData(tab) { it.copy(isLoading = false, error = "Data is empty") }
            return
        }
        
        val newArtworks = body.works.map { illust ->
            illust.toArtwork(
                tagTranslationUtil = tagTranslationUtil,
                tagTranslation = null,
                ageLimitDeterminer = ageLimitDeterminer
            )
        }
        
        // 同步全局状态缓存
        val syncedArtworks = syncArtworkStatesUseCase(newArtworks)
        
        val newOffset = currentOffset + syncedArtworks.size
        val hasMore = newOffset < body.total
        
        updateTabData(tab) {
            it.copy(
                artworks = it.artworks + syncedArtworks,
                offset = newOffset,
                total = body.total,
                isLoading = false,
                hasMore = hasMore
            )
        }
    }
    
    /**
     * 加载收藏的小说
     */
    private suspend fun loadBookmarkNovels(tab: UserProfileTab) {
        val tabData = _state.value.tabDataCache[tab] ?: TabData()
        val currentOffset = tabData.offset
        val selectedTag = tabData.selectedTag
        
        val rest = when (tab) {
            UserProfileTab.BOOKMARK_NOVELS_PUBLIC -> "show"
            UserProfileTab.BOOKMARK_NOVELS_PRIVATE -> "hide"
            else -> return
        }
        
        val userIdLong = currentUserId.toLongOrNull() ?: return
        val response = pixivApi.bookmarkApi.getUserBookmarkNovels(
            uid = userIdLong,
            tag = selectedTag ?: "",
            offset = currentOffset,
            limit = BOOKMARK_NOVEL_PAGE_SIZE,
            rest = rest
        )
        
        if (response.error) {
            updateTabData(tab) { 
                it.copy(isLoading = false, error = response.message ?: "Failed to load") 
            }
            return
        }
        
        val body = response.body ?: run {
            updateTabData(tab) { it.copy(isLoading = false, error = "Data is empty") }
            return
        }
        
        val newNovels = body.works.map { novel ->
            novel.toNovel(
                tagTranslation = null,
                ageLimitDeterminer = ageLimitDeterminer
            )
        }
        
        // 同步全局状态缓存
        val syncedNovels = syncNovelStatesUseCase(newNovels)
        
        val newOffset = currentOffset + syncedNovels.size
        val hasMore = newOffset < body.total
        
        updateTabData(tab) {
            it.copy(
                novels = it.novels + syncedNovels,
                offset = newOffset,
                total = body.total,
                isLoading = false,
                hasMore = hasMore
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
     * 刷新当前Tab（下拉刷新）
     */
    fun refresh() {
        // 如果用户信息加载失败，重新加载用户信息
        if (_state.value.profileError != null && _state.value.userProfile.userId.isEmpty()) {
            retryLoadUser()
            return
        }
        
        val currentTab = _state.value.currentTab
        
        // 如果已经在刷新中，不重复触发
        val tabData = _state.value.tabDataCache[currentTab]
        if (tabData?.isRefreshing == true) return
        
        screenModelScope.launch {
            // 设置刷新状态
            updateTabData(currentTab) {
                it.copy(
                    isRefreshing = true,
                    loadedIds = emptyList(),
                    artworks = emptyList(),
                    novels = emptyList(),
                    users = emptyList(),  // 清空推荐用户列表
                    hasMore = true,
                    error = null,
                    offset = 0  // 重置收藏Tab的偏移量
                )
            }
            
            try {
                // 重新加载数据
                when (currentTab) {
                    UserProfileTab.ILLUSTS, UserProfileTab.MANGA -> {
                        loadIllustOrManga(currentTab)
                    }
                    UserProfileTab.NOVELS -> {
                        loadNovels(currentTab)
                    }
                    UserProfileTab.BOOKMARK_ILLUSTS_PUBLIC,
                    UserProfileTab.BOOKMARK_ILLUSTS_PRIVATE -> {
                        loadBookmarkIllusts(currentTab)
                    }
                    UserProfileTab.BOOKMARK_NOVELS_PUBLIC,
                    UserProfileTab.BOOKMARK_NOVELS_PRIVATE -> {
                        loadBookmarkNovels(currentTab)
                    }
                    UserProfileTab.RECOMMEND_USERS -> {
                        loadRecommendUsers(currentTab)
                    }
                    else -> {
                        // 系列等Tab不需要刷新
                        updateTabData(currentTab) { it.copy(isRefreshing = false) }
                    }
                }
            } catch (e: Exception) {
                updateTabData(currentTab) { 
                    it.copy(isRefreshing = false, isLoading = false, error = e.message ?: "Failed to refresh") 
                }
            } finally {
                // 确保刷新状态被重置
                updateTabData(currentTab) { it.copy(isRefreshing = false) }
            }
        }
    }
    
    /**
     * 重试加载用户信息（用于初始加载失败后的重试）
     */
    fun retryLoadUser() {
        val userId = currentUserId
        if (userId.isNotEmpty()) {
            // 重置状态以允许重新加载
            currentUserId = ""
            loadUser(userId)
        }
    }
    
    /**     * 加载推荐用户
     */
    private suspend fun loadRecommendUsers(tab: UserProfileTab) {
        val userIdLong = currentUserId.toLongOrNull() ?: return
        
        val response = pixivApi.userApi.getRecommendUsers(
            uid = userIdLong,
            userNum = 20,
            workNum = 3,
            isR18 = true
        )
        
        if (response.error) {
            updateTabData(tab) { 
                it.copy(isLoading = false, error = response.message ?: "Failed to load") 
            }
            return
        }
        
        val body = response.body ?: run {
            updateTabData(tab) { 
                it.copy(isLoading = false, error = "No data") 
            }
            return
        }
        
        // 从 users 字段获取用户详细信息，并转换为 User 对象
        val recommendUsers = body.users?.map { userDetail ->
            val user = userDetail.toUser()
            
            // 从 thumbnails 中查找该用户的作品
            val userIllusts = body.thumbnails?.illust?.filter { 
                it.userId == userDetail.userId 
            }?.map { illust ->
                illust.toArtwork(
                    tagTranslationUtil = tagTranslationUtil,
                    tagTranslation = null,
                    ageLimitDeterminer = ageLimitDeterminer
                )
            } ?: emptyList()
            
            // 将作品添加到用户对象中
            user.copy(illusts = userIllusts)
        } ?: emptyList()
        
        updateTabData(tab) {
            it.copy(
                users = recommendUsers,
                isLoading = false,
                hasMore = false  // 推荐用户不支持分页
            )
        }
    }
    
    /**     * 更新Tab数据
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
    
    /**
     * 切换Tag筛选弹窗的开关状态
     * 打开时加载Tag数据
     */
    fun toggleTagFilter(tab: UserProfileTab) {
        // 如果 tabDataCache 中没有该 Tab 的数据，先创建一个默认的
        val tabData = _state.value.tabDataCache[tab] ?: TabData()
        val newOpen = !tabData.isTagDialogOpen
        
        updateTabData(tab) { it.copy(isTagDialogOpen = newOpen) }
        
        // 打开时加载Tag数据（如果还没有加载）
        if (newOpen && tabData.bookmarkTags.isEmpty() && !tabData.isLoadingTags) {
            loadBookmarkTags(tab)
        }
    }
    
    /**
     * 加载收藏标签
     */
    private fun loadBookmarkTags(tab: UserProfileTab) {
        if (!tab.isBookmarkTab()) return
        
        screenModelScope.launch {
            updateTabData(tab) { it.copy(isLoadingTags = true) }
            
            try {
                val isPublic = when (tab) {
                    UserProfileTab.BOOKMARK_ILLUSTS_PUBLIC,
                    UserProfileTab.BOOKMARK_NOVELS_PUBLIC -> true
                    else -> false
                }
                
                val isIllust = when (tab) {
                    UserProfileTab.BOOKMARK_ILLUSTS_PUBLIC,
                    UserProfileTab.BOOKMARK_ILLUSTS_PRIVATE -> true
                    else -> false
                }
                
                val userIdLong = currentUserId.toLongOrNull() ?: return@launch
                val response = if (isIllust) {
                    pixivApi.bookmarkApi.getIllustBookmarkTags(userIdLong)
                } else {
                    pixivApi.bookmarkApi.getNovelBookmarkTags(userIdLong)
                }
                
                if (response.error) {
                    updateTabData(tab) { it.copy(isLoadingTags = false) }
                    return@launch
                }
                
                val body = response.body ?: run {
                    updateTabData(tab) { it.copy(isLoadingTags = false) }
                    return@launch
                }
                
                // 根据公开/私人选择对应的标签列表，并倒序排列
                val tags = if (isPublic) body.public else body.private
                val bookmarkTags = tags.reversed().map { BookmarkTagData(tag = it.tag, count = it.cnt) }
                
                updateTabData(tab) {
                    it.copy(
                        bookmarkTags = bookmarkTags,
                        isLoadingTags = false
                    )
                }
            } catch (e: Exception) {
                updateTabData(tab) { it.copy(isLoadingTags = false) }
            }
        }
    }
    
    /**
     * 选择/取消选择Tag进行筛选
     * @param tab 当前Tab
     * @param tag 选择的标签，传null表示取消筛选
     */
    fun selectTag(tab: UserProfileTab, tag: String?) {
        val tabData = _state.value.tabDataCache[tab] ?: return
        
        // 如果点击的是已选中的Tag，则取消选择
        val newSelectedTag = if (tabData.selectedTag == tag) null else tag
        
        // 更新选中状态，并清空列表数据以便重新加载
        updateTabData(tab) {
            it.copy(
                selectedTag = newSelectedTag,
                artworks = emptyList(),
                novels = emptyList(),
                offset = 0,
                hasMore = true,
                isLoading = true,
                error = null
            )
        }
        
        // 重新加载数据
        screenModelScope.launch {
            try {
                when (tab) {
                    UserProfileTab.BOOKMARK_ILLUSTS_PUBLIC,
                    UserProfileTab.BOOKMARK_ILLUSTS_PRIVATE -> {
                        loadBookmarkIllusts(tab)
                    }
                    UserProfileTab.BOOKMARK_NOVELS_PUBLIC,
                    UserProfileTab.BOOKMARK_NOVELS_PRIVATE -> {
                        loadBookmarkNovels(tab)
                    }
                    else -> {
                        updateTabData(tab) { it.copy(isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                updateTabData(tab) {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load")
                }
            }
        }
    }
}

/**
 * 判断Tab是否为收藏类型
 */
fun UserProfileTab.isBookmarkTab(): Boolean = when (this) {
    UserProfileTab.BOOKMARK_ILLUSTS_PUBLIC,
    UserProfileTab.BOOKMARK_ILLUSTS_PRIVATE,
    UserProfileTab.BOOKMARK_NOVELS_PUBLIC,
    UserProfileTab.BOOKMARK_NOVELS_PRIVATE -> true
    else -> false
}
