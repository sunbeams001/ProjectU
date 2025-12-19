package com.projectu.shared.data.remote.model

/**
 * 插画搜索模式
 * 用于指定搜索范围和匹配方式
 */
enum class IllustSearchMode(
    val value: String,
    val displayNameKey: String,
    val descriptionKey: String
) {
    /** 标签（部分一致） */
    TAG(
        value = "s_tag",
        displayNameKey = "search_mode_tag_partial",
        descriptionKey = "search_mode_tag_partial_desc"
    ),

    /** 标签（完全一致） */
    TAG_FULL(
        value = "s_tag_full",
        displayNameKey = "search_mode_tag_full",
        descriptionKey = "search_mode_tag_full_desc"
    ),

    /** 标题、说明文字 */
    TITLE_CAPTION(
        value = "s_tc",
        displayNameKey = "search_mode_title_caption",
        descriptionKey = "search_mode_title_caption_desc"
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
