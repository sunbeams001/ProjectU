package com.projectu.shared.data.remote.model

/**
 * 用户搜索模式
 * 用于指定用户名称匹配方式
 */
enum class UserSearchMode(
    val value: String,
    val displayName: String,
    val description: String
) {
    /** 部分一致 */
    PARTIAL(
        value = "s_usr",
        displayName = "部分一致",
        description = "用户名称部分匹配"
    ),

    /** 完全一致 */
    EXACT(
        value = "s_usr_full",
        displayName = "完全一致",
        description = "用户名称完全匹配"
    );

    companion object {
        /**
         * 根据值获取搜索模式
         */
        fun fromValue(value: String): UserSearchMode? {
            return entries.find { it.value == value }
        }

        /**
         * 默认搜索模式
         */
        val DEFAULT = PARTIAL
    }
}
