package com.projectu.ui.screens.imagesearch

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.ui.navigation.DeepLinkHandler
import com.projectu.ui.navigation.DeepLinkParser
import com.projectu.ui.navigation.DeepLinkTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.Res
import projectu.composeapp.generated.resources.image_search_title
import projectu.composeapp.generated.resources.image_search_loading
import projectu.composeapp.generated.resources.image_search_failed
import projectu.composeapp.generated.resources.image_search_retry
import projectu.composeapp.generated.resources.image_search_cannot_read
import projectu.composeapp.generated.resources.image_search_unknown_error
import projectu.composeapp.generated.resources.nav_back
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 图片搜索状态
 */
sealed class ImageSearchState {
    data object Idle : ImageSearchState()
    data object Loading : ImageSearchState()
    data class Success(val htmlContent: String) : ImageSearchState()
    data class Error(val message: String) : ImageSearchState()
}

/**
 * 图片搜索 ScreenModel
 * 用于保持搜索状态，避免返回页面时重新加载
 */
class ImageSearchScreenModel : ScreenModel {
    private val _state = MutableStateFlow<ImageSearchState>(ImageSearchState.Idle)
    val state: StateFlow<ImageSearchState> = _state.asStateFlow()
    
    // 记录已搜索的 imageUri，避免重复搜索
    private var searchedImageUri: String? = null
    
    /**
     * 执行图片搜索
     * 如果已经搜索过相同的图片，则跳过
     */
    fun search(
        imageUri: String,
        context: Context,
        cannotReadText: String,
        unknownErrorText: String
    ) {
        // 如果已经搜索过相同的图片，跳过
        if (searchedImageUri == imageUri && _state.value !is ImageSearchState.Idle) {
            return
        }
        
        searchedImageUri = imageUri
        _state.value = ImageSearchState.Loading
        
        screenModelScope.launch {
            try {
                val uri = Uri.parse(imageUri)
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val htmlResult = uploadToSauceNao(inputStream)
                    _state.value = ImageSearchState.Success(htmlResult)
                } else {
                    _state.value = ImageSearchState.Error(cannotReadText)
                }
            } catch (e: Exception) {
                _state.value = ImageSearchState.Error(e.message ?: unknownErrorText)
            }
        }
    }
    
    /**
     * 重试搜索
     */
    fun retry(
        imageUri: String,
        context: Context,
        cannotReadText: String,
        unknownErrorText: String
    ) {
        // 强制重新搜索
        searchedImageUri = null
        search(imageUri, context, cannotReadText, unknownErrorText)
    }
}

/**
 * 上传图片到 SauceNAO 并获取 HTML 结果
 */
private suspend fun uploadToSauceNao(inputStream: InputStream): String = withContext(Dispatchers.IO) {
    val boundary = "----WebKitFormBoundary${System.currentTimeMillis()}"
    val lineEnd = "\r\n"
    val twoHyphens = "--"
    
    // 读取图片数据
    val imageBytes = inputStream.use { it.readBytes() }
    
    // 构建 multipart form data
    val url = URL("https://saucenao.com/search.php")
    val connection = url.openConnection() as HttpURLConnection
    
    connection.apply {
        doInput = true
        doOutput = true
        useCaches = false
        requestMethod = "POST"
        setRequestProperty("Connection", "Keep-Alive")
        setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
    }
    
    // 写入请求体
    val outputStream = connection.outputStream
    val writer = outputStream.bufferedWriter()
    
    // 写入文件部分
    writer.write("$twoHyphens$boundary$lineEnd")
    writer.write("Content-Disposition: form-data; name=\"file\"; filename=\"image.jpg\"$lineEnd")
    writer.write("Content-Type: image/jpeg$lineEnd")
    writer.write(lineEnd)
    writer.flush()
    
    // 写入图片数据
    outputStream.write(imageBytes)
    outputStream.flush()
    
    // 写入结束标记
    writer.write(lineEnd)
    writer.write("$twoHyphens$boundary$twoHyphens$lineEnd")
    writer.flush()
    writer.close()
    
    // 读取响应
    val responseCode = connection.responseCode
    if (responseCode == HttpURLConnection.HTTP_OK) {
        connection.inputStream.bufferedReader().use { it.readText() }
    } else {
        throw Exception("HTTP Error: $responseCode")
    }
}

/**
 * 图片搜索页面 - 使用 SauceNAO 进行以图搜图
 * 
 * @param imageUri 要搜索的图片 URI（来自分享功能）
 */
data class ImageSearchScreen(
    private val imageUri: String
) : Screen {
    
    override val key: ScreenKey
        get() = "ImageSearchScreen_$imageUri"
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        
        // 使用 rememberScreenModel 保持状态，由 Voyager 管理生命周期
        val screenModel = rememberScreenModel { ImageSearchScreenModel() }
        val searchState by screenModel.state.collectAsState()
        
        // 多语言字符串
        val titleText = stringResource(Res.string.image_search_title)
        val loadingText = stringResource(Res.string.image_search_loading)
        val failedText = stringResource(Res.string.image_search_failed)
        val retryText = stringResource(Res.string.image_search_retry)
        val cannotReadText = stringResource(Res.string.image_search_cannot_read)
        val unknownErrorText = stringResource(Res.string.image_search_unknown_error)
        val backText = stringResource(Res.string.nav_back)
        
        // 启动搜索（仅在首次或状态为 Idle 时）
        LaunchedEffect(imageUri) {
            screenModel.search(imageUri, context, cannotReadText, unknownErrorText)
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(titleText) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = backText
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
                when (val state = searchState) {
                    is ImageSearchState.Idle -> {
                        // 空闲状态
                    }
                    
                    is ImageSearchState.Loading -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(loadingText)
                        }
                    }
                    
                    is ImageSearchState.Success -> {
                        // 使用 WebView 显示 HTML 结果
                        SauceNaoResultWebView(
                            htmlContent = state.htmlContent,
                            navigator = navigator,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    is ImageSearchState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = failedText,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                screenModel.retry(imageUri, context, cannotReadText, unknownErrorText)
                            }) {
                                Text(retryText)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 使用 WebView 显示 SauceNAO 搜索结果
 * 拦截 pixiv 链接并跳转到 App 内页面
 */
@Composable
private fun SauceNaoResultWebView(
    htmlContent: String,
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }
    
    // 用于存储待处理的导航目标
    var pendingNavigation by remember { mutableStateOf<DeepLinkTarget?>(null) }
    
    // 处理导航
    LaunchedEffect(pendingNavigation) {
        pendingNavigation?.let { target ->
            DeepLinkHandler.handleTarget(target, navigator)
            pendingNavigation = null
        }
    }
    
    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        // 允许加载图片
                        loadsImagesAutomatically = true
                        // 设置混合内容模式（允许 HTTPS 页面加载 HTTP 资源）
                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    }
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                        }
                        
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                        }
                        
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false
                            
                            // 检查是否是 pixiv 链接
                            val target = DeepLinkParser.parse(url)
                            if (target != DeepLinkTarget.Unknown) {
                                // 是 pixiv 链接，跳转到 App 内页面
                                pendingNavigation = target
                                return true
                            }
                            
                            // 其他链接使用默认行为（在 WebView 中打开）
                            return false
                        }
                    }
                    
                    // 加载 HTML 内容
                    loadDataWithBaseURL(
                        "https://saucenao.com/",
                        htmlContent,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // 加载指示器
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
