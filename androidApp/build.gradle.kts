import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
}

// 配置所有 configuration，排除从 compose-webview-multiplatform 传递来的 JOGAMP 依赖
// 我们使用本地 JAR 文件来代替，避免从 jogamp.org 下载超时
configurations.all {
    exclude(group = "org.jogamp.gluegen", module = "gluegen-rt")
    exclude(group = "org.jogamp.jogl", module = "jogl-all")
}

dependencies {
    implementation(projects.composeApp)
    implementation(projects.shared)

    // Compose UI 依赖
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui)
    implementation(libs.compose.components.resources)
    implementation(libs.compose.ui.tooling.preview)

    // Koin依赖注入
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)

    // Voyager导航
    implementation(libs.voyager.navigator)
    implementation(libs.voyager.koin)

    // Coil图片加载
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor)

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

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        // 启用 expect/actual 类支持（Beta 特性）
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

android {
    namespace = "com.projectu"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.projectu"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = libs.versions.appVersionCode.get().toInt()
        versionName = libs.versions.appVersion.get()
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

                // 启用所有签名版本（向后兼容 + 新特性支持）
                enableV1Signing = true   // Android 7.0+ (API 24+) - 向后兼容
                enableV2Signing = true   // Android 7.0+ (API 24+) - 完整性保护
                enableV3Signing = true   // Android 9.0+ (API 28+) - 密钥轮换支持
                enableV4Signing = true   // Android 11+ (API 30+) - 增量更新优化

                println("✓ Using release signing config from keystore.properties")
            } else if (hasEnvironmentConfig) {
                // 从环境变量读取（用于 GitHub Actions）
                storeFile = file(System.getenv("RELEASE_KEYSTORE_FILE") ?: "")
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: ""
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: ""

                // 启用所有签名版本（向后兼容 + 新特性支持）
                enableV1Signing = true   // Android 7.0+ (API 24+) - 向后兼容
                enableV2Signing = true   // Android 7.0+ (API 24+) - 完整性保护
                enableV3Signing = true   // Android 9.0+ (API 28+) - 密钥轮换支持
                enableV4Signing = true   // Android 11+ (API 30+) - 增量更新优化

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
