package com.projectu.ui.screens.search

import androidx.compose.ui.text.input.TextFieldValue
import com.projectu.shared.data.remote.dto.tag.SearchSuggestionBody
import com.projectu.shared.domain.model.Tag
import com.projectu.shared.data.local.SearchHistoryItem

/**
 * 搜索准备页面状态
 */
data class SearchPreparationState(
    // 搜索关键词（使用TextFieldValue以支持光标控制）
    val searchKeyword: TextFieldValue = TextFieldValue(""),
    
    // 搜索历史（使用SearchHistoryItem）
    val searchHistory: List<SearchHistoryItem> = emptyList(),
    
    // 搜索建议
    val searchRecommendations: SearchSuggestionBody? = null,
    val isLoadingRecommendations: Boolean = false,
    val recommendationsError: String? = null,
    
    // 标签自动补全（已翻译）
    val autocompleteSuggestions: List<Tag> = emptyList(),
    val isLoadingAutocomplete: Boolean = false,
    
    // 用户收藏标签（已翻译）
    val myFavoriteTags: List<Tag> = emptyList(),
    
    // 热门标签（已翻译）
    val popularIllustTags: List<Tag> = emptyList(),
    val popularNovelTags: List<Tag> = emptyList(),
    
    // 推荐标签（已翻译）
    val recommendedTags: List<Tag> = emptyList(),
    
    // 基于收藏的推荐标签（已翻译）
    val bookmarkRecommendedTags: List<Tag> = emptyList()
)
