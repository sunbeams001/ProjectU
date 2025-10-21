package com.projectu.shared.util

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Ktor HTTP客户端工厂
 */
object NetworkClient {
    
    fun create(engine: HttpClientEngine): HttpClient {
        return HttpClient(engine) {
            // JSON序列化配置
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = true
                })
            }
            
            // 日志配置
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
                filter { request ->
                    request.url.host.contains("pixiv")
                }
            }
            
            // 默认请求配置
            install(DefaultRequest) {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(HttpHeaders.UserAgent, "ProjectU/1.0.0")
            }
            
            // 超时配置
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 15_000
            }
            
            // 认证配置（Bearer Token）
            install(Auth) {
                bearer {
                    loadTokens {
                        // TODO: 从DataStore加载token
                        null
                    }
                    refreshTokens {
                        // TODO: 刷新token逻辑
                        null
                    }
                }
            }
        }
    }
}

