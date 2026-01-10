package com.projectu.ui.screens.pixivision

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*

/**
 * Pixivision WebView 页面
 * 用于在应用内打开 Pixivision 特辑页面
 */
data class PixivisionWebViewScreen(
    private val url: String
) : Screen {
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val webViewState = rememberWebViewState(url)
        val webViewNavigator = rememberWebViewNavigator()
        val isDarkMode = isSystemInDarkTheme()
        
        // 监听页面加载完成，注入深色模式样式
        LaunchedEffect(webViewState.loadingState, isDarkMode) {
            if (webViewState.loadingState is LoadingState.Finished && isDarkMode) {
                // 注入 CSS 修改背景色和文字颜色
                val darkModeScript = """
                    (function() {
                        // 设置整体背景色
                        document.body.style.backgroundColor = '#1c1c1e';
                        document.body.style.color = '#e5e5e7';
                        
                        // 修改所有白色背景的元素
                        const allElements = document.querySelectorAll('*');
                        allElements.forEach(element => {
                            const bgColor = window.getComputedStyle(element).backgroundColor;
                            // 检测白色或浅色背景
                            if (bgColor === 'rgb(255, 255, 255)' || bgColor === 'white' || bgColor === '#ffffff') {
                                element.style.backgroundColor = '#1c1c1e';
                            }
                            
                            // 修改文字颜色
                            const textColor = window.getComputedStyle(element).color;
                            if (textColor === 'rgb(0, 0, 0)' || textColor === 'black' || textColor === '#000000') {
                                element.style.color = '#e5e5e7';
                            }
                        });
                        
                        // 特别处理容器背景
                        const containers = [
                            '.body-container',
                            '.main-column-container',
                            '.am__article-body-container',
                            '.fab__paragraph',
                            '._medium-wide-container',
                            '.sidebar-layout-container'
                        ];
                        
                        containers.forEach(selector => {
                            const elements = document.querySelectorAll(selector);
                            elements.forEach(el => {
                                el.style.backgroundColor = '#1c1c1e';
                                el.style.color = '#e5e5e7';
                            });
                        });
                        
                        // 添加全局样式
                        const style = document.createElement('style');
                        style.textContent = `
                            * {
                                background-color: #1c1c1e !important;
                                color: #e5e5e7 !important;
                            }
                            a {
                                color: #0a84ff !important;
                            }
                            img {
                                opacity: 0.9;
                            }
                        `;
                        document.head.appendChild(style);
                    })();
                """.trimIndent()
                
                webViewNavigator.evaluateJavaScript(darkModeScript)
            }
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(stringResource(Res.string.pixivision_detail_title)) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator?.pop() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.common_back)
                            )
                        }
                    },
                    actions = {
                        // 显示加载进度
                        val loadingState = webViewState.loadingState
                        if (loadingState is LoadingState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 12.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                WebView(
                    state = webViewState,
                    navigator = webViewNavigator,
                    modifier = Modifier.fillMaxSize()
                )
                
                // 加载错误提示
                val loadingState = webViewState.loadingState
                if (loadingState is LoadingState.Finished && webViewState.lastLoadedUrl == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.pixivision_webview_load_error),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { webViewNavigator.reload() }) {
                            Text(stringResource(Res.string.common_retry))
                        }
                    }
                }
            }
        }
    }
}
