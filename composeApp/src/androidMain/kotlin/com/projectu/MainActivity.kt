package com.projectu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.projectu.shared.data.local.database.ContextHolder
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val settingsRepository: com.projectu.shared.domain.repository.SettingsRepository by inject()
    
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
        
        // 异步加载设置，完成后隐藏启动画面
        lifecycleScope.launch {
            // 预加载设置，确保主题数据已准备好
            settingsRepository.getSettings().first()
            // 设置加载完成，允许启动画面消失
            keepSplashScreen = false
        }
        
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}

