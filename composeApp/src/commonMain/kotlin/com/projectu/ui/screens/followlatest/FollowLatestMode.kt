package com.projectu.ui.screens.followlatest

/**
 * 动态模式（全部/R-18）
 */
enum class FollowLatestMode(val value: String, val displayNameKey: String) {
    ALL("all", "follow_latest_mode_all"),
    R18("r18", "follow_latest_mode_r18");
    
    companion object {
        fun fromValue(value: String): FollowLatestMode {
            return entries.find { it.value == value } ?: ALL
        }
    }
}
