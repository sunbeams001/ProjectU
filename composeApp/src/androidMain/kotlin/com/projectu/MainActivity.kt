package com.projectu

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.projectu.shared.data.local.database.ContextHolder
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * 分享的图片数据
 */
data class SharedImageData(
    val imageUri: String
)

class MainActivity : ComponentActivity() {
    private val settingsRepository: com.projectu.shared.domain.repository.SettingsRepository by inject()
    
    // 存储待处理的深度链接
    private val pendingDeepLink = mutableStateOf<String?>(null)
    
    // 存储待处理的分享图片
    private val pendingSharedImage = mutableStateOf<SharedImageData?>(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // 用于控制启动画面显示时长
        var keepSplashScreen = true
        
        // 安装启动画面（必须在 super.onCreate 之前调用）
        val splashScreen = installSplashScreen()
        
        // 保持启动画面显示，直到设置加载完成
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        
        super.onCreate(savedInstanceState)
        
        // 初始化ContextHolder - 用于KMP数据库构建
        ContextHolder.setContext(this)
        
        // 处理启动时的 Intent
        handleIntent(intent)
        
        // 异步加载设置，完成后隐藏启动画面
        lifecycleScope.launch {
            // 预加载设置，确保主题数据已准备好
            settingsRepository.getSettings().first()
            // 设置加载完成，允许启动画面消失
            keepSplashScreen = false
        }
        
        enableEdgeToEdge()
        setContent {
            App(
                deepLink = pendingDeepLink.value,
                onDeepLinkConsumed = { pendingDeepLink.value = null },
                sharedImage = pendingSharedImage.value?.let { SharedImage(it.imageUri) },
                onSharedImageConsumed = { pendingSharedImage.value = null }
            )
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 处理新的 Intent（App 已在运行时收到新 Intent）
        handleIntent(intent)
    }
    
    /**
     * 处理 Intent
     */
    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            // 深度链接
            Intent.ACTION_VIEW -> {
                val uri = intent.data
                if (uri != null) {
                    pendingDeepLink.value = uri.toString()
                }
            }
            // 分享图片
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("image/") == true) {
                    handleSharedImage(intent)
                }
            }
        }
    }
    
    /**
     * 处理分享的图片
     */
    @Suppress("DEPRECATION")
    private fun handleSharedImage(intent: Intent) {
        val imageUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        if (imageUri != null) {
            pendingSharedImage.value = SharedImageData(imageUri.toString())
        }
    }
}

