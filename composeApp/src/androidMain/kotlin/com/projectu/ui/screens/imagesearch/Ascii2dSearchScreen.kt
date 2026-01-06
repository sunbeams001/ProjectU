package com.projectu.ui.screens.imagesearch

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.webkit.JavascriptInterface
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.Res
import projectu.composeapp.generated.resources.ascii2d_search_title
import projectu.composeapp.generated.resources.ascii2d_search_uploading
import projectu.composeapp.generated.resources.ascii2d_search_verifying
import projectu.composeapp.generated.resources.ascii2d_search_failed
import projectu.composeapp.generated.resources.ascii2d_search_retry
import projectu.composeapp.generated.resources.nav_back
import java.io.ByteArrayOutputStream

/**
 * Ascii2d 搜索状态
 */
sealed class Ascii2dSearchState {
    data object Idle : Ascii2dSearchState()
    data object LoadingPage : Ascii2dSearchState()
    data object Verifying : Ascii2dSearchState()
    data object Uploading : Ascii2dSearchState()
    data object ShowingResults : Ascii2dSearchState()
    data class Error(val message: String) : Ascii2dSearchState()
}

/**
 * Ascii2d 搜索 ScreenModel
 */
class Ascii2dSearchScreenModel : ScreenModel {
    private val _state = MutableStateFlow<Ascii2dSearchState>(Ascii2dSearchState.Idle)
    val state: StateFlow<Ascii2dSearchState> = _state.asStateFlow()
    // 缓存 WebView，避免在导航返回时重新创建和重新加载
    var cachedWebView: WebView? = null
    
    fun updateState(newState: Ascii2dSearchState) {
        screenModelScope.launch {
            _state.value = newState
        }
    }
    
    fun onPageStarted(url: String) {
        when {
            url == "https://ascii2d.net/" -> {
                _state.value = Ascii2dSearchState.LoadingPage
            }
            url.contains("challenges.cloudflare.com") || 
            url.contains("challenge-platform") -> {
                _state.value = Ascii2dSearchState.Verifying
            }
        }
    }
    
    fun onPageFinished(url: String) {
        when {
            url == "https://ascii2d.net/" && _state.value !is Ascii2dSearchState.Uploading -> {
                // 首页加载完成，准备上传
            }
            url.startsWith("https://ascii2d.net/search/") -> {
                _state.value = Ascii2dSearchState.ShowingResults
            }
        }
    }
    
    fun onUploadStarted() {
        _state.value = Ascii2dSearchState.Uploading
    }
    
    fun onError(message: String) {
        _state.value = Ascii2dSearchState.Error(message)
    }
}

/**
 * Ascii2d 图片搜索页面
 * 
 * @param imageUri 要搜索的图片 URI
 */
data class Ascii2dSearchScreen(
    private val imageUri: String
) : Screen {
    
    override val key: ScreenKey
        get() = "Ascii2dSearchScreen_$imageUri"
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val screenModel = rememberScreenModel { Ascii2dSearchScreenModel() }
        val searchState by screenModel.state.collectAsState()
        
        // 多语言字符串
        val titleText = stringResource(Res.string.ascii2d_search_title)
        val uploadingText = stringResource(Res.string.ascii2d_search_uploading)
        val verifyingText = stringResource(Res.string.ascii2d_search_verifying)
        val failedText = stringResource(Res.string.ascii2d_search_failed)
        val retryText = stringResource(Res.string.ascii2d_search_retry)
        val backText = stringResource(Res.string.nav_back)
        
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
                // WebView 容器
                Ascii2dWebView(
                    imageUri = imageUri,
                    context = context,
                    screenModel = screenModel,
                    navigator = navigator,
                    modifier = Modifier.fillMaxSize()
                )
                
                // 状态覆盖层
                when (val state = searchState) {
                    is Ascii2dSearchState.LoadingPage,
                    is Ascii2dSearchState.Verifying -> {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (state is Ascii2dSearchState.Verifying) {
                                        verifyingText
                                    } else {
                                        uploadingText
                                    }
                                )
                            }
                        }
                    }
                    
                    is Ascii2dSearchState.Uploading -> {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(uploadingText)
                            }
                        }
                    }
                    
                    is Ascii2dSearchState.Error -> {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surface
                        ) {
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
                                    screenModel.updateState(Ascii2dSearchState.Idle)
                                    // WebView 会自动重新加载
                                }) {
                                    Text(retryText)
                                }
                            }
                        }
                    }
                    
                    else -> {
                        // 正常显示 WebView
                    }
                }
            }
        }
    }
}

/**
 * Ascii2d WebView 组件
 */
