package com.projectu.shared.data.remote.dto.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 用户工作环境 DTO
 * 
 * API 返回的字段名格式为 userWorkspaceXxx，图片 URL 为 wsUrl/wsBigUrl
 */
@Serializable
data class UserWorkspace(
    @SerialName("userWorkspacePc") val pc: String? = null,
    @SerialName("userWorkspaceMonitor") val monitor: String? = null,
    @SerialName("userWorkspaceTool") val tool: String? = null,
    @SerialName("userWorkspaceScanner") val scanner: String? = null,
    @SerialName("userWorkspaceTablet") val tablet: String? = null,
    @SerialName("userWorkspaceMouse") val mouse: String? = null,
    @SerialName("userWorkspacePrinter") val printer: String? = null,
    @SerialName("userWorkspaceDesktop") val desktop: String? = null,
    @SerialName("userWorkspaceMusic") val music: String? = null,
    @SerialName("userWorkspaceDesk") val desk: String? = null,
    @SerialName("userWorkspaceChair") val chair: String? = null,
    @SerialName("userWorkspaceComment") val comment: String? = null,
    @SerialName("wsUrl") val imageUrl: String? = null,
    @SerialName("wsBigUrl") val imageBigUrl: String? = null
)
