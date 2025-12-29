package com.projectu.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projectu.shared.domain.model.BrowseHistoryItem
import com.projectu.shared.domain.model.HistoryContentType
import com.projectu.shared.domain.repository.BrowseHistoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 浏览历史页面 State
 */
data class BrowseHistoryScreenState(
    val allHistoryItems: List<BrowseHistoryItem> = emptyList(),
    val filteredHistoryItems: List<BrowseHistoryItem> = emptyList(),
    val selectedFilter: HistoryFilter = HistoryFilter.ALL,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 历史记录筛选类型
 */
enum class HistoryFilter(val contentType: HistoryContentType?) {
    ALL(null),                              // 全部
    ILLUST(HistoryContentType.ILLUST),      // 插画
    MANGA(HistoryContentType.MANGA),        // 漫画
    UGOIRA(HistoryContentType.UGOIRA),      // 动图
    NOVEL(HistoryContentType.NOVEL),        // 小说
    NOVEL_SERIES(HistoryContentType.NOVEL_SERIES), // 小说系列
    MANGA_SERIES(HistoryContentType.MANGA_SERIES); // 漫画系列
}

/**
 * 浏览历史页面 Intent
 */
sealed interface BrowseHistoryIntent {
    data class FilterByType(val filter: HistoryFilter) : BrowseHistoryIntent
    data class DeleteHistoryItem(val id: String) : BrowseHistoryIntent
    data object ClearAllHistory : BrowseHistoryIntent
    data class ClearHistoryByType(val filter: HistoryFilter) : BrowseHistoryIntent
}

/**
 * 浏览历史页面 ViewModel
 */
class BrowseHistoryViewModel(
    private val browseHistoryRepository: BrowseHistoryRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(BrowseHistoryScreenState())
    val state: StateFlow<BrowseHistoryScreenState> = _state.asStateFlow()
    
    init {
        loadHistory()
    }
    
    /**
     * 处理Intent
     */
    fun handleIntent(intent: BrowseHistoryIntent) {
        when (intent) {
            is BrowseHistoryIntent.FilterByType -> filterByType(intent.filter)
            is BrowseHistoryIntent.DeleteHistoryItem -> deleteHistoryItem(intent.id)
            is BrowseHistoryIntent.ClearAllHistory -> clearAllHistory()
            is BrowseHistoryIntent.ClearHistoryByType -> clearHistoryByType(intent.filter)
        }
    }
    
    /**
     * 加载浏览历史
     */
    private fun loadHistory() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            browseHistoryRepository.getAllHistory()
                .catch { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                }
                .collect { historyItems ->
                    _state.update { currentState ->
                        val filtered = filterItems(historyItems, currentState.selectedFilter)
                        currentState.copy(
                            allHistoryItems = historyItems,
                            filteredHistoryItems = filtered,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }
    
    /**
     * 按类型筛选
     */
    private fun filterByType(filter: HistoryFilter) {
        _state.update { currentState ->
            val filtered = filterItems(currentState.allHistoryItems, filter)
            currentState.copy(
                selectedFilter = filter,
                filteredHistoryItems = filtered
            )
        }
    }
    
    /**
     * 删除单条历史记录
     */
    private fun deleteHistoryItem(id: String) {
        viewModelScope.launch {
            browseHistoryRepository.deleteHistoryById(id)
        }
    }
    
    /**
     * 清空所有历史记录
     */
    private fun clearAllHistory() {
        viewModelScope.launch {
            browseHistoryRepository.clearAllHistory()
        }
    }
    
    /**
     * 清空指定类型的历史记录
     */
    private fun clearHistoryByType(filter: HistoryFilter) {
        viewModelScope.launch {
            if (filter == HistoryFilter.ALL) {
                browseHistoryRepository.clearAllHistory()
            } else {
                filter.contentType?.let { type ->
                    browseHistoryRepository.deleteHistoryByType(type)
                }
            }
        }
    }
    
    /**
     * 筛选历史记录列表
     */
    private fun filterItems(items: List<BrowseHistoryItem>, filter: HistoryFilter): List<BrowseHistoryItem> {
        return if (filter == HistoryFilter.ALL) {
            items
        } else {
            items.filter { it.contentType == filter.contentType }
        }
    }
}
