package com.projectu.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * 存储权限管理器
 * 根据 Android 版本请求相应的权限
 * 
 * minSdk=24, targetSdk=36:
 * - Android 7-9 (API 24-28): WRITE_EXTERNAL_STORAGE
 * - Android 10-12 (API 29-32): 无需权限（MediaStore）
 * - Android 13+ (API 33+): READ_MEDIA_IMAGES, READ_MEDIA_VIDEO
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
            // Android 13+ (API 33+): 需要读取媒体权限
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                hasPermission(Manifest.permission.READ_MEDIA_IMAGES) &&
                hasPermission(Manifest.permission.READ_MEDIA_VIDEO)
            }
            // Android 10-12 (API 29-32): MediaStore 不需要权限
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
            // Android 13+: 请求媒体权限
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            }
            // Android 10-12: 无需请求（MediaStore）
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // 理论上不会执行到这里
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
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES) ||
                activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_VIDEO)
            }
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
@androidx.compose.runtime.Composable
fun rememberStoragePermissionManager(
    activity: ComponentActivity = androidx.compose.ui.platform.LocalContext.current as ComponentActivity
): StoragePermissionManager {
    return androidx.compose.runtime.remember(activity) {
        StoragePermissionManager(activity)
    }
}
