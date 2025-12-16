package com.projectu.shared.domain.model

/**
 * 收藏按钮行为配置
 * 定义点击/长按收藏按钮时的行为
 */
enum class BookmarkAction {
    /**
     * 添加公开收藏
     */
    PUBLIC,
    
    /**
     * 添加私人收藏
     */
    PRIVATE,
    
    /**
     * 弹出标签选择对话框进行收藏
     * 允许用户选择标签和公开/私人状态
     */
    WITH_TAGS
}
