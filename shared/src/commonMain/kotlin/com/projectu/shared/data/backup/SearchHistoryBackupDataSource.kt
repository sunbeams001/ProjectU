package com.projectu.shared.data.backup

import com.projectu.shared.data.local.SearchHistoryItem
import com.projectu.shared.data.local.SearchHistoryStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 搜索历史备份数据源
 * 负责导出和导入搜索历史（包含置顶状态）
 */
class SearchHistoryBackupDataSource(
    private val searchHistoryStore: SearchHistoryStore
) {
    
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    /**
     * 导出搜索历史数据
     */
    suspend fun exportData(): String {
        val historyItems = searchHistoryStore.getHistoryList()
        val backupData = SearchHistoryBackupData(
            items = historyItems
        )
        return json.encodeToString(SearchHistoryBackupData.serializer(), backupData)
    }
    
    /**
     * 导入搜索历史数据
     * 导入策略：
     * 1. 保留所有固定的历史
     * 2. 添加新的历史（如果不存在）
     * 3. 更新已存在的历史（保持最新的搜索时间和固定状态）
     */
    suspend fun importData(jsonData: String) {
        val backupData = json.decodeFromString(SearchHistoryBackupData.serializer(), jsonData)
        
        // 获取当前历史
        val currentHistory = searchHistoryStore.getHistoryList().associateBy { it.keyword }
        
        // 合并历史：对每个备份项进行处理
        backupData.items.forEach { backupItem ->
            val currentItem = currentHistory[backupItem.keyword]
            
            when {
                // 如果当前不存在，直接添加
                currentItem == null -> {
                    // 通过 addHistory 添加普通历史，然后根据需要固定
                    searchHistoryStore.addHistory(backupItem.keyword)
                    if (backupItem.isPinned) {
                        searchHistoryStore.togglePin(backupItem.keyword)
                    }
                }
                // 如果备份项是固定的，当前项未固定，则固定当前项
                backupItem.isPinned && !currentItem.isPinned -> {
                    searchHistoryStore.togglePin(backupItem.keyword)
                }
                // 如果备份项未固定，当前项是固定的，保持当前固定状态
                !backupItem.isPinned && currentItem.isPinned -> {
                    // 不做操作，保持固定
                }
                // 其他情况，更新搜索时间
                else -> {
                    // 如果搜索时间更新，通过 addHistory 更新
                    if (backupItem.searchTimestamp > currentItem.searchTimestamp) {
                        searchHistoryStore.addHistory(backupItem.keyword)
                    }
                }
            }
        }
    }
}

/**
 * 搜索历史备份数据容器
 */
@Serializable
data class SearchHistoryBackupData(
    val items: List<SearchHistoryItem>
)
