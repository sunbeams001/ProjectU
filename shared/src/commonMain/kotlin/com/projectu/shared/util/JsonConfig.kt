package com.projectu.shared.util

import kotlinx.serialization.json.Json

/**
 * 全局统一的 Json 序列化配置
 * 
 * 所有需要 Json 解析的地方都应该使用此配置，以确保行为一致：
 * - ignoreUnknownKeys: 忽略未知字段，避免 API 新增字段导致解析失败
 * - isLenient: 宽松模式，允许非标准 JSON 格式
 * - prettyPrint: 格式化输出（便于调试）
 * - encodeDefaults: 不编码默认值（减少数据量）
 * 
 * 使用方式：
 * ```kotlin
 * import com.projectu.shared.util.AppJson
 * 
 * val result = AppJson.decodeFromString<MyType>(jsonString)
 * val jsonString = AppJson.encodeToString(myObject)
 * ```
 */
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = true
    encodeDefaults = false
}
