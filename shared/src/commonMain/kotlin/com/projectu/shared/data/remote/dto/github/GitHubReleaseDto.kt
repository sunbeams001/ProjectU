package com.projectu.shared.data.remote.dto.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GitHub Release API 响应
 * 文档: https://docs.github.com/en/rest/releases/releases#get-the-latest-release
 */
@Serializable
data class GitHubReleaseDto(
    /**
     * 版本标签 (例如: "v1.0.11")
     */
    @SerialName("tag_name")
    val tagName: String,
    
    /**
     * Release 名称 (例如: "ProjectU v1.0.11")
     */
    @SerialName("name")
    val name: String,
    
    /**
     * 更新说明 (Markdown 格式)
     */
    @SerialName("body")
    val body: String,
    
    /**
     * 发布时间 (ISO 8601 格式)
     */
    @SerialName("published_at")
    val publishedAt: String,
    
    /**
     * 是否为预发布版本
     */
    @SerialName("prerelease")
    val prerelease: Boolean,
    
    /**
     * 附件列表 (APK、MSI 等下载文件)
     */
    @SerialName("assets")
    val assets: List<GitHubAssetDto>
)

/**
 * GitHub Release 附件
 */
@Serializable
data class GitHubAssetDto(
    /**
     * 文件名 (例如: "ProjectU-v1.0.11.apk")
     */
    @SerialName("name")
    val name: String,
    
    /**
     * 下载链接
     */
    @SerialName("browser_download_url")
    val downloadUrl: String,
    
    /**
     * 文件大小 (字节)
     */
    @SerialName("size")
    val size: Long
)
