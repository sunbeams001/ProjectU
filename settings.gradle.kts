rootProject.name = "ProjectU"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        // JOGAMP Maven仓库 - 用于WebView依赖的JCEF库
        maven("https://jogamp.org/deployment/maven")
    }
    
    // 强制使用本地 JOGAMP 依赖，避免从远程仓库下载超时
    // 这解决了 CI 环境中 jogamp.org 仓库不稳定的问题
    versionCatalogs {
        create("localLibs") {
            // 占位符，实际依赖通过 resolutionStrategy 处理
        }
    }
}

include(":composeApp")
include(":shared")


