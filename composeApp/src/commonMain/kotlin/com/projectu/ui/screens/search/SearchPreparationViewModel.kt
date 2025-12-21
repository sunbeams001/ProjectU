package com.projectu.ui.screens.search

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.projectu.shared.data.local.SearchHistoryStore
import com.projectu.shared.data.remote.api.TagApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 搜索准备页面 ViewModel
 */
class SearchPreparationViewModel(
    private val tagApi: TagApi,
    private val searchHistoryStore: SearchHistoryStore,
    private val tagTranslationUtil: com.projectu.shared.util.TagTranslationUtil
) : ScreenModel {
    
    private val _state = MutableStateFlow(SearchPreparationState())
    val state: StateFlow<SearchPreparationState> = _state.asStateFlow()
    
    private var autocompleteJob: Job? = null
    
    init {
        // 加载搜索历史
        screenModelScope.launch {
            searchHistoryStore.searchHistory.collect { history ->
                _state.update { it.copy(searchHistory = history) }
            }
        }
        
        // 加载搜索建议
        loadSearchRecommendations()
    }
    
    /**
     * 加载搜索建议（搜索准备页）
     */
    private fun loadSearchRecommendations() {
        screenModelScope.launch {
            _state.update { it.copy(isLoadingRecommendations = true, recommendationsError = null) }
            try {
                val result = tagApi.getSearchRecommendations(mode = "all")
                if (!result.error && result.body != null) {
                    val body = result.body!!
                    val tagTranslation = body.tagTranslation.mapKeys { it.key }.mapValues { entry ->
                        mapOf(
                            "zh" to entry.value.zh,
                            "zh_tw" to entry.value.zhTw,
                            "en" to entry.value.en,
                            "ko" to entry.value.ko,
                            "romaji" to entry.value.romaji
                        )
                    }
                    
                    _state.update { 
                        it.copy(
                            searchRecommendations = body,
                            isLoadingRecommendations = false,
                            // 翻译我的收藏标签
                            myFavoriteTags = tagTranslationUtil.translateTags(body.myFavoriteTags, tagTranslation),
                            // 翻译热门标签
                            popularIllustTags = body.popularTags?.illust?.map { tag -> 
                                tagTranslationUtil.translateTag(tag.tag, tagTranslation)
                            } ?: emptyList(),
                            popularNovelTags = body.popularTags?.novel?.map { tag -> 
                                tagTranslationUtil.translateTag(tag.tag, tagTranslation)
                            } ?: emptyList(),
                            // 翻译推荐标签
                            recommendedTags = body.recommendTags?.illust?.map { tag -> 
                                tagTranslationUtil.translateTag(tag.tag, tagTranslation)
                            } ?: emptyList(),
                            // 翻译基于收藏的推荐标签
                            bookmarkRecommendedTags = body.recommendByTags?.illust?.map { tag -> 
                                tagTranslationUtil.translateTag(tag.tag, tagTranslation)
                            } ?: emptyList()
                        )
                    }
                } else {
                    _state.update { 
                        it.copy(
                            isLoadingRecommendations = false,
                            recommendationsError = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoadingRecommendations = false,
                        recommendationsError = e.message ?: "Failed to load recommendations"
                    )
                }
            }
        }
    }
    
    /**
     * 搜索关键词变化
     */
    fun onSearchKeywordChange(keywordValue: androidx.compose.ui.text.input.TextFieldValue) {
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
            // TagSearchCandidate的tagTranslation是字符串，直接使用Tag对象
            val translatedTags = result.candidates.map { candidate ->
                com.projectu.shared.domain.model.Tag(
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
            // 忽略错误，不影响主流程
            _state.update { it.copy(isLoadingAutocomplete = false) }
        }
    }
    
    /**
     * 点击自动补全建议
     */
    fun onAutocompleteSuggestionClick(tag: com.projectu.shared.domain.model.Tag) {
        val currentKeyword = _state.value.searchKeyword.text
        val words = currentKeyword.split(" ").toMutableList()
        
        if (words.isNotEmpty()) {
            // 使用原始标签名（name），而非翻译后的名称
            words[words.lastIndex] = tag.name
            val newKeyword = words.joinToString(" ") + " "
            _state.update { 
                it.copy(
                    searchKeyword = androidx.compose.ui.text.input.TextFieldValue(
                        text = newKeyword,
                        selection = androidx.compose.ui.text.TextRange(newKeyword.length)
                    ),
                    autocompleteSuggestions = emptyList()
                )
            }
        }
    }
    
    /**
     * 点击搜索历史
     */
    fun onHistoryClick(keyword: String) {
        _state.update { 
            it.copy(
                searchKeyword = androidx.compose.ui.text.input.TextFieldValue(
                    text = keyword,
                    selection = androidx.compose.ui.text.TextRange(keyword.length)
                )
            )
        }
    }
    
    /**
     * 删除单个搜索历史
     */
    fun removeHistory(keyword: String) {
        screenModelScope.launch {
            searchHistoryStore.removeHistory(keyword)
            // 主动刷新历史列表
            val latestHistory = searchHistoryStore.getHistoryList()
            _state.update { it.copy(searchHistory = latestHistory) }
        }
    }
    
    /**
     * 固定/取消固定搜索历史
     */
    fun togglePinHistory(keyword: String) {
        screenModelScope.launch {
            searchHistoryStore.togglePin(keyword)
            // 主动刷新历史列表
            val latestHistory = searchHistoryStore.getHistoryList()
            _state.update { it.copy(searchHistory = latestHistory) }
        }
    }
    
    /**
     * 点击搜索建议标签
     */
    fun onRecommendationTagClick(tag: com.projectu.shared.domain.model.Tag) {
        // 使用原始标签名（name），而非翻译后的名称
        _state.update { 
            it.copy(
                searchKeyword = androidx.compose.ui.text.input.TextFieldValue(
                    text = tag.name,
                    selection = androidx.compose.ui.text.TextRange(tag.name.length)
                )
            )
        }
    }
    
    /**
     * 清空搜索历史
     */
    fun clearHistory() {
        screenModelScope.launch {
            searchHistoryStore.clearHistory()
            // 清空后立即刷新UI
            refreshHistory()
        }
    }
    
    /**
     * 执行搜索（添加到历史并返回关键词）
     */
    suspend fun performSearch(): String? {
        val keyword = _state.value.searchKeyword.text.trim()
        if (keyword.isBlank()) {
            return null
        }
        
        // 保存到搜索历史（等待完成）
        searchHistoryStore.addHistory(keyword)
        
        // 主动读取最新历史并更新 state（不依赖 Flow）
        val latestHistory = searchHistoryStore.getHistoryList()
        _state.update { it.copy(searchHistory = latestHistory) }
        
        return keyword
    }
    
    /**
     * 刷新搜索建议
     */
    fun refreshRecommendations() {
        loadSearchRecommendations()
    }
    
    /**
     * 刷新搜索历史
     * 用于页面重新显示时同步最新的历史记录
     */
    fun refreshHistory() {
        screenModelScope.launch {
            val latestHistory = searchHistoryStore.getHistoryList()
            _state.update { it.copy(searchHistory = latestHistory) }
        }
    }
}
