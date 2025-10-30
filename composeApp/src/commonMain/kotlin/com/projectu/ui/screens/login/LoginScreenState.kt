package com.projectu.ui.screens.login

import com.projectu.shared.data.local.PixivConfig

/**
 * 登录屏幕状态
 */
data class LoginScreenState(
    /**
     * PHPSESSID 输入值
     */
    val phpSessionId: String = "",
    
    /**
     * 是否正在加载
     */
    val isLoading: Boolean = false,
    
    /**
     * 错误消息
     */
    val errorMessage: String? = null,
    
    /**
     * 是否显示帮助对话框
     */
    val showHelpDialog: Boolean = false,
    
    /**
     * 登录模式
     * - PHPSESSID: 手动输入 PHPSESSID (当前实现)
     * - APP_LOGIN: 应用内登录 (预留)
     */
    val loginMode: LoginMode = LoginMode.PHPSESSID
)

/**
 * 登录模式
 */
enum class LoginMode {
    /**
     * 手动输入 PHPSESSID
     */
    PHPSESSID,
    
    /**
     * 应用内登录（预留功能）
     */
    APP_LOGIN
}

/**
 * 登录屏幕意图
 */
sealed interface LoginIntent {
    /**
     * PHPSESSID 输入变化
     */
    data class PhpSessionIdChanged(val value: String) : LoginIntent
    
    /**
     * 点击登录按钮
     */
    data object LoginClicked : LoginIntent
    
    /**
     * 切换登录模式
     */
    data class SwitchLoginMode(val mode: LoginMode) : LoginIntent
    
    /**
     * 显示/隐藏帮助对话框
     */
    data class ToggleHelpDialog(val show: Boolean) : LoginIntent
    
    /**
     * 清除错误消息
     */
    data object ClearError : LoginIntent
}
