package com.projectu.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import cafe.adriel.voyager.navigator.Navigator
import com.projectu.shared.data.local.SearchHistoryStore
import com.projectu.shared.domain.model.Tag
import com.projectu.ui.screens.search.SearchResultScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Tag点击处理器
 * 统一处理所有Tag点击事件，自动记录搜索历史并跳转到搜索结果页
 * 
 * 使用方式：
 * ```kotlin
 * val tagClickHandler = rememberTagClickHandler(navigator)
 * TagChip(tag = tag, onClick = { tagClickHandler(tag) })
 * ```
 */
class TagClickHandler(
    private val navigator: Navigator,
    private val searchHistoryStore: SearchHistoryStore,
    private val coroutineScope: CoroutineScope
) {
    /**
     * 处理Tag对象点击（推荐使用）
     * @param tag Tag对象，包含原文和翻译
     */
    fun handleTagClick(tag: Tag) {
        handleTagClick(tag.name) // 使用原文进行搜索
    }
    
    /**
     * 处理字符串Tag点击（用于NovelSeries等场景）
     * @param tagName Tag名称（原文）
     */
    fun handleTagClick(tagName: String) {
        val keyword = tagName.trim()
        if (keyword.isBlank()) return
        
        // 异步添加到搜索历史
        coroutineScope.launch {
            try {
                searchHistoryStore.addHistory(keyword)
            } catch (e: Exception) {
                // 忽略错误，不影响导航
            }
        }
        
        // 立即跳转到搜索结果页
        navigator.push(SearchResultScreen(keyword))
    }
    
    /**
     * 扩展函数：简化Tag点击处理
     */
    operator fun invoke(tag: Tag) = handleTagClick(tag)
    operator fun invoke(tagName: String) = handleTagClick(tagName)
}

/**
 * Composable辅助函数：创建Tag点击处理器
 * 
 * 使用示例：
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val navigator = LocalNavigator.currentOrThrow
 *     val tagClickHandler = rememberTagClickHandler(navigator)
 *     
 *     TagChip(
 *         tag = tag,
 *         onClick = { tagClickHandler(tag) }
 *     )
 * }
 * ```
 */
@Composable
fun rememberTagClickHandler(
    navigator: Navigator,
    searchHistoryStore: SearchHistoryStore = koinInject(),
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): TagClickHandler {
    return remember(navigator, searchHistoryStore, coroutineScope) {
        TagClickHandler(navigator, searchHistoryStore, coroutineScope)
    }
}
