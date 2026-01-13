package com.projectu.ui.util

import android.content.Context
import android.content.res.Configuration
import com.projectu.shared.data.local.AppLanguage
import java.util.Locale

/**
 * 创建带指定 Locale 的 Context
 * 用于解决 LocalContext 不遵循应用语言设置的问题
 * 
 * @param language 应用语言
 * @return 配置了正确 Locale 的 Context
 */
fun Context.createLocalizedContext(language: AppLanguage): Context {
    val locale = when (language) {
        AppLanguage.SIMPLIFIED_CHINESE -> Locale.SIMPLIFIED_CHINESE
        AppLanguage.TRADITIONAL_CHINESE -> Locale.TRADITIONAL_CHINESE
        AppLanguage.ENGLISH -> Locale.ENGLISH
        AppLanguage.JAPANESE -> Locale.JAPANESE
        AppLanguage.KOREAN -> Locale.KOREAN
    }
    
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    
    return createConfigurationContext(config)
}

/**
 * 从 Locale 创建带该 Locale 的 Context
 * 
 * @param locale Java Locale 对象
 * @return 配置了正确 Locale 的 Context
 */
fun Context.createLocalizedContext(locale: Locale): Context {
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    return createConfigurationContext(config)
}
