package com.projectu.ui.screens.discovery

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.projectu.shared.data.local.SettingsCache
import com.projectu.shared.data.remote.api.PixivApi
import com.projectu.shared.data.remote.dto.pixivision.PixivisionArticle
import com.projectu.shared.data.remote.dto.pixivision.PixivisionCategory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 发现 Pixivision 文章页面 ViewModel
 * 支持插画和漫画两个类别，每个类别独立缓存数据
 */
class DiscoveryPixivisionViewModel(
    private val pixivApi: PixivApi,
    private val settingsCache: SettingsCache
) : ScreenModel {
    
    // UI 状态
    private val _state = MutableStateFlow(DiscoveryPixivisionState())
    val state: StateFlow<DiscoveryPixivisionState> = _state.asStateFlow()
    
    init {
        // 监听应用语言变化，清空缓存
        screenModelScope.launch {
            settingsCache.appLanguage.collect { newLanguage ->
                // 语言改变时清空所有类别的缓存
                _state.update {
                    it.copy(categoryDataCache = emptyMap())
                }
            }
        }
    }
    
    /**
     * 初始化加载（惰性加载）
     * 只在当前类别没有数据时才加载
     */
    fun initLoadIfNeeded() {
        val currentCategory = _state.value.currentCategory
        val categoryData = _state.value.categoryDataCache[currentCategory]
        
        if (categoryData == null && !_state.value.isLoading) {
            loadArticles()
        }
    }
    
    /**
     * 切换类别
     * 不清空数据，保持各类别独立的缓存
     */
    fun switchCategory(category: PixivisionCategory) {
        if (_state.value.currentCategory == category) return
        
        _state.update {
            it.copy(
                currentCategory = category,
                error = null
            )
        }
        
        // 只在该类别没有数据时才加载
        loadArticlesIfNeeded()
    }
    
    /**
     * 只在当前类别没有数据时加载
     */
    private fun loadArticlesIfNeeded() {
        val currentState = _state.value
        val categoryData = currentState.categoryDataCache[currentState.currentCategory]
        
        // 如果已有数据，不加载
        if (categoryData != null && categoryData.articles.isNotEmpty()) {
            return
        }
        
        // 否则开始加载
        _state.update { it.copy(isLoading = true, error = null) }
        loadArticles()
    }
    
    /**
     * 刷新当前类别的数据
     */
    fun refresh() {
        val currentCategory = _state.value.currentCategory
        
        _state.update {
            val updatedCache = it.categoryDataCache.toMutableMap()
            // 重置当前类别的数据，保持第一页
            updatedCache[currentCategory] = CategoryData(emptyList(), 1, true)
            it.copy(
                categoryDataCache = updatedCache,
                isLoading = true,
                error = null
            )
        }
        loadArticles()
    }
    
    /**
     * 加载更多文章（翻页）
     */
    fun loadMore() {
        val currentState = _state.value
        val currentCategory = currentState.currentCategory
        val categoryData = currentState.categoryDataCache[currentCategory]
        
        // 如果正在加载或没有更多数据，不加载
        if (currentState.isLoading || categoryData?.hasMore == false) {
            return
        }
        
        _state.update { it.copy(isLoadingMore = true, error = null) }
        
        screenModelScope.launch {
            try {
                val nextPage = (categoryData?.currentPage ?: 0) + 1
                val lang = getPixivisionLanguageCode()
                
                val response = pixivApi.pixivisionApi.getArticleList(
                    category = currentCategory,
                    lang = lang,
                    page = nextPage
                )
                
                _state.update { state ->
                    val updatedCache = state.categoryDataCache.toMutableMap()
                    val existingData = updatedCache[currentCategory] ?: CategoryData()
                    
                    // 合并新数据
                    val mergedArticles = existingData.articles + response.articles
                    
                    updatedCache[currentCategory] = CategoryData(
                        articles = mergedArticles,
                        currentPage = nextPage,
                        hasMore = response.articles.isNotEmpty() // 如果返回为空，说明没有更多数据
                    )
                    
                    state.copy(
                        categoryDataCache = updatedCache,
                        isLoadingMore = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingMore = false,
                        error = e.message
                    )
                }
            }
        }
    }
    
    /**
     * 加载文章（首次或刷新）
     */
    private fun loadArticles() {
        val currentCategory = _state.value.currentCategory
        
        screenModelScope.launch {
            try {
                val lang = getPixivisionLanguageCode()
                
                val response = pixivApi.pixivisionApi.getArticleList(
                    category = currentCategory,
                    lang = lang,
                    page = 1
                )
                
                _state.update { state ->
                    val updatedCache = state.categoryDataCache.toMutableMap()
                    updatedCache[currentCategory] = CategoryData(
                        articles = response.articles,
                        currentPage = 1,
                        hasMore = response.articles.isNotEmpty()
                    )
                    
                    state.copy(
                        categoryDataCache = updatedCache,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }
    
    /**
     * 获取 Pixivision 适配的语言代码
     * 将应用语言代码转换为 Pixivision 支持的语言代码
     */
    private fun getPixivisionLanguageCode(): String {
        val appLang = settingsCache.getAppLanguage()
        
        // AppLanguage 代码映射到 Pixivision 语言代码
        // AppLanguage: zh-CN, zh-TW, en, ja, ko
        // Pixivision: zh, zh-tw, en, ja, ko
        return when (appLang.code) {
            "zh-CN" -> "zh"      // 简体中文：zh-CN -> zh
            "zh-TW" -> "zh-tw"   // 繁体中文：zh-TW -> zh-tw
            "en" -> "en"         // 英语
            "ja" -> "ja"         // 日语
            "ko" -> "ko"         // 韩语
            else -> "en"         // 默认英语
        }
    }
}

/**
 * Pixivision 页面状态
 */
data class DiscoveryPixivisionState(
    val currentCategory: PixivisionCategory = PixivisionCategory.ILLUSTRATION,
    val categoryDataCache: Map<PixivisionCategory, CategoryData> = emptyMap(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null
) {
    /**
     * 获取当前类别的文章列表
     */
    fun getCurrentArticles(): List<PixivisionArticle> {
        return categoryDataCache[currentCategory]?.articles ?: emptyList()
    }
    
    /**
     * 获取当前类别是否有更多数据
     */
    fun getCurrentHasMore(): Boolean {
        return categoryDataCache[currentCategory]?.hasMore ?: true
    }
}

/**
 * 类别数据
 */
data class CategoryData(
    val articles: List<PixivisionArticle> = emptyList(),
    val currentPage: Int = 0,
    val hasMore: Boolean = true
)
