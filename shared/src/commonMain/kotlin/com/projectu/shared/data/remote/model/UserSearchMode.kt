package com.projectu.shared.data.remote.model

/**
 * 用户搜索模式
 * 用于指定用户名称匹配方式
 */
enum class UserSearchMode(
    val value: String,
    val displayNameKey: String,
    val descriptionKey: String
) {
    /** 部分一致 */
    PARTIAL(
        value = "s_usr",
        displayNameKey = "search_mode_user_partial",
        descriptionKey = "search_mode_user_partial_desc"
    ),

    /** 完全一致 */
    EXACT(
        value = "s_usr_full",
        displayNameKey = "search_mode_user_exact",
        descriptionKey = "search_mode_user_exact_desc"
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
