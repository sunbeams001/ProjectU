package com.projectu.ui.screens.novel

/**
 * 小说显示模式
 * 
 * 定义小说阅读页面的三种显示模式：
 * - ORIGINAL: 仅显示原文
 * - TRANSLATED: 仅显示翻译（自动翻译）
 * - BILINGUAL: 对照模式，同时显示原文和翻译
 */
enum class NovelDisplayMode {
    /** 原文模式：仅显示原文 */
    ORIGINAL,
    
    /** 翻译模式：仅显示翻译（会自动触发翻译） */
    TRANSLATED,
    
    /** 对照模式：同时显示原文和翻译 */
    BILINGUAL
}
