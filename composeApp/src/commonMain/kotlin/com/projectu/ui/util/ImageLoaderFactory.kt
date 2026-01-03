package com.projectu.ui.util

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.key.Keyer
import coil3.request.Options
import coil3.request.crossfade

/**
 * 检测 Pixiv 图片 URL 是否为方形裁剪
 * 
 * 用于判断图片是否经过方形裁剪处理（如 250x250、360x360、540x540）
 * 主要用于：
 * 1. 图片缓存 Key 生成：区分方形裁剪图和原始比例图
 * 2. UI 显示逻辑：确定容器的宽高比
 * 
 * @param url Pixiv 图片 URL
 * @return true 表示为方形裁剪，false 表示原始比例
 */
fun isPixivImageSquareCropped(url: String): Boolean {
    return url.contains("/c/250x250") || 
           url.contains("/c/360x360") || 
           url.contains("/c/540x540") ||
           url.contains("custom-thumb")
}

/**
 * 创建配置好的 ImageLoader
 * 自动为所有图片请求添加 Pixiv 所需的 Referer 请求头
 * 
 * @param context 平台上下文
 * @param maxCacheSizeBytes 最大磁盘缓存大小（字节）
 */
expect fun createImageLoader(context: PlatformContext, maxCacheSizeBytes: Long): ImageLoader

/**
 * 创建使用默认缓存大小的 ImageLoader
 */
expect fun createImageLoader(context: PlatformContext): ImageLoader

/**
 * 获取 ImageLoader 的磁盘缓存（用于缓存管理）
 */
expect fun getImageLoaderDiskCache(imageLoader: ImageLoader): Any?

/**
 * 通用 ImageLoader 配置
 */
fun ImageLoader.Builder.applyPixivConfiguration(): ImageLoader.Builder {
    return this
        .crossfade(true)
        // .logger(DebugLogger()) // 已关闭图片加载日志
}

/**
 * Pixiv 图片智能缓存 Key 生成器
 * 
 * 策略：
 * 1. 从 Pixiv 图片 URL 中提取作品 ID 和页码作为唯一 Key
 * 2. 区分方形裁剪图和原始比例图，使用不同的缓存命名空间
 * 3. 同一裁剪类型的不同分辨率图片共享缓存（高分辨率覆盖低分辨率）
 * 
 * 缓存命名空间：
 * - 方形裁剪图（/c/250x250 等）: pixiv_artwork_cropped_{id}_p{index}
 * - 原始比例图（/img-master 等）: pixiv_artwork_original_{id}_p{index}
 * 
 * 这样可以避免方形裁剪图和原始比例图之间的缓存混淆，
 * 同时保留同一类型图片的高分辨率替代优化。
 * 
 * URL 格式示例：
 * - 方形裁剪: https://i.pximg.net/c/250x250_80_a2/img-master/.../12345678_p0_square1200.jpg
 * - 原始比例: https://i.pximg.net/img-master/.../12345678_p0_master1200.jpg
 * - 原图: https://i.pximg.net/img-original/.../12345678_p0.png
 * - 用户头像: https://i.pximg.net/user-profile/.../12345678_xxxxx.jpg
 */
class PixivImageKeyer : Keyer<String> {
    
    // 匹配作品图片: 12345678_p0 格式
    private val artworkPattern = Regex("""(\d+)_p(\d+)""")
    
    // 匹配用户头像: user-profile 路径中的用户 ID
    private val userProfilePattern = Regex("""/user-profile/.*?/(\d+)""")
    
    override fun key(data: String, options: Options): String? {
        // 只处理 Pixiv 图片 URL
        if (!data.contains("pximg.net") && !data.contains("pixiv.net")) {
            return null // 使用默认 key（完整 URL）
        }
        
        // 尝试匹配作品图片
        artworkPattern.find(data)?.let { match ->
            val artworkId = match.groupValues[1]
            val pageIndex = match.groupValues[2]
            
            // 检测是否为方形裁剪图
            val prefix = if (isPixivImageSquareCropped(data)) "cropped" else "original"
            
            return "pixiv_artwork_${prefix}_${artworkId}_p${pageIndex}"
        }
        
        // 尝试匹配用户头像
        userProfilePattern.find(data)?.let { match ->
            val userId = match.groupValues[1]
            return "pixiv_user_profile_${userId}"
        }
        
        // 其他 Pixiv URL 使用原始 URL 作为 key
        return null
    }
}
