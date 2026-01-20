package com.projectu

import android.app.Application
import com.projectu.di.initKoin
import com.projectu.shared.di.androidPlatformModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.loadKoinModules

class PixivApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger()
            androidContext(this@PixivApplication)
        }
        
        // 单独加载 Android 平台的 shared 模块（需要 Context）
        loadKoinModules(androidPlatformModule(this))
    }
}

