package com.projectu.shared.data.remote.api

import android.os.Environment
import java.io.File

/**
 * Android平台的文件保存实现
 */
internal actual fun saveHtmlToFile(html: String, filename: String) {
    try {
        // 保存到应用的外部文件目录（需要权限）或内部文件目录
        val file = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            // 外部存储可用，保存到 Downloads 目录
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename)
        } else {
            // 使用内部存储（但这需要Context，暂时使用临时目录）
            File("/data/local/tmp", filename)
        }
        
        file.writeText(html, Charsets.UTF_8)
        println("📁 HTML文件已保存到: ${file.absolutePath}")
        println("📏 文件大小: ${html.length} 字符")
    } catch (e: Exception) {
        println("❌ 保存文件失败: ${e.message}")
        e.printStackTrace()
    }
}
