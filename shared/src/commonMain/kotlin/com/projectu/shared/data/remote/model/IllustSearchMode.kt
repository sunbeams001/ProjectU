package com.projectu.shared.data.remote.model

/**
 * 插画搜索模式
 * 用于指定搜索范围和匹配方式
 */
enum class IllustSearchMode(
    val value: String,
    val displayName: String,
    val description: String
) {
    /** 标签（部分一致） */
    TAG(
        value = "s_tag",
        displayName = "标签（部分一致）",
        description = "在标签中进行部分匹配搜索"
    ),

    /** 标签（完全一致） */
    TAG_FULL(
        value = "s_tag_full",
        displayName = "标签（完全一致）",
        description = "在标签中进行完全匹配搜索"
    ),

    /** 标题、说明文字 */
    TITLE_CAPTION(
        value = "s_tc",
        displayName = "标题、说明文字",
        description = "在标题和说明文字中搜索"
    );

    companion object {
        /**
         * 根据值获取搜索模式
         */
        fun fromValue(value: String): IllustSearchMode? {
            return entries.find { it.value == value }
        }

        /**
         * 默认搜索模式
         */
        val DEFAULT = TAG
    }
}
