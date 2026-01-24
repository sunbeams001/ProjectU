package com.projectu.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat

/**
 * 存储权限管理器
 * 根据 Android 版本请求相应的权限
 * 
 * minSdk=24, targetSdk=36:
 * - Android 7-9 (API 24-28): WRITE_EXTERNAL_STORAGE
 * - Android 10+ (API 29+): 无需权限（MediaStore API 允许应用写入自己的文件）
 */
class StoragePermissionManager(
    private val activity: ComponentActivity
) {
    
    private var permissionCallback: ((Boolean) -> Unit)? = null
    
    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        permissionCallback?.invoke(allGranted)
        permissionCallback = null
    }
    
    /**
     * 检查是否有存储权限
     */
    fun hasStoragePermission(): Boolean {
        return when {
            // Android 10+ (API 29+): MediaStore API 允许无权限写入
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                true
            }
            // Android 7-9 (API 24-28): 需要写入外部存储权限
            else -> {
                hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }
    
    /**
     * 请求存储权限
     */
    fun requestStoragePermission(callback: (Boolean) -> Unit) {
        if (hasStoragePermission()) {
            callback(true)
            return
        }
        
        permissionCallback = callback
        
        val permissions = when {
            // Android 10+: 无需请求（MediaStore API）
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // 不应该执行到这里，因为hasStoragePermission()已经返回true
                callback(true)
                return
            }
            // Android 7-9: 请求写入权限
            else -> {
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        
        permissionLauncher.launch(permissions)
    }
    
    /**
     * 是否应该显示权限说明
     */
    fun shouldShowRationale(): Boolean {
        return when {
            // Android 10+: 无需权限，不显示说明
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                false
            }
            else -> {
                activity.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }
    
    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * 扩展函数：在 Composable 中使用
 */
@Composable
fun rememberStoragePermissionManager(): StoragePermissionManager {
    val activity = requireNotNull(LocalActivity.current) {
        "LocalActivity must be present"
    }
    return remember(activity) {
        StoragePermissionManager(activity as ComponentActivity)
    }
}
