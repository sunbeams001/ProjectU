package com.projectu.di

import androidx.lifecycle.viewmodel.compose.viewModel
import com.projectu.shared.data.local.SettingsStore
import com.projectu.shared.data.local.dao.SettingsDao
import com.projectu.shared.data.local.database.AppDatabase
import com.projectu.shared.data.local.database.getDatabaseBuilder
import com.projectu.shared.data.local.database.getRoomDatabase
import com.projectu.shared.data.repository.SettingsRepositoryImpl
import com.projectu.shared.domain.repository.SettingsRepository
import com.projectu.shared.util.NetworkClient
import com.projectu.ui.screens.settings.SettingsViewModel
import io.ktor.client.engine.cio.*
import org.koin.core.module.Module
import org.koin.dsl.module

actual val networkModule: Module = module {
    single { NetworkClient.create(CIO.create()) }
}

actual val databaseModule: Module = module {
    // 数据库实例 - 按照官方文档标准实现
    single {
        val builder = getDatabaseBuilder()
        getRoomDatabase(builder)
    }
    
    // DAO
    single<SettingsDao> { get<AppDatabase>().settingsDao() }
    
    // 设置存储
    single { SettingsStore(get()) }
}

actual val repositoryModule: Module = module {
    // 设置仓储
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
}

actual val useCaseModule: Module = module {
    // TODO: UseCase实现
}

actual val viewModelModule: Module = module {
    // 设置 ViewModel
    single { SettingsViewModel(get()) }
}

