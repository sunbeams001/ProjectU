package com.projectu

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.projectu.di.initKoin

fun main() = application {
    // 初始化Koin
    initKoin()
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "ProjectU - Pixiv Client",
        state = rememberWindowState()
    ) {
        App()
    }
}

