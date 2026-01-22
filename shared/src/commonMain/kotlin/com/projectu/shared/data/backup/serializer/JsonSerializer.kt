package com.projectu.shared.data.backup.serializer

import kotlinx.serialization.json.Json

/**
 * JSON 序列化器
 */
object JsonSerializer {
    
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * 序列化对象为JSON字符串
     */
    inline fun <reified T> encodeToString(value: T): String {
        return json.encodeToString(value)
    }
    
    /**
     * 从JSON字符串反序列化对象
     */
    inline fun <reified T> decodeFromString(string: String): T {
        return json.decodeFromString(string)
    }
}
