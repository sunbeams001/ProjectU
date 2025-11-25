package com.projectu.ui.util

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*

/**
 * Desktop 平台的 ImageLoader 配置
 * 使用 Ktor 拦截器添加 Referer 头
 */
actual fun createImageLoader(context: PlatformContext): ImageLoader {
    // 创建带 Referer 拦截器的 Ktor HttpClient
    val httpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 30000
        }
        
        // 添加默认请求拦截器
        defaultRequest {
            header("Referer", "https://www.pixiv.net/")
        }
    }
    
    return ImageLoader.Builder(context)
        .components {
            add(KtorNetworkFetcherFactory(httpClient))
        }
        .applyPixivConfiguration()
        .build()
}
