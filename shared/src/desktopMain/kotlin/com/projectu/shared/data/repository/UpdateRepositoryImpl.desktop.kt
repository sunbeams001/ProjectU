package com.projectu.shared.data.repository

/**
 * Desktop 平台实现
 */
actual fun getPlatform(): Platform {
    val osName = System.getProperty("os.name").lowercase()
    return when {
        osName.contains("win") -> Platform.WINDOWS
        osName.contains("mac") -> Platform.MACOS
        osName.contains("nix") || osName.contains("nux") -> Platform.LINUX
        else -> Platform.UNKNOWN
    }
}
