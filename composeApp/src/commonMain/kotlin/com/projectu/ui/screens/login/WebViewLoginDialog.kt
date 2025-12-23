package com.projectu.ui.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import com.projectu.ui.util.extractCookiesFromWebView
import com.projectu.ui.util.extractPhpSessionId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*

/**
 * WebView登录页面
 * 
 * 通过内嵌WebView加载Pixiv登录页面，用户登录成功后自动提取PHPSESSID
 * 
 * @param onSuccess 登录成功回调，返回PHPSESSID
 * @param onDismiss 关闭对话框回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewLoginDialog(
    onSuccess: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val webViewState = rememberWebViewState("https://accounts.pixiv.net/login")
    val navigator = rememberWebViewNavigator()
    val coroutineScope = rememberCoroutineScope()
    
    var nativeWebView by remember { mutableStateOf<Any?>(null) }
    var isCheckingCookie by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // 预加载错误消息字符串
    val errorCredentials = stringResource(Res.string.login_webview_error_credentials)
    val errorCookie = stringResource(Res.string.login_webview_error_cookie)
    
    // 监听URL变化，检测登录成功
    LaunchedEffect(webViewState.lastLoadedUrl) {
        val url = webViewState.lastLoadedUrl ?: return@LaunchedEffect
        
        // 当URL跳转到Pixiv主站（非登录页）时，认为登录成功
        if (url.contains("pixiv.net") && 
            !url.contains("accounts.pixiv.net") &&
            !isCheckingCookie &&
            nativeWebView != null
        ) {
            isCheckingCookie = true
            
            coroutineScope.launch {
                try {
                    // 延迟一下确保Cookie已设置
                    delay(500)
                    
                    // 提取Cookie
                    val cookies = extractCookiesFromWebView(
                        nativeWebView = nativeWebView!!,
                        domain = "https://www.pixiv.net"
                    )
                    
                    val phpsessid = extractPhpSessionId(cookies)
                    
                    if (phpsessid != null) {
                        onSuccess(phpsessid)
                    } else {
                        errorMessage = errorCredentials
                        isCheckingCookie = false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    errorMessage = "$errorCookie: ${e.message}"
                    isCheckingCookie = false
                }
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.9f)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 顶部导航栏
                TopAppBar(
                    title = { 
                        Text(stringResource(Res.string.login_webview_title)) 
                    },
                    navigationIcon = {
                        if (navigator.canGoBack) {
                            IconButton(onClick = { navigator.navigateBack() }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = stringResource(Res.string.common_back)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(Res.string.common_close)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                
                // 加载进度条
                val loadingState = webViewState.loadingState
                if (loadingState is LoadingState.Loading) {
                    LinearProgressIndicator(
                        progress = { loadingState.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // 正在检查Cookie的提示
                if (isCheckingCookie) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Text(
                                text = stringResource(Res.string.login_webview_extracting),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                // 错误提示
                errorMessage?.let { error ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                
                // WebView内容
                WebView(
                    state = webViewState,
                    navigator = navigator,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    onCreated = { native ->
                        nativeWebView = native
                    }
                )
                
                // 底部提示
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(Res.string.login_webview_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}
