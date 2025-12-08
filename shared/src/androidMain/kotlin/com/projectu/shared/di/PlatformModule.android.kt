package com.projectu.shared.di

import android.content.Context
import com.projectu.shared.data.util.AndroidFileWriter
import com.projectu.shared.data.util.PlatformFileWriter
import org.koin.dsl.module

/**
 * Android 平台特定的依赖注入模块
 */
fun androidPlatformModule(context: Context) = module {
    single<PlatformFileWriter> {
        AndroidFileWriter(context)
    }
}
