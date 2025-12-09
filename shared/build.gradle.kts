import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    jvm("desktop")
    
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
                implementation(libs.javacv.platform)
            }
        }
        
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.projectu.shared"
    compileSdk = 35
    
    defaultConfig {
        minSdk = 24
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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

