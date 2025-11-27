package com.projectu.ui.screens.artwork

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.FollowStatus
import com.projectu.shared.domain.repository.ArtworkRepository
import com.projectu.shared.domain.repository.UserRepository
import com.projectu.shared.domain.usecase.SyncArtworkStatesUseCase
import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.data.cache.StateCacheEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 作品详情页 ViewModel
 */
class ArtworkDetailViewModel(
    private val artworkRepository: ArtworkRepository,
    private val userRepository: UserRepository,
    private val syncArtworkStatesUseCase: SyncArtworkStatesUseCase,
    private val stateCacheManager: StateCacheManager
) : ScreenModel {

    private val _state = MutableStateFlow(ArtworkDetailState())
    val state: StateFlow<ArtworkDetailState> = _state.asStateFlow()

    init {
        // 监听全局状态变更
        screenModelScope.launch {
            stateCacheManager.stateChangeEvents.collect { event ->
                when (event) {
                    is StateCacheEvent.ArtworkBookmarkChanged -> {
                        // 如果是当前作品，更新状态
                        val currentArtwork = _state.value.artwork
                        if (currentArtwork?.id == event.artworkId) {
                            _state.update {
                                it.copy(
                                    artwork = currentArtwork.copy(
                                        bookmarkStatus = event.status,
                                        bookmarkId = event.bookmarkId
                                    )
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
     * 加载作品详情
     */
    fun loadArtworkDetail(artworkId: String) {
        val currentArtwork = _state.value.artwork
        val isLoadingNow = _state.value.isLoading
        
        // 如果已经在加载，跳过
        if (isLoadingNow) {
            return
        }
        
        // 如果已有数据且ID相同，跳过
        if (currentArtwork?.id == artworkId) {
            return
        }
        
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val artworkIdLong = artworkId.toLongOrNull()
                if (artworkIdLong == null) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "无效的作品ID"
                        )
                    }
                    return@launch
                }
                
                // 1. 获取作品详情
                var artwork = artworkRepository.getArtworkDetail(artworkIdLong).getOrThrow()

                // 2. 如果是多页作品，获取所有页面信息
                if (artwork.pageCount > 1) {
                    artwork = artworkRepository.getArtworkPages(artwork).getOrThrow()
                }

                // 3. 如果头像URL为空，从用户信息接口获取
                if (artwork.userProfileImageUrl.isEmpty()) {
                    try {
                        val userId = artwork.userId.toLongOrNull()
                        if (userId != null) {
                            val userInfo = userRepository.getUserById(userId).getOrNull()
                            if (userInfo != null && userInfo.profileImageUrl.isNotEmpty()) {
                                artwork = artwork.copy(userProfileImageUrl = userInfo.profileImageUrl)
                            }
                        }
                    } catch (e: Exception) {
                        // 不影响主流程，继续执行
                    }
                }

                // 4. 同步全局状态缓存
                syncArtworkStatesUseCase(listOf(artwork))

                // 5. 获取作者关注状态（从缓存）
                val userStates = stateCacheManager.getUserStates(listOf(artwork.userId))
                val followStatus = userStates[artwork.userId]?.followStatus ?: FollowStatus.NOT_FOLLOWING

                _state.update {
                    it.copy(
                        artwork = artwork,
                        authorFollowStatus = followStatus,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败"
                    )
                }
            }
        }
    }
}

/**
 * 作品详情页状态
 */
data class ArtworkDetailState(
    val artwork: Artwork? = null,
    val authorFollowStatus: FollowStatus = FollowStatus.NOT_FOLLOWING,
    val isLoading: Boolean = false,
    val error: String? = null
)
