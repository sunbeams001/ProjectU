package com.projectu.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 页面映射信息接口
 * 用于"一层 Pager + 页码映射"方案
 */
interface PageMapping {
    /** 第一层导航索引 */
    val primaryIndex: Int
    
    /** 第二层导航索引 */
    val secondaryIndex: Int
    
    /** 是否显示第二层导航 */
    val showSecondaryNav: Boolean
}

/**
 * 页码映射管理器
 * 管理多层导航与单一 Pager 页码之间的映射关系
 * 
 * @param T 页面映射信息类型，必须实现 PageMapping 接口
 */
interface PageIndexMapper<T : PageMapping> {
    /** 总页数 */
    val totalPages: Int
    
    /**
     * 反向映射：页码 → 页面映射信息
     * @param pageIndex 页码（从0开始）
     * @return 页面映射信息
     */
    fun parsePageIndex(pageIndex: Int): T
    
    /**
     * 正向映射：(第一层索引, 第二层索引) → 页码
     * @param primaryIndex 第一层导航索引
     * @param secondaryIndex 第二层导航索引
     * @return 页码
     */
    fun calculatePageIndex(primaryIndex: Int, secondaryIndex: Int): Int
}

/**
 * 双层导航的页码映射状态
 * 
 * @param pagerState Pager 状态
 * @param mapper 页码映射管理器
 */
@OptIn(ExperimentalFoundationApi::class)
class PagedNavigationState<T : PageMapping>(
    val pagerState: PagerState,
    val mapper: PageIndexMapper<T>
) {
    /**
     * 当前页面的映射信息
     * 使用 derivedStateOf 自动根据 pagerState.currentPage 计算
     */
    val currentMapping: T
        @Composable
        get() = remember {
            derivedStateOf { mapper.parsePageIndex(pagerState.currentPage) }
        }.value
    
    /**
     * 跳转到指定页码
     */
    suspend fun animateScrollToPage(pageIndex: Int) {
        pagerState.animateScrollToPage(pageIndex)
    }
    
    /**
     * 处理第一层导航点击
     * @param primaryIndex 第一层导航索引
     * @param currentSecondaryIndex 当前第二层导航索引（用于保持第二层的选择）
     * @param scope 协程作用域
     * @param onSamePage 点击当前页时的回调（通常用于刷新或滚动到顶部）
     */
    fun handlePrimaryClick(
        primaryIndex: Int,
        currentSecondaryIndex: Int,
        scope: CoroutineScope,
        onSamePage: () -> Unit
    ) {
        val targetPage = mapper.calculatePageIndex(primaryIndex, currentSecondaryIndex)
        if (targetPage == pagerState.currentPage) {
            onSamePage()
        } else {
            scope.launch {
                pagerState.animateScrollToPage(targetPage)
            }
        }
    }
    
    /**
     * 处理第二层导航点击
     * @param secondaryIndex 第二层导航索引
     * @param currentPrimaryIndex 当前第一层导航索引
     * @param scope 协程作用域
     * @param onSamePage 点击当前页时的回调
     */
    fun handleSecondaryClick(
        secondaryIndex: Int,
        currentPrimaryIndex: Int,
        scope: CoroutineScope,
        onSamePage: () -> Unit
    ) {
        val targetPage = mapper.calculatePageIndex(currentPrimaryIndex, secondaryIndex)
        if (targetPage == pagerState.currentPage) {
            onSamePage()
        } else {
            scope.launch {
                pagerState.animateScrollToPage(targetPage)
            }
        }
    }
}

/**
 * 创建页码导航状态
 * 
 * @param pagerState Pager 状态
 * @param mapper 页码映射管理器
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T : PageMapping> rememberPagedNavigationState(
    pagerState: PagerState,
    mapper: PageIndexMapper<T>
): PagedNavigationState<T> {
    return remember(pagerState, mapper) {
        PagedNavigationState(pagerState, mapper)
    }
}

/**
 * 自定义双层导航映射器
 * 适用于复杂场景：不同主分类有不同数量的第二层导航
 * 
 * 例如：排行榜页面，综合有7个模式，插画有6个模式等
 * 
 * @param T 页面映射信息类型
 * @param secondaryCountPerPrimary 每个主分类的第二层导航数量列表
 * @param createMapping 创建页面映射信息的工厂函数
 */
class CustomTwoLayerMapper<T : PageMapping>(
    private val secondaryCountPerPrimary: List<Int>,
    private val createMapping: (primaryIndex: Int, secondaryIndex: Int, showSecondary: Boolean) -> T
) : PageIndexMapper<T> {
    
    // 每个主分类的起始页码
    private val primaryStartPages: List<Int> = buildList {
        var sum = 0
        add(sum)
        secondaryCountPerPrimary.forEach { count ->
            sum += count
            add(sum)
        }
    }
    
    override val totalPages: Int = secondaryCountPerPrimary.sum()
    
    override fun parsePageIndex(pageIndex: Int): T {
        var primaryIndex = 0
        for (i in 0 until primaryStartPages.size - 1) {
            if (pageIndex >= primaryStartPages[i] && pageIndex < primaryStartPages[i + 1]) {
                primaryIndex = i
                break
            }
        }
        
        val secondaryIndex = pageIndex - primaryStartPages[primaryIndex]
        val showSecondary = secondaryCountPerPrimary[primaryIndex] > 1
        
        return createMapping(primaryIndex, secondaryIndex, showSecondary)
    }
    
    override fun calculatePageIndex(primaryIndex: Int, secondaryIndex: Int): Int {
        return primaryStartPages[primaryIndex] + secondaryIndex
    }
}
