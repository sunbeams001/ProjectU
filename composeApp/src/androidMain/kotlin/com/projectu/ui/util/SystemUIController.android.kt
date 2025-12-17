package com.projectu.ui.util

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * 隐藏系统UI（状态栏和导航栏）
 * Android实现
 */
@Composable
actual fun HideSystemUI() {
    val view = LocalView.current
    
    DisposableEffect(Unit) {
        val activity = view.context as? Activity
        val window = activity?.window
        
        if (window != null) {
            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            
            windowInsetsController.apply {
                // 隐藏状态栏和导航栏
                hide(WindowInsetsCompat.Type.systemBars())
                // 设置行为：滑动时临时显示，然后自动隐藏
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            
            onDispose {
                // 恢复系统UI
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        } else {
            onDispose { }
        }
    }
}
