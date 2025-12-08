package com.projectu.shared.data.local

/**
 * Desktop平台的默认下载路径
 * ~/Pictures/ProjectU
 */
actual fun getDefaultDownloadPath(): String {
    val userHome = System.getProperty("user.home")
    return "$userHome/Pictures/ProjectU"
}
