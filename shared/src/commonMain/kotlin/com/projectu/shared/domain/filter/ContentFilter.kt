package com.projectu.shared.domain.filter

import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.ArtworkType
import com.projectu.shared.domain.model.BlockRule
import com.projectu.shared.domain.model.BlockRuleType
import com.projectu.shared.domain.model.ContentScope
import com.projectu.shared.domain.model.MangaSeries
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.TagMatchMode

/**
 * 内容过滤器
 * 
 * 根据启用的屏蔽规则过滤作品和小说列表
 */
class ContentFilter(
    private val enabledRules: List<BlockRule>
) {
    
    /**
     * 过滤作品列表
     * 
     * @param artworks 待过滤的作品列表
     * @return 过滤后的作品列表
     */
    fun filter(artworks: List<Artwork>): List<Artwork> {
        if (enabledRules.isEmpty()) {
            return artworks
        }
        
        return artworks.filterNot { artwork ->
            shouldBlockArtwork(artwork)
        }
    }
    
    /**
     * 过滤小说列表
     * 
     * @param novels 待过滤的小说列表
     * @return 过滤后的小说列表
     */
    fun filterNovels(novels: List<Novel>): List<Novel> {
        if (enabledRules.isEmpty()) {
            return novels
        }
        
        return novels.filterNot { novel ->
            shouldBlockNovel(novel)
        }
    }
    
    /**
     * 判断是否应该屏蔽某个作品
     * 
     * @param artwork 作品
     * @return true 表示应该屏蔽，false 表示不应该屏蔽
     */
    private fun shouldBlockArtwork(artwork: Artwork): Boolean {
        // 确定作品的内容范围
        val artworkScope = when (artwork.type) {
            ArtworkType.MANGA -> ContentScope.MANGA
            ArtworkType.UGOIRA -> ContentScope.UGOIRA
            ArtworkType.ILLUSTRATION -> ContentScope.ILLUST
        }
        
        return enabledRules.any { rule ->
            // 检查规则是否适用于此作品类型
            if (!rule.scopes.contains(artworkScope)) {
                return@any false
            }
            
            when (rule.type) {
                BlockRuleType.R18_CONTENT -> isR18Content(artwork.ageLimit)
                BlockRuleType.AI_GENERATED -> artwork.isAiGenerated
                BlockRuleType.AUTHOR_ID -> artwork.userId == rule.value
                BlockRuleType.TAG -> matchesTag(
                    tags = artwork.tags.map { it.name to it.translatedName },
                    blockedTag = rule.value,
                    matchMode = rule.matchMode
                )
            }
        }
    }
    
    /**
     * 判断是否应该屏蔽某个小说
     * 
     * @param novel 小说
     * @return true 表示应该屏蔽，false 表示不应该屏蔽
     */
    private fun shouldBlockNovel(novel: Novel): Boolean {
        return enabledRules.any { rule ->
            // 检查规则是否适用于小说
            if (!rule.scopes.contains(ContentScope.NOVEL)) {
                return@any false
            }
            
            when (rule.type) {
                BlockRuleType.R18_CONTENT -> isR18Content(novel.ageLimit)
                BlockRuleType.AI_GENERATED -> novel.isAiGenerated
                BlockRuleType.AUTHOR_ID -> novel.userId == rule.value
                BlockRuleType.TAG -> matchesTag(
                    tags = novel.tags.map { it.name to it.translatedName },
                    blockedTag = rule.value,
                    matchMode = rule.matchMode
                )
            }
        }
    }
    
    /**
     * 过滤漫画系列列表
     * 
     * @param series 原始漫画系列列表
     * @return 过滤后的漫画系列列表
     */
    fun filterMangaSeries(series: List<MangaSeries>): List<MangaSeries> {
        return series.filterNot { shouldBlockMangaSeries(it) }
    }
    
    /**
     * 判断是否应该屏蔽某个漫画系列
     * 
     * @param series 漫画系列
     * @return true 表示应该屏蔽，false 表示不应该屏蔽
     */
    private fun shouldBlockMangaSeries(series: MangaSeries): Boolean {
        return enabledRules.any { rule ->
            // 检查规则是否适用于漫画系列
            if (!rule.scopes.contains(ContentScope.MANGA_SERIES)) {
                return@any false
            }
            
            when (rule.type) {
                // 漫画系列没有 ageLimit 和 tags 字段，只支持作者屏蔽
                // AI 生成标识也不适用于系列（系列是作品集合，不是单个作品）
                BlockRuleType.AUTHOR_ID -> series.userId == rule.value
                else -> false  // R18_CONTENT, AI_GENERATED, TAG 不适用
            }
        }
    }
    
    /**
     * 判断是否为 R-18 内容
     * 
     * 检查年龄限制等级
     */
    private fun isR18Content(ageLimit: com.projectu.shared.domain.model.AgeLimit): Boolean {
        return ageLimit == com.projectu.shared.domain.model.AgeLimit.R18 ||
               ageLimit == com.projectu.shared.domain.model.AgeLimit.R18G
    }
    
    /**
     * 判断标签是否匹配屏蔽规则
     * 
     * @param tags 标签列表（名称和翻译名称的配对）
     * @param blockedTag 被屏蔽的标签规则（可能是精确值或正则表达式）
     * @param matchMode 匹配模式
     * @return true 表示匹配（应该屏蔽），false 表示不匹配
     */
    private fun matchesTag(
        tags: List<Pair<String, String?>>,
        blockedTag: String,
        matchMode: TagMatchMode
    ): Boolean {
        return when (matchMode) {
            TagMatchMode.EXACT -> {
                // 精确匹配：标签名或翻译名完全相同（忽略大小写）
                tags.any { (name, translatedName) ->
                    name.equals(blockedTag, ignoreCase = true) ||
                    translatedName?.equals(blockedTag, ignoreCase = true) == true
                }
            }
            TagMatchMode.REGEX -> {
                // 正则表达式匹配
                try {
                    val regex = Regex(blockedTag, RegexOption.IGNORE_CASE)
                    tags.any { (name, translatedName) ->
                        regex.matches(name) ||
                        (translatedName != null && regex.matches(translatedName))
                    }
                } catch (e: Exception) {
                    // 如果正则表达式无效，回退到精确匹配
                    tags.any { (name, translatedName) ->
                        name.equals(blockedTag, ignoreCase = true) ||
                        translatedName?.equals(blockedTag, ignoreCase = true) == true
                    }
                }
            }
        }
    }
}
