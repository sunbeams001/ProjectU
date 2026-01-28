package com.projectu.shared.data.repository

import com.projectu.shared.data.remote.api.GitHubApi
import com.projectu.shared.domain.model.UpdateCheckResult
import com.projectu.shared.domain.model.UpdateInfo
import com.projectu.shared.domain.repository.UpdateRepository

/**
 * 更新检查仓储实现
 */
class UpdateRepositoryImpl(
    private val gitHubApi: GitHubApi
) : UpdateRepository {
    
    override suspend fun checkForUpdate(
        currentVersionName: String,
        currentVersionCode: Int
    ): UpdateCheckResult {
        return try {
            val release = gitHubApi.getLatestRelease()
            
            // 跳过预发布版本
            if (release.prerelease) {
                return UpdateCheckResult.NoUpdate
            }
            
            // 解析版本号 (去掉 "v" 前缀)
            val remoteVersionName = release.tagName.removePrefix("v")
            
            // 比较版本号
            val comparison = compareVersions(currentVersionName, remoteVersionName)
            
            if (comparison < 0) {
                // 需要更新，查找对应平台的下载文件
                val asset = findAssetForCurrentPlatform(release.assets)
                
                if (asset != null) {
                    // 尝试从文件名提取 versionCode (例如: ProjectU-v1.0.11.apk)
                    val remoteVersionCode = extractVersionCode(remoteVersionName)
                    
                    val updateInfo = UpdateInfo(
                        versionName = remoteVersionName,
                        versionCode = remoteVersionCode,
                        releaseNotes = release.body,
                        downloadUrl = asset.downloadUrl,
                        fileSize = asset.size,
                        publishedAt = release.publishedAt
                    )
                    UpdateCheckResult.HasUpdate(updateInfo)
                } else {
                    UpdateCheckResult.Error("No compatible download file found for current platform")
                }
            } else {
                UpdateCheckResult.NoUpdate
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Unknown error occurred")
        }
    }
    
    /**
     * 比较版本号
     * @return -1: local < remote, 0: local == remote, 1: local > remote
     */
    private fun compareVersions(local: String, remote: String): Int {
        val localParts = local.split(".").map { it.toIntOrNull() ?: 0 }
        val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
        
        val maxLength = maxOf(localParts.size, remoteParts.size)
        
        for (i in 0 until maxLength) {
            val localPart = localParts.getOrNull(i) ?: 0
            val remotePart = remoteParts.getOrNull(i) ?: 0
            
            if (localPart < remotePart) return -1
            if (localPart > remotePart) return 1
        }
        
        return 0
    }
    
    /**
     * 从版本字符串提取版本代码
     * 例如: "1.0.11" -> 1*10000 + 0*100 + 11 = 10011
     */
    private fun extractVersionCode(versionName: String): Int {
        val parts = versionName.split(".").mapNotNull { it.toIntOrNull() }
        return when (parts.size) {
            3 -> parts[0] * 10000 + parts[1] * 100 + parts[2]
            2 -> parts[0] * 10000 + parts[1] * 100
            1 -> parts[0] * 10000
            else -> 0
        }
    }
    
    /**
     * 查找当前平台的下载文件
     */
    private fun findAssetForCurrentPlatform(assets: List<com.projectu.shared.data.remote.dto.github.GitHubAssetDto>):
            com.projectu.shared.data.remote.dto.github.GitHubAssetDto? {
        // 获取平台标识
        val platform = getCurrentPlatform()
        
        return when (platform) {
            Platform.ANDROID -> assets.find { it.name.endsWith(".apk", ignoreCase = true) }
            Platform.WINDOWS -> assets.find { 
                it.name.endsWith(".msi", ignoreCase = true) || 
                it.name.contains("windows", ignoreCase = true) && it.name.endsWith(".zip", ignoreCase = true)
            }
            Platform.MACOS -> assets.find { it.name.endsWith(".dmg", ignoreCase = true) }
            Platform.LINUX -> assets.find { 
                it.name.endsWith(".deb", ignoreCase = true) || 
                it.name.endsWith(".AppImage", ignoreCase = true)
            }
            Platform.UNKNOWN -> null
        }
    }
    
    /**
     * 获取当前平台
     */
    private fun getCurrentPlatform(): Platform {
        return try {
            // 使用 expect/actual 模式获取平台
            getPlatform()
        } catch (e: Exception) {
            Platform.UNKNOWN
        }
    }
}

/**
 * 平台枚举
 */
enum class Platform {
    ANDROID, WINDOWS, MACOS, LINUX, UNKNOWN
}

/**
 * 获取当前平台 (需要在各平台实现)
 */
expect fun getPlatform(): Platform
