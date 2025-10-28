package com.projectu.di

import com.projectu.ui.localization.LocaleManager
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * 初始化Koin依赖注入
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(
        commonModule,
        networkModule,
        databaseModule,
        repositoryModule,
        useCaseModule,
        viewModelModule
    )
}

/**
 * 网络层模块
 */
expect val networkModule: Module

/**
 * 数据库模块
 */
expect val databaseModule: Module

/**
 * Repository模块
 */
expect val repositoryModule: Module

/**
 * UseCase模块
 */
expect val useCaseModule: Module

/**
 * ViewModel模块
 */
expect val viewModelModule: Module

/**
 * 通用模块 - 跨平台共享的组件
 */
val commonModule: Module = module {
    // 语言管理器 - 需要 SettingsRepository 依赖
    single { LocaleManager(get()) }
}

