@file:Suppress("HardcodedChinese") // 该文件专门用于提供多语言文本，需要硬编码各语言字符串

package com.projectu.shared.util

import com.projectu.shared.data.local.AppLanguage

/**
 * 本地化文本提供器
 * 
 * 用于在无法访问 Compose Resources 的上下文中（如后台任务、Worker 等）提供多语言文本支持
 * 
 * 使用场景：
 * - EPUB 文件生成（后台任务）
 * - 下载任务通知
 * - 其他非 UI 线程的多语言需求
 * 
 * 注意：这是一个补充方案，UI 层应该优先使用 stringResource()
 */
object LocalizedTextProvider {
    
    /**
     * 格式化页码文本
     * 用于小说分页标题
     * 
     * @param pageNumber 页码（从 1 开始）
     * @param language 目标语言
     * @return 本地化的页码文本，例如："第1页"、"Page 1"、"ページ1"
     */
    fun formatPageNumber(pageNumber: Int, language: AppLanguage): String {
        return when (language) {
            AppLanguage.SIMPLIFIED_CHINESE -> "第${pageNumber}页"
            AppLanguage.TRADITIONAL_CHINESE -> "第${pageNumber}頁"
            AppLanguage.JAPANESE -> "ページ$pageNumber"
            AppLanguage.KOREAN -> "페이지 $pageNumber"
            AppLanguage.ENGLISH -> "Page $pageNumber"
        }
    }
}
