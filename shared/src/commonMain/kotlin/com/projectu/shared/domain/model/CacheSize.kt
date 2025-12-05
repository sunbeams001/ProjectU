package com.projectu.shared.domain.model

/**
 * 图片缓存大小设置
 * 定义了不同的磁盘缓存容量选项
 */
enum class CacheSize(
    val displayNameKey: String,
    val sizeInMB: Long
) {
    /**
     * 小容量缓存 (256 MB)
     */
    SMALL("cache_size_small", 256),
    
    /**
     * 中等容量缓存 (512 MB)
     */
    MEDIUM("cache_size_medium", 512),
    
    /**
     * 大容量缓存 (1 GB)
     */
    LARGE("cache_size_large", 1024),
    
    /**
     * 超大容量缓存 (2 GB)
     */
    EXTRA_LARGE("cache_size_extra_large", 2048),
    
    /**
     * 不限制缓存大小
     * 使用 Long.MAX_VALUE 表示无限制，不进行自动清理
     */
    UNLIMITED("cache_size_unlimited", Long.MAX_VALUE / (1024 * 1024));
    
    /**
     * 获取字节数
     * 对于 UNLIMITED，返回 Long.MAX_VALUE
     */
    val sizeInBytes: Long
        get() = if (this == UNLIMITED) Long.MAX_VALUE else sizeInMB * 1024 * 1024
    
    /**
     * 是否为无限制模式
     */
    val isUnlimited: Boolean
        get() = this == UNLIMITED
    
    companion object {
        /**
         * 从名称获取枚举
         */
        fun fromName(name: String): CacheSize {
            return entries.find { it.name == name } ?: LARGE
        }
        
        /**
         * 默认缓存大小
         */
        val DEFAULT = LARGE
    }
}
