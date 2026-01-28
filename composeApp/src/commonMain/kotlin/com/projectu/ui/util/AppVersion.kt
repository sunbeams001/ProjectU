package com.projectu.ui.util

/**
 * 应用版本信息
 * 从 gradle 配置文件中获取版本号
 */
object AppVersion {
    /**
     * 版本名称 (例如: "1.0.10")
     * 这个值会在编译时从 libs.versions.toml 中注入
     */
    const val VERSION_NAME = "1.0.10"  // 从 appVersion 获取
    
    /**
     * 版本代码 (例如: 11)
     * 这个值会在编译时从 libs.versions.toml 中注入
     */
    const val VERSION_CODE = 11  // 从 appVersionCode 获取
}
