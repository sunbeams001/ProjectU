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
    EXTRA_LARGE("cache_size_extra_large", 2048);
    
    /**
     * 获取字节数
     */
    val sizeInBytes: Long
        get() = sizeInMB * 1024 * 1024
    
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
