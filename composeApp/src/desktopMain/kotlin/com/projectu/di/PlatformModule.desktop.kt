package com.projectu.di

import com.projectu.shared.util.NetworkClient
import io.ktor.client.engine.cio.*
import org.koin.core.module.Module
import org.koin.dsl.module

actual val networkModule: Module = module {
    single { NetworkClient.create(CIO.create()) }
}

actual val databaseModule: Module = module {
    // TODO: 配置Desktop数据库
}

actual val repositoryModule: Module = module {
    // TODO: Repository实现
}

actual val useCaseModule: Module = module {
    // TODO: UseCase实现
}

actual val viewModelModule: Module = module {
    // TODO: ViewModel实现
}