@Composable
private fun Ascii2dWebView(
    imageUri: String,
    context: Context,
    screenModel: Ascii2dSearchScreenModel,
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var shouldInjectUpload by remember { mutableStateOf(false) }
    
    AndroidView(
        factory = { ctx ->
            // 如果已有缓存的 WebView，复用它（先从父容器移除）
            val existing = screenModel.cachedWebView
            if (existing != null) {
                try {
                    val parent = existing.parent
                    if (parent is android.view.ViewGroup) {
                        parent.removeView(existing)
                    }
                } catch (e: Exception) {
                    // ignore
                }
                webView = existing
                return@AndroidView existing
            }

            WebView(ctx).apply {
                screenModel.cachedWebView = this
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    loadsImagesAutomatically = true
                    // 允许混合内容
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                }
                
                // 设置长按监听器
                setOnLongClickListener { view ->
                    val result = (view as? WebView)?.hitTestResult
                    val url = result?.extra
                    
                    if (url != null && (result.type == WebView.HitTestResult.SRC_ANCHOR_TYPE ||
                                result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE)) {
                        // 长按任何链接都在系统浏览器中打开
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        return@setOnLongClickListener true
                    }
                    false
                }
                
                // 添加 JavaScript 接口用于调试
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun log(message: String) {
                        println("Ascii2d JS: $message")
                    }
                    
                    @JavascriptInterface
                    fun onUploadSuccess() {
                        screenModel.onUploadStarted()
                    }
                    
                    @JavascriptInterface
                    fun onUploadError(error: String) {
                        screenModel.onError(error)
                    }
                }, "Android")
                
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let { screenModel.onPageStarted(it) }
                    }
                    
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        url?.let { 
                            screenModel.onPageFinished(it)
                            
                            // 如果是首页加载完成，注入上传脚本
                            if (it == "https://ascii2d.net/" && !shouldInjectUpload) {
                                shouldInjectUpload = true
                                injectImageUploadScript(view, imageUri, context, screenModel)
                            }
                        }
                    }
                    
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: return false
                        
                        // 检查是否是 Pixiv 链接
                        val target = DeepLinkParser.parse(url)
                        if (target != DeepLinkTarget.Unknown) {
                            // 跳转到 App 内页面
                            DeepLinkHandler.handleTarget(target, navigator)
                            return true
                        }
                        
                        // 其他链接在 WebView 中打开
                        return false
                    }
                }
                
                webView = this
                
                // 加载 ascii2d 首页
                loadUrl("https://ascii2d.net/")
            }
        },
        modifier = modifier
    )
}

/**
 * 注入图片上传脚本
 */
private fun injectImageUploadScript(
    webView: WebView?,
    imageUri: String,
    context: Context,
    screenModel: Ascii2dSearchScreenModel
) {
    webView ?: return
    
    // 读取并优化图片
    val imageBase64 = try {
        getOptimizedImageBase64(imageUri, context)
    } catch (e: Exception) {
        screenModel.onError("Failed to read image: ${e.message}")
        return
    }
    
    if (imageBase64 == null) {
        screenModel.onError("Failed to process image")
        return
    }
    
    // 注入 JS 代码
    val javascript = """
        (function() {
            try {
                Android.log('Starting image upload process...');
                
                // 1. 找到文件输入框
                const fileInput = document.querySelector('input[type="file"][name="file"]');
                if (!fileInput) {
                    Android.onUploadError('File input not found');
                    return;
                }
                Android.log('File input found');
                
                // 2. 将 Base64 转换为 Blob
                const base64Data = '$imageBase64';
                Android.log('Base64 data length: ' + base64Data.length);
                
                const byteCharacters = atob(base64Data);
                const byteNumbers = new Array(byteCharacters.length);
                for (let i = 0; i < byteCharacters.length; i++) {
                    byteNumbers[i] = byteCharacters.charCodeAt(i);
                }
                const byteArray = new Uint8Array(byteNumbers);
                const blob = new Blob([byteArray], { type: 'image/jpeg' });
                Android.log('Blob created, size: ' + blob.size);
                
                // 3. 创建 File 对象
                const file = new File([blob], 'image.jpg', { type: 'image/jpeg' });
                Android.log('File object created');
                
                // 4. 创建 DataTransfer 对象并设置文件
                const dataTransfer = new DataTransfer();
                dataTransfer.items.add(file);
                fileInput.files = dataTransfer.files;
                Android.log('File set to input');
                
                // 5. 触发 change 事件
                const changeEvent = new Event('change', { bubbles: true });
                fileInput.dispatchEvent(changeEvent);
                Android.log('Change event dispatched');
                
                // 6. 查找并提交表单
                setTimeout(() => {
                    const form = fileInput.closest('form');
                    if (form) {
                        Android.log('Form found, submitting...');
                        Android.onUploadSuccess();
                        form.submit();
                    } else {
                        Android.onUploadError('Form not found');
                    }
                }, 800);
                
            } catch (error) {
                Android.onUploadError('Error: ' + error.message);
            }
        })();
    """.trimIndent()
    
    webView.evaluateJavascript(javascript, null)
}

/**
 * 获取优化后的图片 Base64
 * 限制最大 5MB，超过则压缩
 */
private fun getOptimizedImageBase64(
    imageUri: String,
    context: Context,
    maxSizeBytes: Long = 5 * 1024 * 1024  // 5MB
): String? {
    return try {
        val uri = Uri.parse(imageUri)
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val originalBytes = inputStream.readBytes()
        inputStream.close()
        
        val finalBytes = if (originalBytes.size > maxSizeBytes) {
            // 需要压缩
            val bitmap = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size)
            compressBitmap(bitmap, maxSizeBytes)
        } else {
            originalBytes
        }
        
        Base64.encodeToString(finalBytes, Base64.NO_WRAP)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * 压缩图片到指定大小以下
 */
private fun compressBitmap(bitmap: Bitmap, maxSizeBytes: Long): ByteArray {
    var quality = 90
    var outputBytes: ByteArray
    
    do {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        outputBytes = outputStream.toByteArray()
        quality -= 10
    } while (outputBytes.size > maxSizeBytes && quality > 10)
    
    return outputBytes
}
