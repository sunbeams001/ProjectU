package com.projectu.ui.util

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*

/**
 * UI层格式化工具函数
 * 
 * 提供支持多语言的格式化函数
 */

/**
 * 格式化阅读时间（从秒转换）
 * 
 * @param seconds 秒数
 * @return 本地化的阅读时间字符串，例如："5分钟"、"1小时30分钟"
 */
@Composable
fun formatReadingTime(seconds: Int): String {
    val totalMinutes = (seconds + 59) / 60 // 向上取整到分钟
    return when {
        totalMinutes >= 60 -> {
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            if (mins > 0) {
                stringResource(Res.string.time_hours_minutes, hours, mins)
            } else {
                stringResource(Res.string.time_hours, hours)
            }
        }
        totalMinutes > 0 -> stringResource(Res.string.time_minutes, totalMinutes)
        else -> stringResource(Res.string.time_less_than_minute)
    }
}

/**
 * 格式化数字（带单位缩写）
 * 
 * @param number 数字
 * @return 格式化后的字符串，例如："1.2w"、"3.4k"
 */
fun formatNumber(number: Int): String {
    return when {
        number >= 10000 -> String.format("%.1fw", number / 10000.0)
        number >= 1000 -> String.format("%.1fk", number / 1000.0)
        else -> number.toString()
    }
}
