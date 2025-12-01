package com.projectu.shared.util

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 日期时间格式化工具
 * 
 * 用于将ISO 8601格式的UTC时间转换为本地时区的可读格式
 */
@OptIn(kotlin.time.ExperimentalTime::class)
object DateTimeFormatter {
    
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
            "${localDateTime.year}-${localDateTime.monthNumber.toString().padStart(2, '0')}-${localDateTime.dayOfMonth.toString().padStart(2, '0')} ${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
        } catch (e: Exception) {
            // 解析失败时返回原始字符串
            isoString
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
            
            "${localDateTime.year}-${localDateTime.monthNumber.toString().padStart(2, '0')}-${localDateTime.dayOfMonth.toString().padStart(2, '0')} ${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}:${localDateTime.second.toString().padStart(2, '0')}"
        } catch (e: Exception) {
            isoString
        }
    }
    
    /**
     * 从秒转换为阅读时间显示
     * 
     * @param seconds 秒数
     * @return 格式化的阅读时间，例如："5分钟"、"1小时30分钟"
     */
    fun formatReadingTimeFromSeconds(seconds: Int): String {
        val totalMinutes = (seconds + 59) / 60 // 向上取整到分钟
        return when {
            totalMinutes >= 60 -> {
                val hours = totalMinutes / 60
                val mins = totalMinutes % 60
                if (mins > 0) "${hours}小时${mins}分钟" else "${hours}小时"
            }
            totalMinutes > 0 -> "${totalMinutes}分钟"
            else -> "不到1分钟"
        }
    }
}
