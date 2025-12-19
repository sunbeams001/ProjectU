package com.projectu.shared.data.remote.model

/**
 * 小说搜索模式
 * 用于指定搜索范围和匹配方式
 */
enum class NovelSearchMode(
    val value: String,
    val displayNameKey: String,
    val descriptionKey: String
) {
    /** 标签（部分一致） */
    TAG_ONLY(
        value = "s_tag_only",
        displayNameKey = "search_mode_tag_partial",
        descriptionKey = "search_mode_tag_partial_desc"
    ),

    /** 标签（完全一致） */
    TAG_FULL(
        value = "s_tag_full",
        displayNameKey = "search_mode_tag_full",
        descriptionKey = "search_mode_tag_full_desc"
    ),

    /** 正文 */
    TEXT_CONTENT(
        value = "s_tc",
        displayNameKey = "search_mode_text",
        descriptionKey = "search_mode_text_desc"
    ),

    /** 标签、标题、说明文字 */
    TAG_TITLE_CAPTION(
        value = "s_tag",
        displayNameKey = "search_mode_keyword",
        descriptionKey = "search_mode_keyword_desc"
    );

    companion object {
        /**
         * 根据值获取搜索模式
         */
        fun fromValue(value: String): NovelSearchMode? {
            return entries.find { it.value == value }
        }

        /**
         * 默认搜索模式
         */
        val DEFAULT = TAG_TITLE_CAPTION
    }
}
