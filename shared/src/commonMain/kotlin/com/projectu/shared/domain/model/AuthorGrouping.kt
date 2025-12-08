package com.projectu.shared.domain.model

/**
 * 作者分组模式
 */
enum class AuthorGrouping {
    /**
     * 按作者ID创建子文件夹
     */
    BY_ID,
    
    /**
     * 按作者名创建子文件夹
     */
    BY_NAME,
    
    /**
     * 不创建作者子文件夹
     */
    NONE
}
