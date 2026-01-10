package com.projectu.ui.screens.discovery

import org.jetbrains.compose.resources.StringResource
import projectu.composeapp.generated.resources.Res
import projectu.composeapp.generated.resources.discovery_pixivision
import projectu.composeapp.generated.resources.discovery_recommended_illusts
import projectu.composeapp.generated.resources.discovery_recommended_novels
import projectu.composeapp.generated.resources.discovery_recommended_users

/**
 * 发现页面内容类型
 */
enum class DiscoveryContentType(val displayNameRes: StringResource) {
    USERS(Res.string.discovery_recommended_users),
    ILLUSTS(Res.string.discovery_recommended_illusts),
    NOVELS(Res.string.discovery_recommended_novels),
    PIXIVISION(Res.string.discovery_pixivision);
    
    companion object {
        fun getAll(): List<DiscoveryContentType> = listOf(USERS, ILLUSTS, NOVELS, PIXIVISION)
    }
}
