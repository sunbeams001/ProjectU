package com.projectu.shared.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 作品数据库实体
 */
@Entity(tableName = "artworks")
data class ArtworkEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val imageUrlsJson: String,
    val userJson: String,
    val tagsJson: String,
    val createDate: Long,
    val pageCount: Int,
    val width: Int,
    val height: Int,
    val viewCount: Int,
    val bookmarkCount: Int,
    val likeCount: Int,
    val commentCount: Int,
    val bookmarkStatus: String, // BookmarkStatus 的 name
    val bookmarkId: String?,
    val type: String,
    val ageLimit: String,
    val cachedAt: Long
)

/**
 * Ugoira缓存实体
 */
@Entity(tableName = "ugoira_cache")
data class UgoiraCacheEntity(
    @PrimaryKey
    val artworkId: String,
    val zipPath: String,
    val framesPath: String,
    val metadataJson: String,
    val cachedAt: Long,
    val lastAccessedAt: Long
)

