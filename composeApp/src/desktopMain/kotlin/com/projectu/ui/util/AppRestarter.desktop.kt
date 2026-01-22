package com.projectu.ui.util

import kotlin.system.exitProcess

/**
 * Desktop 平台的应用重启实现
 */
actual object AppRestarter {
    /**
     * 重启应用
     * 
     * Desktop 平台的重启较为简单，直接退出进程
     * 用户需要手动重新打开应用
     */
    actual fun restartApp(delayMillis: Long) {
        // 在 Desktop 平台，可以简单地退出应用
        // 用户需要手动重新启动
        println("Application will exit in ${delayMillis}ms. Please restart manually.")
        
        Thread.sleep(delayMillis)
        exitProcess(0)
    }
}
