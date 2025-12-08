package com.projectu

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.projectu.di.initKoin
import org.koin.core.context.loadKoinModules

fun main() = application {
    // 初始化Koin
    initKoin()
    
    // 加载 Desktop 平台的 shared 模块
    loadKoinModules(com.projectu.shared.di.desktopPlatformModule())
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "ProjectU - Pixiv Client",
        state = rememberWindowState()
    ) {
        App()
    }
}

