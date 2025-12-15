package com.projectu.shared.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

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
    }
    
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * 搜索历史流
     */
    val searchHistory: Flow<List<String>> = dataStore.data.map { preferences ->
        val historyJson = preferences[KEY_SEARCH_HISTORY] ?: "[]"
        try {
            json.decodeFromString<List<String>>(historyJson)
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
                json.decodeFromString<List<String>>(currentHistoryJson).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            
            // 移除重复项（如果存在）
            currentHistory.remove(trimmedKeyword)
            
            // 添加到最前面
            currentHistory.add(0, trimmedKeyword)
            
            // 限制最大数量
            val limitedHistory = currentHistory.take(MAX_HISTORY_SIZE)
            
            // 保存
            preferences[KEY_SEARCH_HISTORY] = json.encodeToString(limitedHistory)
        }
    }
    
    /**
     * 清空搜索历史
     */
    suspend fun clearHistory() {
        dataStore.edit { preferences ->
            preferences[KEY_SEARCH_HISTORY] = "[]"
        }
    }
    
    /**
     * 删除单个历史记录
     */
    suspend fun removeHistory(keyword: String) {
        dataStore.edit { preferences ->
            val currentHistoryJson = preferences[KEY_SEARCH_HISTORY] ?: "[]"
            val currentHistory = try {
                json.decodeFromString<List<String>>(currentHistoryJson).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            
            currentHistory.remove(keyword)
            preferences[KEY_SEARCH_HISTORY] = json.encodeToString(currentHistory)
        }
    }
}

/**
 * 创建搜索历史数据存储的expect函数
 */
expect fun createSearchHistoryDataStore(): DataStore<Preferences>
