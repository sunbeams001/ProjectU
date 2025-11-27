package com.projectu.ui.screens.discovery

/**
 * 发现页面内容类型
 */
enum class DiscoveryContentType(val displayName: String) {
    USERS("推荐用户"),
    ILLUSTS("推荐插画·漫画"),
    NOVELS("推荐小说");
    
    companion object {
        fun getAll(): List<DiscoveryContentType> = listOf(USERS, ILLUSTS, NOVELS)
    }
}
