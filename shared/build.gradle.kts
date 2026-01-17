import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
}

kotlin {
    androidLibrary {
        namespace = "com.projectu.shared"
        compileSdk = 36
        minSdk = 24

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
        commonMain.dependencies {
            // Koin依赖注入
            implementation(libs.koin.core)
            
            // Ktor网络层
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.auth)
            
            // Room数据库
            implementation(libs.room.runtime)
            implementation(libs.sqlite.driver.bundled)
            
            // DataStore
            implementation(libs.datastore.preferences)
            
            // Kotlinx库
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            
            // Okio文件处理
            implementation(libs.okio)
            
            // Ksoup - HTML解析器
            implementation(libs.ksoup)
            
            // GIF.kt - GIF编解码
            implementation(libs.gifkt)
        }
        
        androidMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.documentfile)
        }
        
        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
                implementation(libs.kotlinx.coroutines.swing)
                
                // JavaCV for MP4 encoding on Desktop
                // 只使用 FFmpeg，排除其他不需要的本地库（opencv, openblas, tesseract等）
                // 避免包含所有平台的本地库（每个库都有 ~100MB 的多平台版本）
                implementation("org.bytedeco:javacv:1.5.12") {
                    exclude(group = "org.bytedeco", module = "opencv")
                    exclude(group = "org.bytedeco", module = "openblas")
                    exclude(group = "org.bytedeco", module = "tesseract")
                    exclude(group = "org.bytedeco", module = "leptonica")
                    exclude(group = "org.bytedeco", module = "flycapture")
                    exclude(group = "org.bytedeco", module = "libdc1394")
                    exclude(group = "org.bytedeco", module = "libfreenect")
                    exclude(group = "org.bytedeco", module = "libfreenect2")
                    exclude(group = "org.bytedeco", module = "librealsense")
                    exclude(group = "org.bytedeco", module = "librealsense2")
                    exclude(group = "org.bytedeco", module = "videoinput")
                    exclude(group = "org.bytedeco", module = "artoolkitplus")
                }
                
                val osName = System.getProperty("os.name").lowercase()
                val osArch = System.getProperty("os.arch").lowercase()
                
                when {
                    osName.contains("win") -> {
                        // Windows: 只包含 Windows x64 的 FFmpeg
                        implementation("org.bytedeco:ffmpeg:7.1.1-1.5.12")
                        implementation("org.bytedeco:ffmpeg:7.1.1-1.5.12:windows-x86_64")
                    }
                    osName.contains("mac") || osName.contains("osx") -> {
                        // macOS: 根据架构选择
                        implementation("org.bytedeco:ffmpeg:7.1.1-1.5.12")
                        if (osArch.contains("aarch64") || osArch.contains("arm")) {
                            implementation("org.bytedeco:ffmpeg:7.1.1-1.5.12:macosx-arm64")
                        } else {
                            implementation("org.bytedeco:ffmpeg:7.1.1-1.5.12:macosx-x86_64")
                        }
                    }
                    osName.contains("linux") -> {
                        // Linux: 只包含 Linux x64 的 FFmpeg
                        implementation("org.bytedeco:ffmpeg:7.1.1-1.5.12")
                        implementation("org.bytedeco:ffmpeg:7.1.1-1.5.12:linux-x86_64")
                    }
                    else -> {
                        // 回退到 platform（包含所有平台，仅用于兼容性）
                        implementation(libs.javacv.platform)
                    }
                }
            }
        }
        
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspCommonMainMetadata", libs.room.compiler)
    add("kspAndroid", libs.room.compiler)
    add("kspDesktop", libs.room.compiler)
}
