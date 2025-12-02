package com.projectu.ui.navigation

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.projectu.ui.screens.user.UserProfileTab
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 作品详情页导航上下文
 * 
 * 用于在导航到作品详情页时传递不可序列化的参数（列表源、回调等）
 * 通过 contextKey 关联 Screen 和上下文数据
 * 
 * @param listSource 列表源（实现 ArtworkListSource 接口的 ViewModel）
 * @param onReturnWithIndex 返回时回调，传递最后浏览的索引
 */
data class ArtworkDetailContext(
    val listSource: ArtworkListSource? = null,
    val onReturnWithIndex: ((Int) -> Unit)? = null
)

/**
 * 小说详情页导航上下文
 * 
 * 用于在导航到小说详情页时传递不可序列化的参数（列表源、回调等）
 * 
 * @param listSource 列表源（实现 NovelListSource 接口的 ViewModel）
 * @param onReturnWithIndex 返回时回调，传递最后浏览的索引
 */
data class NovelDetailContext(
    val listSource: NovelListSource? = null,
    val onReturnWithIndex: ((Int) -> Unit)? = null
)

/**
 * 导航上下文管理器
 * 
 * 管理作品详情页和小说详情页的导航上下文，解决 Screen 参数不可序列化的问题
 * 
 * 使用方式：
 * 1. 推送页面前，调用 createContext()/createNovelContext() 创建上下文并获取 key
 * 2. 将 key 传递给 ArtworkDetailScreen/NovelDetailScreen
 * 3. 详情页通过 key 获取上下文，订阅 listSource 的 Flow
 * 4. 页面销毁时，调用 removeContext()/removeNovelContext() 清理
 * 
 * Activity 状态恢复时：
 * - contextKey 对应的上下文可能已丢失（进程被杀死）
 * - 详情页会降级使用序列化的 IDs 快照
 * 
 * 对于 UserScreen 的滚动位置：
 * - 使用 getOrCreateUserScrollIndices(userId) 获取或创建滚动位置存储
 * - 不需要手动清理，因为用户滚动位置可以长期保留
 */
object NavigationContextManager {
    
    private val artworkContexts = ConcurrentHashMap<String, ArtworkDetailContext>()
    private val novelContexts = ConcurrentHashMap<String, NovelDetailContext>()
    
    // UserScreen 的滚动位置存储，key 为 userId
    private val userScreenScrollIndices = ConcurrentHashMap<Long, SnapshotStateMap<UserProfileTab, Int>>()
    
    // ===================== 作品详情页上下文管理 =====================
    
    /**
     * 创建作品导航上下文
     * 
     * @param listSource 列表源（实现 ArtworkListSource 接口的 ViewModel）
     * @param onReturnWithIndex 返回时回调
     * @return 上下文的唯一标识 key
     */
    fun createContext(
        listSource: ArtworkListSource? = null,
        onReturnWithIndex: ((Int) -> Unit)? = null
    ): String {
        val key = UUID.randomUUID().toString()
        artworkContexts[key] = ArtworkDetailContext(
            listSource = listSource,
            onReturnWithIndex = onReturnWithIndex
        )
        return key
    }
    
    /**
     * 获取作品导航上下文
     */
    fun getContext(key: String): ArtworkDetailContext? {
        return artworkContexts[key]
    }
    
    /**
     * 移除作品导航上下文
     */
    fun removeContext(key: String) {
        artworkContexts.remove(key)
    }
    
    // ===================== 小说详情页上下文管理 =====================
    
    /**
     * 创建小说导航上下文
     * 
     * @param listSource 列表源（实现 NovelListSource 接口的 ViewModel）
     * @param onReturnWithIndex 返回时回调
     * @return 上下文的唯一标识 key
     */
    fun createNovelContext(
        listSource: NovelListSource? = null,
        onReturnWithIndex: ((Int) -> Unit)? = null
    ): String {
        val key = UUID.randomUUID().toString()
        novelContexts[key] = NovelDetailContext(
            listSource = listSource,
            onReturnWithIndex = onReturnWithIndex
        )
        return key
    }
    
    /**
     * 获取小说导航上下文
     */
    fun getNovelContext(key: String): NovelDetailContext? {
        return novelContexts[key]
    }
    
    /**
     * 移除小说导航上下文
     */
    fun removeNovelContext(key: String) {
        novelContexts.remove(key)
    }
    
    /**
     * 清理所有上下文（用于调试或异常情况）
     */
    fun clearAll() {
        artworkContexts.clear()
        novelContexts.clear()
        userScreenScrollIndices.clear()
    }
    
    // ===================== UserScreen 滚动位置管理 =====================
    
    /**
     * 获取或创建用户页面的滚动位置存储
     * 
     * @param userId 用户ID
     * @return 该用户页面各Tab的滚动位置Map
     */
    fun getOrCreateUserScrollIndices(userId: Long): SnapshotStateMap<UserProfileTab, Int> {
        return userScreenScrollIndices.getOrPut(userId) { mutableStateMapOf() }
    }
    
    /**
     * 清除指定用户的滚动位置存储
     * 
     * @param userId 用户ID
     */
    fun clearUserScrollIndices(userId: Long) {
        userScreenScrollIndices.remove(userId)
    }
}
