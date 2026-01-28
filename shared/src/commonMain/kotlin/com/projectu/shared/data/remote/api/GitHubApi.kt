package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.github.GitHubReleaseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header

/**
 * GitHub API 客户端
 * 用于检查 ProjectU 的更新
 */
class GitHubApi(private val httpClient: HttpClient) {
    
    companion object {
        private const val GITHUB_API_BASE = "https://api.github.com"
        private const val REPO_OWNER = "sunbeams001"
        private const val REPO_NAME = "ProjectU"
    }
    
    /**
     * 获取最新的 Release 信息
     * 
     * @return GitHubReleaseDto 最新版本信息
     * @throws Exception 网络错误或解析错误
     */
    suspend fun getLatestRelease(): GitHubReleaseDto {
        val response = httpClient.get("$GITHUB_API_BASE/repos/$REPO_OWNER/$REPO_NAME/releases/latest") {
            header("Accept", "application/vnd.github.v3+json")
            header("User-Agent", "ProjectU-App")
        }
        return response.body()
    }
}
