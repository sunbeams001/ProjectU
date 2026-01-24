package com.projectu.ui.screens.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projectu.shared.data.share.ShareExecutor
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.ArtworkShareType
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.ShareResult
import com.projectu.shared.domain.usecase.PrepareShareContentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 分享状态
 */
data class ShareState(
    val isSharing: Boolean = false,
    val shareResult: ShareResult? = null,
    val errorMessage: String? = null
)

/**
 * 分享意图
 */
sealed interface ShareIntent {
    /**
     * 分享作品
     */
    data class ShareArtwork(
        val artwork: Artwork,
        val formattedText: String,
        val shareType: ArtworkShareType,
        val pageIndex: Int = 0
    ) : ShareIntent
    
    /**
     * 分享用户
     */
    data class ShareUser(
        val userId: String,
        val userName: String,
        val formattedDescription: String
    ) : ShareIntent
    
    /**
     * 分享小说
     */
    data class ShareNovel(
        val novel: Novel,
        val formattedDescription: String
    ) : ShareIntent
    
    /**
     * 清除结果
     */
    data object DismissResult : ShareIntent
}

/**
 * 分享ViewModel
 * 
 * 职责：
 * - 处理分享意图
 * - 调用UseCase准备分享数据
 * - 调用ShareExecutor执行分享
 * - 管理分享状态
 * 
 * 注：文本格式化由UI层负责，通过ShareIntent传递格式化后的文本
 */
class ShareViewModel(
    private val prepareShareUseCase: PrepareShareContentUseCase,
    private val shareExecutor: ShareExecutor
) : ViewModel() {
    
    private val _state = MutableStateFlow(ShareState())
    val state: StateFlow<ShareState> = _state.asStateFlow()
    
    /**
     * 处理分享意图
     */
    fun handleIntent(intent: ShareIntent) {
        when (intent) {
            is ShareIntent.ShareArtwork -> {
                shareArtwork(intent.artwork, intent.formattedText, intent.shareType, intent.pageIndex)
            }
            is ShareIntent.ShareUser -> {
                shareUser(intent.userId, intent.userName, intent.formattedDescription)
            }
            is ShareIntent.ShareNovel -> {
                shareNovel(intent.novel, intent.formattedDescription)
            }
            is ShareIntent.DismissResult -> {
                _state.update { it.copy(shareResult = null, errorMessage = null) }
            }
        }
    }
    
    /**
     * 分享作品
     */
    private fun shareArtwork(
        artwork: Artwork,
        formattedText: String,
        shareType: ArtworkShareType,
        pageIndex: Int
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSharing = true, shareResult = null, errorMessage = null) }
            
            try {
                val shareData = prepareShareUseCase.prepareArtworkShare(
                    artwork = artwork,
                    formattedText = formattedText,
                    shareType = shareType,
                    pageIndex = pageIndex
                )
                
                val result = shareExecutor.executeShare(shareData)
                
                _state.update { 
                    it.copy(
                        isSharing = false,
                        shareResult = result,
                        errorMessage = if (result is ShareResult.Error) result.message else null
                    )
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isSharing = false,
                        shareResult = ShareResult.Error(e.message ?: "Unknown error"),
                        errorMessage = e.message
                    )
                }
            }
        }
    }
    
    /**
     * 分享用户
     */
    private fun shareUser(
        userId: String, 
        userName: String,
        formattedDescription: String
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSharing = true, shareResult = null, errorMessage = null) }
            
            try {
                val shareData = prepareShareUseCase.prepareUserShare(
                    userId, 
                    userName, 
                    formattedDescription
                )
                val result = shareExecutor.executeShare(shareData)
                
                _state.update { 
                    it.copy(
                        isSharing = false,
                        shareResult = result,
                        errorMessage = if (result is ShareResult.Error) result.message else null
                    )
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isSharing = false,
                        shareResult = ShareResult.Error(e.message ?: "Unknown error"),
                        errorMessage = e.message
                    )
                }
            }
        }
    }
    
    /**
     * 分享小说
     */
    private fun shareNovel(novel: Novel, formattedDescription: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSharing = true, shareResult = null, errorMessage = null) }
            
            try {
                val shareData = prepareShareUseCase.prepareNovelShare(
                    novel,
                    formattedDescription
                )
                val result = shareExecutor.executeShare(shareData)
                
                _state.update { 
                    it.copy(
                        isSharing = false,
                        shareResult = result,
                        errorMessage = if (result is ShareResult.Error) result.message else null
                    )
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isSharing = false,
                        shareResult = ShareResult.Error(e.message ?: "Unknown error"),
                        errorMessage = e.message
                    )
                }
            }
        }
    }
}
