package com.projectu.shared.data.remote.api

import java.io.File

/**
 * Desktop平台的文件保存实现
 */
internal actual fun saveHtmlToFile(html: String, filename: String) {
    try {
        // 保存到用户主目录下的 Downloads 文件夹
        val userHome = System.getProperty("user.home")
        val downloadsDir = File(userHome, "Downloads")
        
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        
        val file = File(downloadsDir, filename)
        file.writeText(html, Charsets.UTF_8)
        
        println("📁 HTML文件已保存到: ${file.absolutePath}")
        println("📏 文件大小: ${html.length} 字符")
    } catch (e: Exception) {
        println("❌ 保存文件失败: ${e.message}")
        e.printStackTrace()
    }
}
