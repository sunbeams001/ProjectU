package com.projectu.shared.data.local

import android.os.Environment
import com.projectu.shared.data.local.database.ContextHolder
import java.io.File

/**
 * Android平台的默认下载路径
 * /storage/emulated/0/Pictures/ProjectU
 */
actual fun getDefaultDownloadPath(): String {
    return try {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        File(picturesDir, "ProjectU").absolutePath
    } catch (e: Exception) {
        // 降级方案：使用应用私有目录
        val context = ContextHolder.getContext()
        File(context.filesDir, "Downloads").absolutePath
    }
}
