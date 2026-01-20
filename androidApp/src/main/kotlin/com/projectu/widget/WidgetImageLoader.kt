package com.projectu.widget

import android.content.Context
import android.graphics.Bitmap
import coil3.ImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Scale
import coil3.toBitmap
import com.projectu.shared.domain.model.Artwork
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Widget 图片加载器（使用 Coil 同步执行方案）
 */
object WidgetImageLoader {
    
    private const val TAG = "WidgetImageLoader"
    
    /**
     * 为Widget创建ImageLoader（配置Pixiv请求头）
     */
    private fun createImageLoader(context: Context): ImageLoader {
        // 创建HttpClient，添加Pixiv所需的Referer
        val httpClient = HttpClient(OkHttp) {
            engine {
                config {
                    followRedirects(true)
                }
            }
            // 添加拦截器，为所有Pixiv图片请求添加Referer
            install(DefaultRequest) {
                headers.append("Referer", "https://www.pixiv.net/")
                headers.append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36")
            }
        }
        
        return ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory(httpClient))
            }
            .build()
    }
    /**
     * 加载作品图片（使用 Coil 同步执行）
     * 
     * 注意：必须在后台线程（如 Worker 的 Dispatchers.IO）中调用
     * 
     * @param context 应用上下文
     * @param artwork 作品信息
     * @param targetWidth 目标宽度（默认1200）
     * @param targetHeight 目标高度（默认800）
     * @return 加载的Bitmap，失败时返回null
     */
    suspend fun loadArtworkImage(
        context: Context,
        artwork: Artwork,
        targetWidth: Int = 1200,
        targetHeight: Int = 800
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // 获取ImageLoader实例
            val imageLoader = createImageLoader(context)
            
            // 获取图片URL（优先使用高质量图片）
            val imageUrl = artwork.imageUrls.pages.firstOrNull()?.urls?.let { urls ->
                urls.large ?: urls.medium ?: urls.squareMedium
            }
            
            if (imageUrl == null) {
                return@withContext null
            }
            
            // 使用 Coil 同步执行
            val result = imageLoader.execute(
                ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(targetWidth, targetHeight)
                    .scale(Scale.FIT)
                    .allowHardware(false) // Widget 需要软件 Bitmap
                    .build()
            )
            
            val bitmap = result.image?.toBitmap()
            
            bitmap
        } catch (e: Exception) {
            // 加载失败时的降级策略：尝试加载缩略图
            loadThumbnail(context, artwork)
        }
    }
    /**
     * 降级方案：加载缩略图
     */
    private suspend fun loadThumbnail(
        context: Context,
        artwork: Artwork
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val imageLoader = createImageLoader(context)
            val thumbnailUrl = artwork.imageUrls.pages.firstOrNull()?.urls?.squareMedium
            
            if (thumbnailUrl == null) {
                return@withContext null
            }
            
            val result = imageLoader.execute(
                ImageRequest.Builder(context)
                    .data(thumbnailUrl)
                    .allowHardware(false)
                    .build()
            )
            
            result.image?.toBitmap()
        } catch (e: Exception) {
            null
        }
    }
    /**
     * 预加载下一批作品的图片到缓存
     * 
     * @param context 应用上下文
     * @param artworks 作品列表
     * @param count 预加载数量（默认3张）
     */
    suspend fun preloadArtworks(
        context: Context,
        artworks: List<Artwork>,
        count: Int = 3
    ) = withContext(Dispatchers.IO) {
        val imageLoader = createImageLoader(context)
        
        artworks.take(count).forEach { artwork ->
            try {
                val imageUrl = artwork.imageUrls.pages.firstOrNull()?.urls?.let { urls ->
                    urls.medium ?: urls.squareMedium
                } ?: return@forEach
                
                imageLoader.execute(
                    ImageRequest.Builder(context)
                        .data(imageUrl)
                        .build()
                )
            } catch (e: Exception) {
                // 预加载失败不影响主流程
            }
        }
    }
}