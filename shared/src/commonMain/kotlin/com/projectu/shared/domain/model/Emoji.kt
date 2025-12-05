package com.projectu.shared.domain.model

/**
 * Emoji 表情（可在评论文本中混排）
 * @param label 表情标签，如 "(normal)"
 * @param url 表情图片URL
 */
data class Emoji(
    val label: String,
    val url: String
)

/**
 * Stamp 贴图（独立发送，不能与文字混排）
 * @param id 贴图ID，用于API提交
 * @param url 贴图预览图URL
 */
data class Stamp(
    val id: Int,
    val url: String
)

/**
 * 表情配置数据
 */
object EmojiConfig {
    
    /**
     * Emoji 列表
     */
    val emojis: List<Emoji> = listOf(
        Emoji("(normal)", "https://s.pximg.net/common/images/emoji/101.png"),
        Emoji("(surprise)", "https://s.pximg.net/common/images/emoji/102.png"),
        Emoji("(serious)", "https://s.pximg.net/common/images/emoji/103.png"),
        Emoji("(heaven)", "https://s.pximg.net/common/images/emoji/104.png"),
        Emoji("(happy)", "https://s.pximg.net/common/images/emoji/105.png"),
        Emoji("(excited)", "https://s.pximg.net/common/images/emoji/106.png"),
        Emoji("(sing)", "https://s.pximg.net/common/images/emoji/107.png"),
        Emoji("(cry)", "https://s.pximg.net/common/images/emoji/108.png"),
        Emoji("(normal2)", "https://s.pximg.net/common/images/emoji/201.png"),
        Emoji("(shame2)", "https://s.pximg.net/common/images/emoji/202.png"),
        Emoji("(love2)", "https://s.pximg.net/common/images/emoji/203.png"),
        Emoji("(interesting2)", "https://s.pximg.net/common/images/emoji/204.png"),
        Emoji("(blush2)", "https://s.pximg.net/common/images/emoji/205.png"),
        Emoji("(fire2)", "https://s.pximg.net/common/images/emoji/206.png"),
        Emoji("(angry2)", "https://s.pximg.net/common/images/emoji/207.png"),
        Emoji("(shine2)", "https://s.pximg.net/common/images/emoji/208.png"),
        Emoji("(panic2)", "https://s.pximg.net/common/images/emoji/209.png"),
        Emoji("(normal3)", "https://s.pximg.net/common/images/emoji/301.png"),
        Emoji("(satisfaction3)", "https://s.pximg.net/common/images/emoji/302.png"),
        Emoji("(surprise3)", "https://s.pximg.net/common/images/emoji/303.png"),
        Emoji("(smile3)", "https://s.pximg.net/common/images/emoji/304.png"),
        Emoji("(shock3)", "https://s.pximg.net/common/images/emoji/305.png"),
        Emoji("(gaze3)", "https://s.pximg.net/common/images/emoji/306.png"),
        Emoji("(wink3)", "https://s.pximg.net/common/images/emoji/307.png"),
        Emoji("(happy3)", "https://s.pximg.net/common/images/emoji/308.png"),
        Emoji("(excited3)", "https://s.pximg.net/common/images/emoji/309.png"),
        Emoji("(love3)", "https://s.pximg.net/common/images/emoji/310.png"),
        Emoji("(normal4)", "https://s.pximg.net/common/images/emoji/401.png"),
        Emoji("(surprise4)", "https://s.pximg.net/common/images/emoji/402.png"),
        Emoji("(serious4)", "https://s.pximg.net/common/images/emoji/403.png"),
        Emoji("(love4)", "https://s.pximg.net/common/images/emoji/404.png"),
        Emoji("(shine4)", "https://s.pximg.net/common/images/emoji/405.png"),
        Emoji("(sweat4)", "https://s.pximg.net/common/images/emoji/406.png"),
        Emoji("(shame4)", "https://s.pximg.net/common/images/emoji/407.png"),
        Emoji("(sleep4)", "https://s.pximg.net/common/images/emoji/408.png"),
        Emoji("(heart)", "https://s.pximg.net/common/images/emoji/501.png"),
        Emoji("(teardrop)", "https://s.pximg.net/common/images/emoji/502.png"),
        Emoji("(star)", "https://s.pximg.net/common/images/emoji/503.png")
    )
    
