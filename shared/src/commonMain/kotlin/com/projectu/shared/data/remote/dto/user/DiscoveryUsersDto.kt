package com.projectu.shared.data.remote.dto.user

import kotlinx.serialization.Serializable

/**
 * 发现用户响应体
 * 用于 `/ajax/discovery/users` 接口
 */
@Serializable
data class DiscoveryUsersBody(
    /** 标签翻译字典 */
    val tagTranslation: Map<String, TagTranslation>,
    /** 缩略图信息 */
    val thumbnails: DiscoveryThumbnails,
    /** 推荐用户列表 */
    val users: List<DiscoveryUserInfo>,
    /** 推荐用户ID列表 */
    val recommendedUsers: List<RecommendedUserEntry>
)

/**
 * 标签翻译信息
 */
@Serializable
data class TagTranslation(
    /** 英文翻译 */
    val en: String? = null,
    /** 韩文翻译 */
    val ko: String? = null,
    /** 简体中文翻译 */
    val zh: String? = null,
    /** 繁体中文翻译 */
    val zh_tw: String? = null,
    /** 罗马音 */
    val romaji: String? = null
)

/**
 * 缩略图信息
 */
@Serializable
data class DiscoveryThumbnails(
    /** 插画缩略图列表 */
    val illust: List<IllustThumbnail>,
    /** 小说缩略图列表 */
    val novel: List<NovelThumbnailInfo>
)

/**
 * 插画缩略图信息
 */
@Serializable
data class IllustThumbnail(
    /** 作品ID */
    val id: String,
    /** 标题 */
    val title: String,
    /** 插画类型 (0=插画, 1=漫画, 2=动图) */
    val illustType: Int,
    /** 年龄限制 (0=全年龄, 1=R-18, 2=R-18G) */
    val xRestrict: Int,
    /** 限制级别 */
    val restrict: Int,
    /** 敏感级别 */
    val sl: Int,
    /** 缩略图URL */
    val url: String,
    /** 描述 */
    val description: String,
    /** 标签列表 */
    val tags: List<String>,
    /** 用户ID */
    val userId: String,
    /** 用户名 */
    val userName: String,
    /** 宽度 */
    val width: Int,
    /** 高度 */
    val height: Int,
    /** 页数 */
    val pageCount: Int,
    /** 是否可收藏 */
    val isBookmarkable: Boolean,
    /** 收藏数据 */
    val bookmarkData: String? = null,
    /** 替代文本 */
    val alt: String,
    /** 标题和说明翻译 */
    val titleCaptionTranslation: TitleCaptionTranslation,
    /** 创建日期 */
    val createDate: String,
    /** 更新日期 */
    val updateDate: String,
    /** 是否未列出 */
    val isUnlisted: Boolean,
    /** 是否被屏蔽 */
    val isMasked: Boolean,
    /** AI类型 (0=非AI, 1=AI生成, 2=AI辅助) */
    val aiType: Int,
    /** 可见范围 */
    val visibilityScope: Int,
    /** 各尺寸URL */
    val urls: ThumbnailUrls,
    /** 用户头像URL */
    val profileImageUrl: String,
    /** 系列ID (可选) */
    val seriesId: String? = null,
    /** 系列标题 (可选) */
    val seriesTitle: String? = null
)

/**
 * 标题说明翻译
 */
@Serializable
data class TitleCaptionTranslation(
    /** 作品标题 */
    val workTitle: String? = null,
    /** 作品说明 */
    val workCaption: String? = null
)

/**
 * 缩略图URL集合
 */
@Serializable
data class ThumbnailUrls(
    /** 250x250 */
    @kotlinx.serialization.SerialName("250x250")
    val size250x250: String,
    /** 360x360 */
    @kotlinx.serialization.SerialName("360x360")
    val size360x360: String,
    /** 540x540 */
    @kotlinx.serialization.SerialName("540x540")
    val size540x540: String,
    /** 1200x1200 */
    @kotlinx.serialization.SerialName("1200x1200")
    val size1200x1200: String
)

/**
 * 小说缩略图信息
 */
@Serializable
data class NovelThumbnailInfo(
    /** 小说ID */
    val id: String,
    /** 标题 */
    val title: String,
    /** 年龄限制 */
    val xRestrict: Int,
    /** 限制级别 */
    val restrict: Int,
    /** 缩略图URL */
    val url: String,
    /** 标签列表 */
    val tags: List<String>,
    /** 用户ID */
    val userId: String,
    /** 用户名 */
    val userName: String,
    /** 用户头像URL */
    val profileImageUrl: String,
    /** 文字数 */
    val textCount: Int,
    /** 描述 */
    val description: String,
    /** 是否可收藏 */
    val isBookmarkable: Boolean,
    /** 收藏数据 */
    val bookmarkData: String? = null,
    /** 是否原创 */
    val isOriginal: Boolean,
    /** 标记 */
    val marker: String? = null,
    /** 标题和说明翻译 */
    val titleCaptionTranslation: TitleCaptionTranslation,
    /** 创建日期 */
    val createDate: String,
    /** 更新日期 */
    val updateDate: String,
    /** 是否未列出 */
    val isUnlisted: Boolean,
    /** AI类型 */
    val aiType: Int
)

/**
 * 发现页推荐用户信息
 */
@Serializable
data class DiscoveryUserInfo(
    /** 部分信息标记 */
    val partial: Int,
    /** 评论/简介 */
    val comment: String,
    /** 是否被对方关注 */
    val followedBack: Boolean,
    /** 用户ID */
    val userId: String,
    /** 用户名 */
    val name: String,
    /** 头像URL (小) */
    val image: String,
    /** 头像URL (大) */
    val imageBig: String,
    /** 是否为高级会员 */
    val premium: Boolean,
    /** 是否已关注 */
    val isFollowed: Boolean,
    /** 是否为好P友 */
    val isMypixiv: Boolean,
    /** 是否已屏蔽 */
    val isBlocking: Boolean,
    /** 背景图 */
    val background: String? = null,
    /** 约稿信息 */
    val commission: CommissionInfo? = null
)

/**
 * 约稿信息
 */
@Serializable
data class CommissionInfo(
    /** 是否接受约稿 */
    val acceptRequest: Boolean,
    /** 是否为订阅用户 */
    val isSubscribed: Boolean? = null
)

/**
 * 推荐用户条目
 */
@Serializable
data class RecommendedUserEntry(
    /** 用户ID */
    val userId: String,
    /** 最近插画ID列表 */
    val recentIllustIds: List<String>,
    /** 最近小说ID列表 */
    val recentNovelIds: List<String>
)
