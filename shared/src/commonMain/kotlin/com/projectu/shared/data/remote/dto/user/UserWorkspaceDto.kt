package com.projectu.shared.data.remote.dto.user

import kotlinx.serialization.Serializable

@Serializable
data class UserWorkspace(
    val pc: String? = null,
    val monitor: String? = null,
    val tool: String? = null,
    val scanner: String? = null,
    val tablet: String? = null,
    val mouse: String? = null,
    val printer: String? = null,
    val desktop: String? = null,
    val music: String? = null,
    val desk: String? = null,
    val chair: String? = null,
    val comment: String? = null,
    val imageUrl: String? = null
)