    /**
     * Stamp 贴图列表
     */
    val stamps: List<Stamp> = listOf(
        Stamp(301, "https://s.pximg.net/common/images/stamp/generated-stamps/301_s.jpg?20180605"),
        Stamp(302, "https://s.pximg.net/common/images/stamp/generated-stamps/302_s.jpg?20180605"),
        Stamp(303, "https://s.pximg.net/common/images/stamp/generated-stamps/303_s.jpg?20180605"),
        Stamp(304, "https://s.pximg.net/common/images/stamp/generated-stamps/304_s.jpg?20180605"),
        Stamp(305, "https://s.pximg.net/common/images/stamp/generated-stamps/305_s.jpg?20180605"),
        Stamp(306, "https://s.pximg.net/common/images/stamp/generated-stamps/306_s.jpg?20180605"),
        Stamp(307, "https://s.pximg.net/common/images/stamp/generated-stamps/307_s.jpg?20180605"),
        Stamp(308, "https://s.pximg.net/common/images/stamp/generated-stamps/308_s.jpg?20180605"),
        Stamp(309, "https://s.pximg.net/common/images/stamp/generated-stamps/309_s.jpg?20180605"),
        Stamp(310, "https://s.pximg.net/common/images/stamp/generated-stamps/310_s.jpg?20180605"),
        Stamp(401, "https://s.pximg.net/common/images/stamp/generated-stamps/401_s.jpg?20180605"),
        Stamp(402, "https://s.pximg.net/common/images/stamp/generated-stamps/402_s.jpg?20180605"),
        Stamp(403, "https://s.pximg.net/common/images/stamp/generated-stamps/403_s.jpg?20180605"),
        Stamp(404, "https://s.pximg.net/common/images/stamp/generated-stamps/404_s.jpg?20180605"),
        Stamp(405, "https://s.pximg.net/common/images/stamp/generated-stamps/405_s.jpg?20180605"),
        Stamp(406, "https://s.pximg.net/common/images/stamp/generated-stamps/406_s.jpg?20180605"),
        Stamp(407, "https://s.pximg.net/common/images/stamp/generated-stamps/407_s.jpg?20180605"),
        Stamp(408, "https://s.pximg.net/common/images/stamp/generated-stamps/408_s.jpg?20180605"),
        Stamp(409, "https://s.pximg.net/common/images/stamp/generated-stamps/409_s.jpg?20180605"),
        Stamp(410, "https://s.pximg.net/common/images/stamp/generated-stamps/410_s.jpg?20180605"),
        Stamp(201, "https://s.pximg.net/common/images/stamp/generated-stamps/201_s.jpg?20180605"),
        Stamp(202, "https://s.pximg.net/common/images/stamp/generated-stamps/202_s.jpg?20180605"),
        Stamp(203, "https://s.pximg.net/common/images/stamp/generated-stamps/203_s.jpg?20180605"),
        Stamp(204, "https://s.pximg.net/common/images/stamp/generated-stamps/204_s.jpg?20180605"),
        Stamp(205, "https://s.pximg.net/common/images/stamp/generated-stamps/205_s.jpg?20180605"),
        Stamp(206, "https://s.pximg.net/common/images/stamp/generated-stamps/206_s.jpg?20180605"),
        Stamp(207, "https://s.pximg.net/common/images/stamp/generated-stamps/207_s.jpg?20180605"),
        Stamp(208, "https://s.pximg.net/common/images/stamp/generated-stamps/208_s.jpg?20180605"),
        Stamp(209, "https://s.pximg.net/common/images/stamp/generated-stamps/209_s.jpg?20180605"),
        Stamp(210, "https://s.pximg.net/common/images/stamp/generated-stamps/210_s.jpg?20180605"),
        Stamp(101, "https://s.pximg.net/common/images/stamp/generated-stamps/101_s.jpg?20180605"),
        Stamp(102, "https://s.pximg.net/common/images/stamp/generated-stamps/102_s.jpg?20180605"),
        Stamp(103, "https://s.pximg.net/common/images/stamp/generated-stamps/103_s.jpg?20180605"),
        Stamp(104, "https://s.pximg.net/common/images/stamp/generated-stamps/104_s.jpg?20180605"),
        Stamp(105, "https://s.pximg.net/common/images/stamp/generated-stamps/105_s.jpg?20180605"),
        Stamp(106, "https://s.pximg.net/common/images/stamp/generated-stamps/106_s.jpg?20180605"),
        Stamp(107, "https://s.pximg.net/common/images/stamp/generated-stamps/107_s.jpg?20180605"),
        Stamp(108, "https://s.pximg.net/common/images/stamp/generated-stamps/108_s.jpg?20180605"),
        Stamp(109, "https://s.pximg.net/common/images/stamp/generated-stamps/109_s.jpg?20180605"),
        Stamp(110, "https://s.pximg.net/common/images/stamp/generated-stamps/110_s.jpg?20180605")
    )
    
    /**
     * 根据 label 获取 emoji URL
     */
    fun getEmojiUrl(label: String): String? {
        return emojis.find { it.label == label }?.url
    }
    
    /**
     * 根据 stamp ID 获取 stamp URL
     */
    fun getStampUrl(stampId: Int): String? {
        return stamps.find { it.id == stampId }?.url
    }
    
    /**
     * 从评论 stamp 链接中获取完整的 stamp URL
     * 评论中返回的可能是小图 xxx_s.jpg，这里转换为预览用的 URL
     */
    fun getStampUrlFromLink(stampLink: String): String {
        return stampLink
    }
    
    /**
     * emoji 标签的正则表达式
     */
    val emojiPattern = Regex("\\([a-zA-Z0-9]+\\)")
    
    /**
     * 解析文本中的 emoji 标签
     * @return 包含文本片段和 emoji 的列表
     */
    fun parseEmojiText(text: String): List<EmojiTextSegment> {
        val result = mutableListOf<EmojiTextSegment>()
        var lastIndex = 0
        
        emojiPattern.findAll(text).forEach { match ->
            // 添加 emoji 之前的文本
            if (match.range.first > lastIndex) {
                result.add(EmojiTextSegment.Text(text.substring(lastIndex, match.range.first)))
            }
            
            // 检查是否是有效的 emoji
            val label = match.value
            val emoji = emojis.find { it.label == label }
            if (emoji != null) {
                result.add(EmojiTextSegment.EmojiImage(emoji))
            } else {
                // 不是有效的 emoji，当作普通文本
                result.add(EmojiTextSegment.Text(label))
            }
            
            lastIndex = match.range.last + 1
        }
        
        // 添加剩余的文本
        if (lastIndex < text.length) {
            result.add(EmojiTextSegment.Text(text.substring(lastIndex)))
        }
        
        return result
    }
}

/**
 * emoji 文本片段
 */
sealed class EmojiTextSegment {
    data class Text(val text: String) : EmojiTextSegment()
    data class EmojiImage(val emoji: Emoji) : EmojiTextSegment()
}
