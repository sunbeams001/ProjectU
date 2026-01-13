package com.projectu.shared.util

import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * 日期时间格式化工具
 * 
 * 用于将ISO 8601格式的UTC时间转换为本地时区的可读格式
 */
object DateTimeFormatter {
    
    // 日本时区 (UTC+9)
    private val japanTimeZone = TimeZone.of("Asia/Tokyo")
    
    /**
     * 格式化ISO 8601时间字符串为本地时区的可读格式
     * 
     * @param isoString ISO 8601格式的时间字符串，例如："2025-11-26T15:30:00+00:00"
     * @return 本地时区的格式化字符串，例如："2025-11-26 23:30"（如果本地是UTC+8）
     *         如果解析失败，返回原始字符串
     */
    fun formatToLocalDateTime(isoString: String): String {
        return try {
            // 解析ISO 8601字符串为Instant
            val instant = Instant.parse(isoString)
            
            // 转换为本地时区
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            
            // 格式化为可读字符串
            "${localDateTime.year}-${localDateTime.month.number.toString().padStart(2, '0')}-${localDateTime.day.toString().padStart(2, '0')} ${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
        } catch (e: Exception) {
            // 解析失败时返回原始字符串
            isoString
        }
    }
    
    /**
     * 格式化日本时间（UTC+9）为本地时区的可读格式
     * 
     * Pixiv API 返回的评论时间是日本时间，格式如 "2025-12-05 23:39"（不带时区信息）
     * 此方法会将其视为日本时间并转换为本地时区
     * 
     * @param dateTimeString 日本时间字符串，例如："2025-12-05 23:39"
     * @return 本地时区的格式化字符串，例如："2025-12-05 22:39"（如果本地是UTC+8）
     *         如果解析失败，返回原始字符串
     */
    fun formatJapanTimeToLocal(dateTimeString: String): String {
        return try {
            // 尝试直接解析带时区的 ISO 格式
            val instant = try {
                Instant.parse(dateTimeString)
            } catch (e: Exception) {
                // 解析 "2025-12-05 23:39" 格式
                val normalized = dateTimeString.replace(" ", "T").let {
                    // 如果没有秒数，添加 :00
                    if (it.count { c -> c == ':' } == 1) "$it:00" else it
                }
                val localDateTime = LocalDateTime.parse(normalized)
                localDateTime.toInstant(japanTimeZone)
            }
            
            // 转换为本地时区
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            
            // 格式化为可读字符串
            "${localDateTime.year}-${localDateTime.month.number.toString().padStart(2, '0')}-${localDateTime.day.toString().padStart(2, '0')} ${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
        } catch (e: Exception) {
            // 解析失败时返回原始字符串
            dateTimeString
        }
    }
    
    /**
     * 格式化ISO 8601时间字符串为详细的本地时间（包含秒）
     * 
     * @param isoString ISO 8601格式的时间字符串
     * @return 详细格式，例如："2025-11-26 23:30:55"
     */
    fun formatToDetailedLocalDateTime(isoString: String): String {
        return try {
            val instant = Instant.parse(isoString)
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            
            "${localDateTime.year}-${localDateTime.month.number.toString().padStart(2, '0')}-${localDateTime.day.toString().padStart(2, '0')} ${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}:${localDateTime.second.toString().padStart(2, '0')}"
        } catch (e: Exception) {
            isoString
        }
    }
}
