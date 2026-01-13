package com.projectu.shared.domain.model

/**
 * Widget 配置领域模型
 */
data class WidgetConfig(
    /**
     * Widget ID（由系统分配）
     */
    val widgetId: Int,
    
    /**
     * 数据来源类型
     */
    val dataSource: WidgetDataSource,
    
    /**
     * 排行榜模式（仅当 dataSource 为 RANKING 时有效）
     */
    val rankingMode: WidgetRankingMode? = null,
    
    /**
     * R-18 过滤
     */
    val r18Filter: FilterType,
    
    /**
     * AI 生成过滤
     */
    val aiFilter: FilterType,
    
    /**
     * 更新间隔（分钟）
     */
    val updateIntervalMinutes: Int,
    
    /**
     * 是否显示刷新按钮
     */
    val showRefreshButton: Boolean,
    
    /**
     * 图片缩放方式
     */
    val imageScaleType: WidgetImageScaleType = WidgetImageScaleType.FIT_CENTER,
    
    /**
     * 当前显示的作品 ID
     */
    val currentArtworkId: String? = null,
    
    /**
     * 当前显示的作品索引
     */
    val currentIndex: Int = 0,
    
    /**
     * 创建时间
     */
    val createdAt: Long = System.currentTimeMillis(),
    
    /**
     * 最后更新时间
     */
    val lastUpdatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * 最小更新间隔（分钟）
         */
        const val MIN_UPDATE_INTERVAL = 5
        
        /**
         * 默认更新间隔（分钟）
         */
        const val DEFAULT_UPDATE_INTERVAL = 60
    }
}

/**
 * Widget 数据来源类型
 */
enum class WidgetDataSource {
    /**
     * 推荐插画/漫画
     */
    RECOMMENDED,
    
    /**
     * 动态插画/漫画（关注用户的最新作品）
     */
    FOLLOWING_LATEST,
    
    /**
     * 插画排行榜
     */
    RANKING
}

/**
 * Widget 专用排行榜模式
 * 注意：这是Widget专用的，区别于现有的RankingMode
 */
enum class WidgetRankingMode {
    /**
     * 今日排行榜
     */
    DAY,
    
    /**
     * 本周排行榜
     */
    WEEK,
    
    /**
     * 本月排行榜
     */
    MONTH
}

/**
 * Widget 图片缩放方式
 */
enum class WidgetImageScaleType {
    /**
     * 完整显示（FitCenter）
     * 按比例缩放，完整显示所有内容，可能有留白
     */
    FIT_CENTER,
    
    /**
     * 填充裁剪（CenterCrop）
     * 按比例缩放并裁剪，填满整个视图，可能裁剪部分内容
     */
    CENTER_CROP
}
