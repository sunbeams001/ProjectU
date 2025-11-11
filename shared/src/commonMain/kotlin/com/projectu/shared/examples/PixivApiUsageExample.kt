package com.projectu.shared.examples

import com.projectu.shared.data.remote.api.PixivApi
import com.projectu.shared.domain.repository.ArtworkRepository
import com.projectu.shared.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Pixiv API 使用示例
 * 
 * 注意：这些是示例代码，实际使用时需要：
 * 1. 配置正确的 PHPSESSID
 * 2. 在适当的协程作用域中执行
 * 3. 处理异常和错误
 */

/**
 * 示例 1: 获取作品详情
 */
suspend fun example1GetArtworkDetail(
    artworkRepository: ArtworkRepository,
    artworkId: Long = 102814610L
) {
    artworkRepository.getArtworkDetail(artworkId)
        .onSuccess { artwork ->
            println("=== 作品详情 ===")
            println("ID: ${artwork.id}")
            println("标题: ${artwork.title}")
            println("作者: ${artwork.userName}")
            println("类型: ${artwork.type}")
            println("尺寸: ${artwork.width}x${artwork.height}")
            println("浏览: ${artwork.viewCount}")
            println("收藏: ${artwork.bookmarkCount}")
            println("点赞: ${artwork.likeCount}")
            println("标签: ${artwork.tags.joinToString(", ")}")
        }
        .onFailure { error ->
            println("获取失败: ${error.message}")
        }
}

/**
 * 示例 2: 搜索作品
 */
suspend fun example2SearchArtworks(
    artworkRepository: ArtworkRepository,
    keyword: String = "初音ミク"
) {
    artworkRepository.searchArtworks(
        keyword = keyword,
        page = 1,
        searchMode = "s_tag",  // 标签搜索
        order = "date_d"       // 从新到旧
    ).onSuccess { artworks ->
        println("=== 搜索结果 ===")
        println("关键词: $keyword")
        println("找到 ${artworks.size} 个作品")
        artworks.take(5).forEach { artwork ->
            println("- [${artwork.id}] ${artwork.title} by ${artwork.userName}")
        }
    }
}

/**
 * 示例 3: 获取推荐作品
 */
suspend fun example3GetRecommendations(
    artworkRepository: ArtworkRepository
) {
    artworkRepository.getRecommendedArtworks(
        page = 1,
        limit = 20
    ).onSuccess { artworks ->
        println("=== 推荐作品 ===")
        println("共 ${artworks.size} 个推荐")
        artworks.take(10).forEach { artwork ->
            println("- ${artwork.title} by ${artwork.userName} (❤️${artwork.likeCount})")
        }
    }
}

/**
 * 示例 4: 获取关注用户的最新作品
 */
suspend fun example4GetFollowingArtworks(
    artworkRepository: ArtworkRepository
) {
    artworkRepository.getFollowingArtworks(page = 1)
        .onSuccess { artworks ->
            println("=== 关注用户最新作品 ===")
            artworks.take(10).forEach { artwork ->
                println("- ${artwork.title} by ${artwork.userName}")
            }
        }
}

/**
 * 示例 5: 获取排行榜
 */
suspend fun example5GetRanking(
    artworkRepository: ArtworkRepository
) {
    artworkRepository.getRankingArtworks(
        mode = "daily",  // 日榜
        page = 1
    ).onSuccess { artworks ->
        println("=== 日榜 Top 10 ===")
        artworks.take(10).forEachIndexed { index, artwork ->
            println("${index + 1}. ${artwork.title} by ${artwork.userName}")
            println("   ❤️${artwork.likeCount} 💾${artwork.bookmarkCount}")
        }
    }
}

/**
 * 示例 6: 收藏作品
 */
suspend fun example6BookmarkArtwork(
    artworkRepository: ArtworkRepository,
    artworkId: Long
) {
    // 添加收藏
    artworkRepository.addBookmark(
        artworkId = artworkId,
        isPrivate = false,  // 公开收藏
        tags = listOf("喜欢", "收藏")
    ).onSuccess {
        println("收藏成功！")
    }.onFailure { error ->
        println("收藏失败: ${error.message}")
    }
}

/**
 * 示例 7: 获取用户信息
 */
suspend fun example7GetUserInfo(
    userRepository: UserRepository,
    userId: Long
) {
    userRepository.getUserById(userId)
        .onSuccess { user ->
            println("=== 用户信息 ===")
            println("ID: ${user.id}")
            println("名称: ${user.name}")
            println("账号: ${user.account}")
            println("是否关注: ${user.isFollowed}")
        }
}

/**
 * 示例 8: 关注/取消关注用户
 */
suspend fun example8FollowUser(
    userRepository: UserRepository,
    userId: Long
) {
    // 关注用户
    userRepository.followUser(userId)
        .onSuccess {
            println("关注成功！")
        }
    
    // 取消关注
    userRepository.unfollowUser(userId)
        .onSuccess {
            println("已取消关注")
        }
}

/**
 * 示例 9: 直接使用 Pixiv API
 */
suspend fun example9DirectApiUsage(pixivApi: PixivApi) {
    // 获取作品详情
    val detailResponse = pixivApi.illustApi.getDetail(102814610L)
    if (!detailResponse.error) {
        println("作品: ${detailResponse.body?.title}")
    }
    
    // 搜索
    val searchResponse = pixivApi.illustApi.search(
        keyword = "原神",
        page = 1
    )
    println("搜索到 ${searchResponse.body?.illustManga?.total} 个结果")
    
    // 获取 Ugoira 元数据
    val ugoiraResponse = pixivApi.illustApi.getUgoiraMeta(102814610L)
    if (!ugoiraResponse.error) {
        val meta = ugoiraResponse.body
        println("动图ZIP: ${meta?.originalSrc}")
        println("帧数: ${meta?.frames?.size}")
    }
}

/**
 * 示例 10: 获取 Ugoira 动图
 */
suspend fun example10GetUgoiraMetadata(
    artworkRepository: ArtworkRepository,
    ugoiraId: Long = 102814610L
) {
    artworkRepository.getUgoiraMetadata(ugoiraId)
        .onSuccess { metadata ->
            println("=== Ugoira 元数据 ===")
            println("ZIP URL: ${metadata.zipUrl}")
            println("总帧数: ${metadata.frames.size}")
            println("前5帧:")
            metadata.frames.take(5).forEach { frame ->
                println("- ${frame.file}: ${frame.delay}ms")
            }
        }
}

/**
 * 完整示例：运行所有示例
 */
fun runAllExamples(
    artworkRepository: ArtworkRepository,
    userRepository: UserRepository,
    pixivApi: PixivApi,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    scope.launch {
        try {
            println("\n========== Pixiv API 使用示例 ==========\n")
            
            // 示例 1: 获取作品详情
            example1GetArtworkDetail(artworkRepository)
            println()
            
            // 示例 2: 搜索
            example2SearchArtworks(artworkRepository, "初音ミク")
            println()
            
            // 示例 3: 推荐
            example3GetRecommendations(artworkRepository)
            println()
            
            // 示例 5: 排行榜
            example5GetRanking(artworkRepository)
            println()
            
            // 示例 9: 直接使用 API
            example9DirectApiUsage(pixivApi)
            println()
            
            // 示例 10: Ugoira
            example10GetUgoiraMetadata(artworkRepository)
            
            println("\n========== 示例运行完成 ==========\n")
        } catch (e: Exception) {
            println("示例运行出错: ${e.message}")
            e.printStackTrace()
        }
    }
}

