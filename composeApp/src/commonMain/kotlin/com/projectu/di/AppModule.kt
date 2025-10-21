package com.projectu.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

/**
 * 初始化Koin依赖注入
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(
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

