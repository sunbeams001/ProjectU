package com.projectu.shared.domain.model

/**
 * 插画卡片图片质量设置
 * 
 * 定义了不同质量级别的缩略图，按质量从低到高排序
 */
enum class ImageQuality(val displayNameKey: String) {
    /**
     * 250x250 方形缩略图（最低质量，加载最快）
     */
    SQUARE_MEDIUM("image_quality_square_medium"),
    
    /**
     * 360x360 缩略图（中等质量）
     */
    MEDIUM("image_quality_medium"),
    
    /**
     * 540x540 大缩略图（较高质量）
     */
    LARGE("image_quality_large"),
    
    /**
     * master1200 最大缩略图（最高质量，不含原图）
     */
    MASTER_1200("image_quality_master_1200");
    
    companion object {
        /**
         * 从名称获取枚举
         */
        fun fromName(name: String): ImageQuality {
            return values().find { it.name == name } ?: SQUARE_MEDIUM
        }
        
        /**
         * 获取所有质量级别，按质量从低到高排序
         */
        fun getAllSortedByQuality(): List<ImageQuality> {
            return listOf(SQUARE_MEDIUM, MEDIUM, LARGE, MASTER_1200)
        }
    }
}

/**
 * ImageUrls 扩展函数：根据质量偏好获取 URL
 * 
 * 逻辑：
 * 1. 首先尝试获取指定质量的 URL
 * 2. 如果不存在，向低质量顺延
 * 3. 如果低质量也不存在，向高质量顺延
 * 4. 确保总能返回一个可用的 URL
 */
fun ImageUrls.getUrlByQuality(preferredQuality: ImageQuality): String {
    // 根据质量级别获取对应的 URL
    val getUrl: (ImageQuality) -> String? = { quality ->
        when (quality) {
            ImageQuality.SQUARE_MEDIUM -> this.squareMedium
            ImageQuality.MEDIUM -> this.medium
            ImageQuality.LARGE -> this.large
            ImageQuality.MASTER_1200 -> this.master1200
        }
    }
    
    // 先尝试获取首选质量
    getUrl(preferredQuality)?.let { return it }
    
    // 获取所有质量级别
    val allQualities = ImageQuality.getAllSortedByQuality()
    val preferredIndex = allQualities.indexOf(preferredQuality)
    
    // 向低质量顺延
    for (i in (preferredIndex - 1) downTo 0) {
        getUrl(allQualities[i])?.let { return it }
    }
    
    // 向高质量顺延
    for (i in (preferredIndex + 1) until allQualities.size) {
        getUrl(allQualities[i])?.let { return it }
    }
    
    // 兜底：返回 squareMedium（必定存在）
    return this.squareMedium
}

/**
 * 插画详情页图片质量设置
 * 
 * 定义了详情页可用的图片质量级别，包括原图
 */
enum class DetailImageQuality(val displayNameKey: String) {
    /**
     * 250x250 方形缩略图
     */
    SQUARE_MEDIUM("detail_image_quality_square_medium"),
    
    /**
     * 360x360 缩略图
     */
    MEDIUM("detail_image_quality_medium"),
    
    /**
     * 540x540 大缩略图
     */
    LARGE("detail_image_quality_large"),
    
    /**
     * master1200 最大缩略图
     */
    MASTER_1200("detail_image_quality_master_1200"),
    
    /**
     * 原图（最高质量）
     */
    ORIGINAL("detail_image_quality_original");
    
    companion object {
        /**
         * 从名称获取枚举
         */
        fun fromName(name: String): DetailImageQuality {
            return values().find { it.name == name } ?: LARGE
        }
        
        /**
         * 获取所有质量级别，按质量从低到高排序
         */
        fun getAllSortedByQuality(): List<DetailImageQuality> {
            return listOf(SQUARE_MEDIUM, MEDIUM, LARGE, MASTER_1200, ORIGINAL)
        }
    }
}

/**
 * PageImageUrls 扩展函数：根据详情页质量偏好获取 URL
 */
fun PageImageUrls.getUrlByQuality(preferredQuality: DetailImageQuality): String {
    val getUrl: (DetailImageQuality) -> String? = { quality ->
        when (quality) {
            DetailImageQuality.SQUARE_MEDIUM -> this.urls.squareMedium
            DetailImageQuality.MEDIUM -> this.urls.medium
            DetailImageQuality.LARGE -> this.urls.large
            DetailImageQuality.MASTER_1200 -> this.urls.master1200
            DetailImageQuality.ORIGINAL -> this.urls.original
        }
    }
    
    // 先尝试获取首选质量
    getUrl(preferredQuality)?.let { return it }
    
    // 获取所有质量级别
    val allQualities = DetailImageQuality.getAllSortedByQuality()
    val preferredIndex = allQualities.indexOf(preferredQuality)
    
    // 向低质量顺延
    for (i in (preferredIndex - 1) downTo 0) {
        getUrl(allQualities[i])?.let { return it }
    }
    
    // 向高质量顺延
    for (i in (preferredIndex + 1) until allQualities.size) {
        getUrl(allQualities[i])?.let { return it }
    }
    
    // 兜底：返回 squareMedium（必定存在）
    return this.urls.squareMedium
}
