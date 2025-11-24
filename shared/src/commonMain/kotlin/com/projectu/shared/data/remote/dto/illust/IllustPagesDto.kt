package com.projectu.shared.data.remote.dto.illust

import kotlinx.serialization.Serializable

/**
 * 插画多页详情响应体
 * 
 * 用于获取多页作品（漫画）的所有页面图片 URL
 * API 端点：/ajax/illust/{pid}/pages
 * 
 * 示例响应：
 * ```json
 * {
 *   "urls": {
 *     "thumb_mini": "https://i.pximg.net/c/128x128/img-master/.../p0_square1200.jpg",
 *     "small": "https://i.pximg.net/c/540x540_70/img-master/.../p0_master1200.jpg",
 *     "regular": "https://i.pximg.net/img-master/.../p0_master1200.jpg",
 *     "original": "https://i.pximg.net/img-original/.../p0.jpg"
 *   },
 *   "width": 1536,
 *   "height": 1024
 * }
 * ```
 */
@Serializable
data class PageInfo(
    val urls: PageUrls,
    val width: Int,
    val height: Int
)

/**
 * 单页图片的各种尺寸 URL
 */
@Serializable
data class PageUrls(
    val thumb_mini: String,      // 128x128 square1200 缩略图
    val small: String,            // 540x540 img-master/master1200
    val regular: String,          // img-master/master1200
    val original: String          // img-original 原图
)
