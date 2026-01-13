package com.projectu.ui.screens.discovery

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.projectu.shared.data.cache.StateCacheEvent
import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.domain.model.FollowStatus
import com.projectu.shared.domain.model.User
import com.projectu.shared.domain.repository.UserRepository
import com.projectu.shared.domain.usecase.SyncUserFollowDetailsUseCase
import com.projectu.shared.domain.usecase.SyncUserStatesUseCase
import com.projectu.ui.navigation.ArtworkListSource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 发现用户页面 ViewModel
 * MVI 架构模式
 */
class DiscoveryUsersViewModel(
    private val userRepository: UserRepository,
    private val syncUserStatesUseCase: SyncUserStatesUseCase,
    private val syncUserFollowDetailsUseCase: SyncUserFollowDetailsUseCase,
    private val stateCacheManager: StateCacheManager
) : ScreenModel {
    
    // UI 状态
    private val _state = MutableStateFlow(DiscoveryUsersState())
    val state: StateFlow<DiscoveryUsersState> = _state.asStateFlow()
    
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
     * 创建用户作品预览的 ArtworkListSource
     * 
     * 用户卡片上显示的作品预览，点击后需要导航到作品详情页。
     * 这个方法返回包含所有用户作品预览ID的列表源。
     * 
     * @return 包含所有用户作品预览的 ArtworkListSource
     */
    fun createArtworkListSource(): ArtworkListSource {
        return object : ArtworkListSource {
            override val artworkIdsFlow: StateFlow<List<String>> = state.map { currentState ->
                currentState.users.flatMap { user ->
                    user.illusts.map { it.id }
                }
            }.stateIn(
                scope = screenModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = state.value.users.flatMap { user ->
                    user.illusts.map { it.id }
                }
            )
            
            override fun loadMoreArtworks() {
                loadMore()
            }
        }
    }
    
    /**
     * 初始化加载（惰性加载）
     */
    fun initLoadIfNeeded() {
        if (_state.value.users.isEmpty() && !_state.value.isLoading && _state.value.error == null) {
            loadUsers()
        }
    }
    
    /**
     * 加载更多用户
     */
    fun loadMore() {
        if (_state.value.isLoading || _state.value.isLoadingMore) return
        
        _state.update { it.copy(isLoadingMore = true) }
        loadUsers(append = true)
    }
    
    /**
     * 刷新数据
     */
    fun refresh() {
        _state.update {
            it.copy(
                users = emptyList(),
                isLoading = true,
                error = null
            )
        }
        loadUsers()
    }
    
    /**
     * 加载用户
     */
    private fun loadUsers(append: Boolean = false) {
        screenModelScope.launch {
            userRepository.getDiscoveryUsers(limit = 20)
                .onSuccess { newUsers ->
                    // 第一步：应用全局状态缓存
                    val syncedUsers = syncUserStatesUseCase(newUsers)
                    
                    // 第二步：精确同步已关注用户的关注状态（公开/悄悄关注）
                    // 只对已关注的用户调用接口，减少性能开销
                    val detailedUsers = syncUserFollowDetailsUseCase(syncedUsers)
                    
                    _state.update { currentState ->
                        // 如果是追加模式，需要去重
                        val updatedUsers = if (append) {
                            val existingIds = currentState.users.map { it.id }.toSet()
                            val uniqueNewUsers = detailedUsers.filter { it.id !in existingIds }
                            currentState.users + uniqueNewUsers
                        } else {
                            detailedUsers
                        }
                        
                        currentState.copy(
                            users = updatedUsers,
                            isLoading = false,
                            isLoadingMore = false,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = error.message ?: "Unknown error"
                        )
                    }
                }
        }
    }
    
    /**
     * 更新列表中用户的关注状态
     * 由全局状态变更事件触发
     */
    private fun updateUserFollowStatus(
        userId: String,
        status: FollowStatus
    ) {
        _state.update { currentState ->
            currentState.copy(
                users = currentState.users.map { user ->
                    if (user.id == userId) {
                        user.copy(followStatus = status)
                    } else {
                        user
                    }
                }
            )
        }
    }
}

/**
 * 发现用户页面状态
 */
data class DiscoveryUsersState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null
)
