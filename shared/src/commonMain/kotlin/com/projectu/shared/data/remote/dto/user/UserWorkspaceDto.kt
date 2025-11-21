package com.projectu.shared.data.remote.dto.user

import kotlinx.serialization.Serializable

@Serializable
data class UserWorkspace(
    val userWorkspacePc: String? = null,
    val userWorkspaceMonitor: String? = null,
    val userWorkspaceTool: String? = null,
    val userWorkspaceScanner: String? = null,
    val userWorkspaceTablet: String? = null,
    val userWorkspaceMouse: String? = null,
    val userWorkspacePrinter: String? = null,
    val userWorkspaceDesktop: String? = null,
    val userWorkspaceMusic: String? = null,
    val userWorkspaceDesk: String? = null,
    val userWorkspaceChair: String? = null,
    val userWorkspaceComment: String? = null,
    val userWorkspaceImageUrl: String? = null
)
