package com.projectu.shared.data.remote.model

/**
 * 发现模块的内容模式
 */
enum class DiscoveryMode(
    val value: String,
    val displayNameKey: String
) {
    /** 全部 */
    ALL("all", "discovery_mode_all"),
    
    /** 全年龄 */
    SAFE("safe", "discovery_mode_safe"),
    
    /** R-18 */
    R18("r18", "discovery_mode_r18");
    
    companion object {
        fun fromValue(value: String): DiscoveryMode {
            return entries.find { it.value == value } ?: ALL
        }
    }
}
