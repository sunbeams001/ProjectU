package com.projectu.shared.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.projectu.shared.domain.model.AuthorGrouping
import com.projectu.shared.domain.model.DownloadRule
import com.projectu.shared.domain.model.FilterType
import com.projectu.shared.domain.model.ResourceTypeFilter

/**
 * 下载规则实体（数据库表）
 */
@Entity(
    tableName = "download_rules",
    indices = [Index(value = ["ruleOrder"])]
)
data class DownloadRuleEntity(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    
    /**
     * 规则优先级（值越小优先级越高）
     * 用于排序：ORDER BY ruleOrder ASC
     */
    val ruleOrder: Int,
    
    /**
     * 资源类型过滤器
     * ILLUSTRATION / MANGA / UGOIRA / NOVEL / NOVEL_SERIES / ANY
     */
    val resourceTypeFilter: String,
    
    /**
     * R-18 过滤器
     * MUST_BE / MUST_NOT_BE / ANY
     */
    val r18Filter: String,
    
    /**
     * AI 作品过滤器
     * MUST_BE / MUST_NOT_BE / ANY
     */
    val aiFilter: String,
    
    /**
     * 作者分组模式
     * BY_ID / BY_NAME / NONE
     */
    val authorGrouping: String,
    
    /**
     * 目标存储路径
     * - 可以是传统文件路径：/storage/emulated/0/Pictures/MyPixiv
     * - 也可以是 SAF URI：content://com.android.externalstorage.documents/...
     */
    val targetPath: String,
    
    /**
     * 资源类型子目录（仅内置规则使用）
     * 例如：Illustrations, Manga, Ugoira 等
     */
    val subDirectory: String = "",
    
    /**
     * 是否启用该规则
     * 允许用户临时禁用某条规则而不删除
     */
    val enabled: Boolean = true,
    
    /**
     * 规则创建时间
     */
    val createdAt: Long = System.currentTimeMillis(),
    
    /**
     * 规则更新时间
     */
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 实体 → 领域模型转换
 */
fun DownloadRuleEntity.toDownloadRule() = DownloadRule(
    id = id,
    order = ruleOrder,
    resourceTypeFilter = ResourceTypeFilter.valueOf(resourceTypeFilter),
    r18Filter = FilterType.valueOf(r18Filter),
    aiFilter = FilterType.valueOf(aiFilter),
    authorGrouping = AuthorGrouping.valueOf(authorGrouping),
    targetPath = targetPath,
    enabled = enabled,
    createdAt = createdAt,
    updatedAt = updatedAt,
    subDirectory = subDirectory
)

/**
 * 领域模型 → 实体转换
 */
fun DownloadRule.toEntity() = DownloadRuleEntity(
    id = id,
    ruleOrder = order,
    resourceTypeFilter = resourceTypeFilter.name,
    r18Filter = r18Filter.name,
    aiFilter = aiFilter.name,
    authorGrouping = authorGrouping.name,
    targetPath = targetPath,
    enabled = enabled,
    createdAt = createdAt,
    updatedAt = updatedAt,
    subDirectory = subDirectory
)
