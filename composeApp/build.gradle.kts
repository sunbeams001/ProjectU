import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
}

// 配置所有 configuration，排除从 compose-webview-multiplatform 传递来的 JOGAMP 依赖
// 我们使用本地 JAR 文件来代替，避免从 jogamp.org 下载超时
configurations.all {
    exclude(group = "org.jogamp.gluegen", module = "gluegen-rt")
    exclude(group = "org.jogamp.jogl", module = "jogl-all")
}

kotlin {
    androidLibrary {
        namespace = "com.projectu.composeapp"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
            // 启用 expect/actual 类支持（Beta 特性）
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }

        androidResources {
            enable = true
        }
    }
    
    jvm("desktop") {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            // 启用 expect/actual 类支持（Beta 特性）
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }
    
    sourceSets {
        val desktopMain by getting
        
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
            
            // 依赖shared模块
            implementation(projects.shared)
            
            // Koin依赖注入
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            
            // Voyager导航
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.tab.navigator)
            implementation(libs.voyager.transitions)
            implementation(libs.voyager.koin)
            
            // Coil图片加载
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            
            // Ktor (for ImageLoader configuration)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            
            // Kotlinx库
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            
            // Lifecycle
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            
            // WebView Multiplatform - 排除 JOGAMP 依赖，使用本地 JAR
            implementation(libs.compose.webview.multiplatform)
        }
        
        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.koin.android)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
            
            // Room数据库依赖 - Android平台需要
            implementation(libs.room.runtime)
            
            // WorkManager - Widget后台更新
            implementation(libs.androidx.work.runtime.ktx)
        }
        
        desktopMain.dependencies {
            // 根据操作系统选择特定平台的依赖，避免包含所有平台的资源
            // 这样可以显著减小最终打包体积（从 ~500MB 降至 ~150MB）
            val osName = System.getProperty("os.name").lowercase()
            val osArch = System.getProperty("os.arch").lowercase()
            
            when {
                osName.contains("win") -> {
                    if (osArch.contains("aarch64") || osArch.contains("arm")) {
                        implementation(libs.compose.desktop.windows.arm64)
                    } else {
                        implementation(libs.compose.desktop.windows.x64)
                    }
                }
                osName.contains("mac") || osName.contains("osx") -> {
                    if (osArch.contains("aarch64") || osArch.contains("arm")) {
                        implementation(libs.compose.desktop.macos.arm64)
                    } else {
                        implementation(libs.compose.desktop.macos.x64)
                    }
                }
                osName.contains("linux") -> {
                    if (osArch.contains("aarch64") || osArch.contains("arm")) {
                        implementation(libs.compose.desktop.linux.arm64)
                    } else {
                        implementation(libs.compose.desktop.linux.x64)
                    }
                }
                else -> {
                    // 回退到 currentOs（包含所有平台）
                    implementation(compose.desktop.currentOs)
                }
            }
            
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.cio)
            
            // JOGAMP 依赖 - 从 Maven Central 获取（已在 Maven Central 上稳定可用）
            implementation(libs.jogamp.gluegen.rt)
            implementation(libs.jogamp.jogl.all)

            // Room数据库依赖 - Desktop平台需要
            implementation(libs.room.runtime)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.projectu.MainKt"
        
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ProjectU"
            packageVersion = libs.versions.appVersion.get()
            
            // 添加 JVM 模块 - 确保运行时包含 jdk.unsupported 模块
            // 这个模块包含 sun.misc.Unsafe，被 Protobuf 库使用
            modules("jdk.unsupported")
            
            windows {
                iconFile.set(project.file("src/desktopMain/resources/icon.ico"))
            }
            linux {
                iconFile.set(project.file("src/desktopMain/resources/icon.png"))
            }
            macOS {
                iconFile.set(project.file("src/desktopMain/resources/icon.icns"))
            }
            
            // JVM 参数 - 用于打包后的应用
            // KCEF (WebView) 和 JavaCV 所需的 JVM 参数
            jvmArgs(
                "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                "--add-opens", "java.base/java.nio=ALL-UNNAMED",  
                "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED",
                "--add-opens", "jdk.unsupported/sun.misc=ALL-UNNAMED",
                "--add-opens", "java.desktop/sun.awt=ALL-UNNAMED",
                "--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED"
            )
        }
    }
}

// KCEF (WebView) 所需的 JVM 参数
afterEvaluate {
    tasks.withType<JavaExec> {
        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED")

        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }
    }
}
