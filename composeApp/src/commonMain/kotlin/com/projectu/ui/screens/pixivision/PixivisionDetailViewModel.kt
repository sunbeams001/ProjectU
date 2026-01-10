package com.projectu.ui.screens.pixivision

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.projectu.shared.data.local.SettingsCache
import com.projectu.shared.domain.model.pixivision.PixivisionDetail
import com.projectu.shared.domain.usecase.GetPixivisionDetailUseCase
import com.projectu.shared.domain.repository.ArtworkRepository
import com.projectu.shared.data.cache.ArtworkCacheManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Pixivision 详情页面 ViewModel
 */
class PixivisionDetailViewModel(
    private val getPixivisionDetailUseCase: GetPixivisionDetailUseCase,
    private val settingsCache: SettingsCache,
    private val artworkRepository: ArtworkRepository,
    private val artworkCacheManager: ArtworkCacheManager
) : ScreenModel {
    
    // UI 状态
    private val _state = MutableStateFlow(PixivisionDetailState())
    val state: StateFlow<PixivisionDetailState> = _state.asStateFlow()
    
    /**
     * 加载文章详情
     */
    fun loadArticleDetail(articleId: String) {
        if (_state.value.isLoading) return
        
        _state.update { it.copy(isLoading = true, error = null) }
        
        screenModelScope.launch {
            val lang = getPixivisionLanguageCode()
            
            getPixivisionDetailUseCase(articleId, lang)
                .onSuccess { detail ->
                    _state.update {
                        it.copy(
                            detail = detail,
                            isLoading = false,
                            error = null
                        )
                    }
                    
                    // 批量加载作品详情
                    loadArtworks(detail.artworkIds)
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Unknown error"
                        )
                    }
                }
        }
    }
    
    /**
     * 重试加载
     */
    fun retry(articleId: String) {
        loadArticleDetail(articleId)
    }
    
    /**
     * 批量加载作品详情到缓存
     */
    private suspend fun loadArtworks(artworkIds: List<String>) {
        screenModelScope.launch {
            artworkIds.forEach { artworkId ->
                try {
                    val artworkIdLong = artworkId.toLongOrNull()
                    if (artworkIdLong != null) {
                        // 从API获取作品详情
                        artworkRepository.getArtworkDetail(artworkIdLong)
                            .onSuccess { artwork ->
                                // 添加到缓存
                                artworkCacheManager.cacheArtworkDetail(artwork)
                            }
                    }
                } catch (e: Exception) {
                    // 静默处理异常
                }
            }
        }
    }
    
    /**
     * 获取 Pixivision 语言代码
     * 根据应用语言转换为 Pixivision 支持的语言
     */
    private fun getPixivisionLanguageCode(): String {
        return when (settingsCache.getAppLanguage()) {
            com.projectu.shared.data.local.AppLanguage.SIMPLIFIED_CHINESE -> "zh"
            com.projectu.shared.data.local.AppLanguage.TRADITIONAL_CHINESE -> "zh-tw"
            com.projectu.shared.data.local.AppLanguage.ENGLISH -> "en"
            com.projectu.shared.data.local.AppLanguage.JAPANESE -> "ja"
            com.projectu.shared.data.local.AppLanguage.KOREAN -> "ko"
        }
    }
}

/**
 * Pixivision 详情页面状态
 */
data class PixivisionDetailState(
    val detail: PixivisionDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
