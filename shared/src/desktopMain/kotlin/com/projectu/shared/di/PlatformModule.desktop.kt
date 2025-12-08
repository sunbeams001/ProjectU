package com.projectu.shared.di

import com.projectu.shared.data.util.DesktopFileWriter
import com.projectu.shared.data.util.PlatformFileWriter
import okio.FileSystem
import org.koin.dsl.module

/**
 * Desktop 平台特定的依赖注入模块
 */
fun desktopPlatformModule() = module {
    single<PlatformFileWriter> {
        DesktopFileWriter(FileSystem.SYSTEM)
    }
}
