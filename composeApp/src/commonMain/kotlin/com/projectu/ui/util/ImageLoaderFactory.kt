package com.projectu.ui.util

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.crossfade

/**
 * 创建配置好的 ImageLoader
 * 自动为所有图片请求添加 Pixiv 所需的 Referer 请求头
 */
expect fun createImageLoader(context: PlatformContext): ImageLoader

/**
 * 通用 ImageLoader 配置
 */
fun ImageLoader.Builder.applyPixivConfiguration(): ImageLoader.Builder {
    return this
        .crossfade(true)
        // .logger(DebugLogger()) // 已关闭图片加载日志
}
