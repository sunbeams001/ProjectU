package com.projectu.ui.util

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.projectu.shared.data.local.database.ContextHolder
import com.projectu.shared.di.pixivApiModule
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import kotlin.system.exitProcess

/**
 * Android 平台的应用重启实现
 */
actual object AppRestarter {
    private const val TAG = "AppRestarter"

    /**
     * 重启应用
     * 
     * 策略：重新启动 MainActivity 并清空 Activity 栈
     * 
     * 工作原理：
     * 1. 刷新内存中的单例缓存（重新加载 Koin 模块）
     * 2. 创建新的 MainActivity Intent，设置清空任务栈标志
     * 3. 启动全新的 MainActivity 实例
     * 
     * 能够重新加载的内容：
     * - DataStore/SharedPreferences（会重新读取）
     * - 数据库连接（Koin 单例会保持，但查询会获取新数据）
     * - UI 状态（完全重建）
     * - 主题、语言等设置（重新读取）
     * - API 凭据（通过 reload moodule 刷新）
     */
    actual fun restartApp(delayMillis: Long) {
        val context = ContextHolder.getContext()
        // Log.d(TAG, "Attempting to restart app with delay: $delayMillis ms")
        
        // 关键修复：重新加载 Pixiv API 模块
        // 因为使用 FLAG_ACTIVITY_CLEAR_TASK 不会杀死进程，Application 和 Koin 容器仍然存活
        // 所以必须手动重新加载模块，强制重新创建 PixivApiClient 单例以读取最新凭据
        try {
            // Log.d(TAG, "Reloading Pixiv API module to refresh credentials...")
            unloadKoinModules(pixivApiModule)
            loadKoinModules(pixivApiModule)
        } catch (e: Exception) {
            // Log.e(TAG, "Failed to reload Koin modules", e)
            e.printStackTrace()
        }
        
        // 使用Handler延迟执行，确保当前对话框等UI有时间完成关闭动画
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                // 获取 MainActivity 的 Intent
                val packageManager = context.packageManager
                val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                
                if (intent != null) {
                    // Log.d(TAG, "Found launch intent, recreating task stack...")
                    // 关键标志组合：
                    // FLAG_ACTIVITY_NEW_TASK: 在新任务中启动（ApplicationContext启动Activity必需）
                    // FLAG_ACTIVITY_CLEAR_TASK: 清空该任务的所有 Activity
                    // 这会导致当前所有 Activity 被销毁，然后启动全新的 MainActivity
                    intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or 
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                    )
                    
                    // 启动新的 MainActivity
                    context.startActivity(intent)
                    // Log.d(TAG, "New activity started.")
                    
                    // 注意：因为 context 是 ApplicationContext，所以不能调用 finish()
                    // 但 FLAG_ACTIVITY_CLEAR_TASK 已经负责清理旧 Activity
                } else {
                    // Log.e(TAG, "Launch intent is null!")
                    // 兜底方案：退出进程
                     exitProcess(0)
                }
            } catch (e: Exception) {
                // Log.e(TAG, "Restart failed", e)
                e.printStackTrace()
                // 如果启动失败，尝试直接退出进程，让用户手动重启
                 exitProcess(0)
            }
        }, delayMillis)
    }
}
