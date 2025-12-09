package com.projectu.shared.domain.model

/**
 * 下载规则默认配置
 */
object DownloadRuleDefaults {
    /**
     * 默认基础路径（无自定义规则时使用）
     * Android: /storage/emulated/0/Pictures/ProjectU
     * Desktop: ~/ProjectU
     */
    const val DEFAULT_BASE_PATH_ANDROID = "/storage/emulated/0/Pictures/ProjectU"
    
    /**
     * 系统内置的默认规则组（当用户没有任何自定义规则时使用）
     * 
     * 创建 5 条规则，分别对应 5 种资源类型：
     * - 插画 → ProjectU/Illustrations
     * - 漫画 → ProjectU/Manga
     * - 动图 → ProjectU/Ugoira
     * - 小说 → ProjectU/Novels
     * - 小说系列 → ProjectU/NovelSeries
     * 
     * 行为：
     * - 不区分 R-18 / 非 R-18
     * - 不区分 AI / 非 AI
     * - 不按作者分组
     */
    fun getBuiltInRules(basePath: String = DEFAULT_BASE_PATH_ANDROID): List<DownloadRule> = listOf(
        DownloadRule(
            id = -1,
            order = 0,
            resourceTypes = setOf(ResourceType.ILLUSTRATION),
            r18Filter = FilterType.ANY,
            aiFilter = FilterType.ANY,
            authorGrouping = AuthorGrouping.NONE,
            targetPath = basePath,
            enabled = true,
            subDirectory = "Illustrations"
        ),
        DownloadRule(
            id = -2,
            order = 1,
            resourceTypes = setOf(ResourceType.MANGA),
            r18Filter = FilterType.ANY,
            aiFilter = FilterType.ANY,
            authorGrouping = AuthorGrouping.NONE,
            targetPath = basePath,
            enabled = true,
            subDirectory = "Manga"
        ),
        DownloadRule(
            id = -3,
            order = 2,
            resourceTypes = setOf(ResourceType.UGOIRA),
            r18Filter = FilterType.ANY,
            aiFilter = FilterType.ANY,
            authorGrouping = AuthorGrouping.NONE,
            targetPath = basePath,
            enabled = true,
            subDirectory = "Ugoira"
        ),
        DownloadRule(
            id = -4,
            order = 3,
            resourceTypes = setOf(ResourceType.NOVEL),
            r18Filter = FilterType.ANY,
            aiFilter = FilterType.ANY,
            authorGrouping = AuthorGrouping.NONE,
            targetPath = basePath,
            enabled = true,
            subDirectory = "Novels"
        ),
        DownloadRule(
            id = -5,
            order = 4,
            resourceTypes = setOf(ResourceType.NOVEL_SERIES),
            r18Filter = FilterType.ANY,
            aiFilter = FilterType.ANY,
            authorGrouping = AuthorGrouping.NONE,
            targetPath = basePath,
            enabled = true,
            subDirectory = "NovelSeries"
        )
    )
}
