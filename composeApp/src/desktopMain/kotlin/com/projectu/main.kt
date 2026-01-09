package com.projectu

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.projectu.di.initKoin
import dev.datlag.kcef.KCEF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.context.loadKoinModules
import projectu.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import java.io.File
import kotlin.math.max

fun main() = application {
    // 在应用启动时立即初始化 Koin（不依赖 KCEF）
    initKoin()
    loadKoinModules(com.projectu.shared.di.desktopPlatformModule())
    
    var restartRequired by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(0F) }
    var initialized by remember { mutableStateOf(false) }
    
    // 初始化 KCEF (WebView)
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            KCEF.init(builder = {
                installDir(File("kcef-bundle"))
                progress {
                    onDownloading {
                        downloading = max(it, 0F)
                    }
                    onInitialized {
                        initialized = true
                    }
                }
                settings {
                    cachePath = File("cache").absolutePath
                    // 启用 Cookie 持久化，这样才能提取 Cookie
                    persistSessionCookies = true
                }
            }, onError = {
                it?.printStackTrace()
            }, onRestartRequired = {
                restartRequired = true
            })
        }
    }
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "ProjectU - Pixiv Client",
        state = rememberWindowState()
    ) {
        when {
            restartRequired -> {
                // 需要重启
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(Res.string.desktop_webview_restart_required))
                }
            }
            !initialized -> {
                // 正在下载 KCEF
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(stringResource(Res.string.desktop_webview_initializing))
                        if (downloading > 0) {
                            Text(
                                stringResource(Res.string.desktop_download_progress, downloading.toInt()),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            else -> {
                // KCEF 已初始化，显示应用
                App()
            }
        }
        
        // 清理 KCEF
        DisposableEffect(Unit) {
            onDispose {
                KCEF.disposeBlocking()
            }
        }
    }
}

