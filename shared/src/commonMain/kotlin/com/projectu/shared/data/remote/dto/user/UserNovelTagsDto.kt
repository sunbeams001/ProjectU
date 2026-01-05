package com.projectu.shared.data.remote.dto.user

/**
 * 用户小说标签列表响应体
 * 
 * 接口地址: GET /ajax/user/{userId}/novels/tags?all=1&lang=zh
 * 示例: https://www.pixiv.net/ajax/user/16208053/novels/tags?all=1&lang=zh
 * 
 * 用途：获取该用户所有小说的标签列表（按作品数量统计）
 * 
 * 注意：API 返回的 body 字段直接是标签数组，标签结构与插画标签相同
 */
typealias UserNovelTagsBody = List<UserIllustTag>
