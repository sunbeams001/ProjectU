import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
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
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
            // 启用 expect/actual 类支持（Beta 特性）
            freeCompilerArgs.add("-Xexpect-actual-classes")
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
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            
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
            
            // Compose Resources多语言 (内置支持)
            implementation(compose.components.resources)
            
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
            implementation(compose.preview)
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
                        implementation(compose.desktop.windows_arm64)
                    } else {
                        implementation(compose.desktop.windows_x64)
                    }
                }
                osName.contains("mac") || osName.contains("osx") -> {
                    if (osArch.contains("aarch64") || osArch.contains("arm")) {
                        implementation(compose.desktop.macos_arm64)
                    } else {
                        implementation(compose.desktop.macos_x64)
                    }
                }
                osName.contains("linux") -> {
                    if (osArch.contains("aarch64") || osArch.contains("arm")) {
                        implementation(compose.desktop.linux_arm64)
                    } else {
                        implementation(compose.desktop.linux_x64)
                    }
                }
                else -> {
                    // 回退到 currentOs（包含所有平台）
                    implementation(compose.desktop.currentOs)
                }
            }
            
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.cio)
            
            // JOGAMP 依赖 - 使用本地 JAR 文件（解决官方仓库不稳定问题）
            implementation(files("../libs/jogamp/gluegen-rt-2.5.0.jar"))
            implementation(files("../libs/jogamp/jogl-all-2.5.0.jar"))

            // Room数据库依赖 - Desktop平台需要
            implementation(libs.room.runtime)
        }
    }
}

android {
    namespace = "com.projectu"
    compileSdk = 36
    
    defaultConfig {
        applicationId = "com.projectu"
        minSdk = 24
        targetSdk = 36
        versionCode = 8
        versionName = "1.0.7"
    }
    
    // 读取签名配置
    // 优先级：1. 本地 keystore.properties 文件  2. 环境变量（用于 CI/CD）  3. 回退到 debug 签名
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val hasKeystoreProperties = keystorePropertiesFile.exists()
    val hasEnvironmentConfig = System.getenv("RELEASE_KEYSTORE_FILE") != null
    
    // 总是创建 release 签名配置
    signingConfigs {
        create("release") {
            if (hasKeystoreProperties) {
                // 从本地 keystore.properties 文件读取
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                
                // 启用 V1 和 V2 签名
                enableV1Signing = true
                enableV2Signing = true
                
                println("✓ Using release signing config from keystore.properties")
            } else if (hasEnvironmentConfig) {
                // 从环境变量读取（用于 GitHub Actions）
                storeFile = file(System.getenv("RELEASE_KEYSTORE_FILE") ?: "")
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: ""
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: ""
                
                // 启用 V1 和 V2 签名
                enableV1Signing = true
                enableV2Signing = true
                
                println("✓ Using release signing config from environment variables")
            } else {
                // 没有自定义签名配置时，不设置任何属性，将使用 debug 签名
                println("⚠ No signing config found, release build will use debug signing")
                println("  To configure release signing:")
                println("  1. Copy keystore.properties.example to keystore.properties")
                println("  2. Update with your keystore information")
                println("  3. See docs/guides/签名配置指南.md for details")
            }
        }
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    
    buildTypes {
        getByName("debug") {
            isDebuggable = true
        }
        
        getByName("release") {
            // 根据是否有签名配置，选择使用 release 或 debug 签名
            signingConfig = if (hasKeystoreProperties || hasEnvironmentConfig) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = false
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

compose.desktop {
    application {
        mainClass = "com.projectu.MainKt"
        
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ProjectU"
            packageVersion = "1.0.7"
            
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
