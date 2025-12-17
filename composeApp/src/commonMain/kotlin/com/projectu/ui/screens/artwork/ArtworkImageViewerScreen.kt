package com.projectu.ui.screens.artwork

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.shared.domain.model.PageImageUrls
import com.projectu.ui.util.PlatformBackHandler

/**
 * 作品大图浏览页面
 * 
 * 全屏展示单张或多张作品的高质量图片（Original质量）
 * 
 * 功能：
 * - 全屏展示图片
 * - 双击放大缩小
 * - 多图左右滑动切换
 * - 点击显示/隐藏底部信息蒙版
 * - 底部蒙版显示：分辨率、页号、下载按钮
 * 
 * @param artworkId 作品ID
 * @param artworkTitle 作品标题
 * @param pages 图片页面列表
 * @param initialPage 初始显示的页码（从0开始）
 */
data class ArtworkImageViewerScreen(
    val artworkId: String,
    val artworkTitle: String,
    val pages: List<PageImageUrls>,
    val initialPage: Int = 0
) : Screen {
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        
        // 系统返回键处理
        PlatformBackHandler(enabled = true) {
            navigator.pop()
        }
        
        ArtworkImageViewerContent(
            artworkId = artworkId,
            artworkTitle = artworkTitle,
            pages = pages,
            initialPage = initialPage,
            onBackClick = { navigator.pop() }
        )
    }
}
