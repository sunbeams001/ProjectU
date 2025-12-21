package com.projectu.shared.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * 搜索历史项
 * @param keyword 搜索关键词
 * @param isPinned 是否固定
 * @param searchTimestamp 最后搜索时间戳
 * @param pinnedTimestamp 固定时间戳（仅在固定时有值）
 */
@Serializable
data class SearchHistoryItem(
    val keyword: String,
    val isPinned: Boolean = false,
    val searchTimestamp: Long = System.currentTimeMillis(),
    val pinnedTimestamp: Long? = null
)

/**
 * 搜索历史存储
 * 使用 DataStore 进行搜索历史持久化存储
 */
class SearchHistoryStore(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_SEARCH_HISTORY = stringPreferencesKey("search_history")
        private const val MAX_HISTORY_SIZE = 20
        private const val MAX_PINNED_SIZE = 10
    }
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true  // 强制序列化所有字段，包括默认值
    }
    
    /**
     * 搜索历史流
     */
    val searchHistory: Flow<List<SearchHistoryItem>> = dataStore.data.map { preferences ->
        val historyJson = preferences[KEY_SEARCH_HISTORY] ?: "[]"
        try {
            val items = json.decodeFromString<List<SearchHistoryItem>>(historyJson)
            // 排序：
            // 1. 固定的在前，按固定时间倒序
            // 2. 未固定的在后，按搜索时间倒序
            items.sortedWith(
                compareByDescending<SearchHistoryItem> { it.isPinned }
                    .thenByDescending { if (it.isPinned) it.pinnedTimestamp ?: 0L else it.searchTimestamp }
            )
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 添加搜索历史
     * @param keyword 搜索关键词
     */
    suspend fun addHistory(keyword: String) {
        val trimmedKeyword = keyword.trim()
        if (trimmedKeyword.isBlank()) return
        
        dataStore.edit { preferences ->
            val currentHistoryJson = preferences[KEY_SEARCH_HISTORY] ?: "[]"
            val currentHistory = try {
                json.decodeFromString<List<SearchHistoryItem>>(currentHistoryJson).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            
            // 查找是否已存在
            val existingItem = currentHistory.find { it.keyword == trimmedKeyword }
            
            if (existingItem != null) {
                // 如果已存在，移除旧的，添加更新后的（保持固定状态和固定时间）
                currentHistory.remove(existingItem)
                currentHistory.add(existingItem.copy(searchTimestamp = System.currentTimeMillis()))
            } else {
                // 添加新项
                currentHistory.add(SearchHistoryItem(
                    keyword = trimmedKeyword,
                    isPinned = false,
                    searchTimestamp = System.currentTimeMillis(),
                    pinnedTimestamp = null
                ))
            }
            
            // 排序：
            // 1. 固定的在前，按固定时间倒序
            // 2. 未固定的在后，按搜索时间倒序
            val sortedHistory = currentHistory.sortedWith(
                compareByDescending<SearchHistoryItem> { it.isPinned }
                    .thenByDescending { if (it.isPinned) it.pinnedTimestamp ?: 0L else it.searchTimestamp }
            )
            
            // 限制最大数量
            val limitedHistory = sortedHistory.take(MAX_HISTORY_SIZE)
            
            // 保存
            preferences[KEY_SEARCH_HISTORY] = json.encodeToString(limitedHistory)
        }
    }
    
    /**
     * 清空搜索历史（保留固定的历史）
     */
    suspend fun clearHistory() {
        dataStore.edit { preferences ->
            val historyJson = preferences[KEY_SEARCH_HISTORY] ?: "[]"
            val currentHistory = try {
                json.decodeFromString<List<SearchHistoryItem>>(historyJson)
            } catch (e: Exception) {
                emptyList()
            }
            
            // 只保留固定的历史
            val pinnedHistory = currentHistory.filter { it.isPinned }
            
            preferences[KEY_SEARCH_HISTORY] = json.encodeToString(pinnedHistory)
        }
    }
    
    /**
     * 同步获取历史列表（不依赖Flow）
     */
    suspend fun getHistoryList(): List<SearchHistoryItem> {
        val preferences = dataStore.data.first()
        val historyJson = preferences[KEY_SEARCH_HISTORY] ?: "[]"
        return try {
            val items = json.decodeFromString<List<SearchHistoryItem>>(historyJson)
            // 排序：
            // 1. 固定的在前，按固定时间倒序
            // 2. 未固定的在后，按搜索时间倒序
            items.sortedWith(
                compareByDescending<SearchHistoryItem> { it.isPinned }
                    .thenByDescending { if (it.isPinned) it.pinnedTimestamp ?: 0L else it.searchTimestamp }
            )
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 删除单个历史记录
     */
    suspend fun removeHistory(keyword: String) {
        dataStore.edit { preferences ->
            val currentHistoryJson = preferences[KEY_SEARCH_HISTORY] ?: "[]"
            val currentHistory = try {
                json.decodeFromString<List<SearchHistoryItem>>(currentHistoryJson).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            
            currentHistory.removeAll { it.keyword == keyword }
            preferences[KEY_SEARCH_HISTORY] = json.encodeToString(currentHistory)
        }
    }
    
    /**
     * 固定/取消固定搜索历史
     */
    suspend fun togglePin(keyword: String) {
        dataStore.edit { preferences ->
            val currentHistoryJson = preferences[KEY_SEARCH_HISTORY] ?: "[]"
            val currentHistory = try {
                json.decodeFromString<List<SearchHistoryItem>>(currentHistoryJson).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            
            val index = currentHistory.indexOfFirst { it.keyword == keyword }
            if (index >= 0) {
                val item = currentHistory[index]
                val newIsPinned = !item.isPinned
                
                // 检查固定数量限制
                if (newIsPinned) {
                    val pinnedCount = currentHistory.count { it.isPinned }
                    if (pinnedCount >= MAX_PINNED_SIZE) {
                        // 如果已达到固定上限，不执行操作
                        return@edit
                    }
                }
                
                // 更新项：
                // - 固定时：记录固定时间
                // - 解除固定时：清除固定时间
                currentHistory[index] = item.copy(
                    isPinned = newIsPinned,
                    pinnedTimestamp = if (newIsPinned) System.currentTimeMillis() else null
                )
                preferences[KEY_SEARCH_HISTORY] = json.encodeToString(currentHistory)
            }
        }
    }
}

/**
 * 创建搜索历史数据存储的expect函数
 */
expect fun createSearchHistoryDataStore(): DataStore<Preferences>
