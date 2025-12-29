package com.projectu.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.MangaSeries
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.NovelSeries
import com.projectu.shared.domain.usecase.SaveBrowseHistoryUseCase
import org.koin.compose.koinInject

/**
 * 保存作品浏览历史的Composable帮助函数
 * 在作品详情页面加载完成后自动调用
 */
@Composable
fun SaveArtworkHistory(artwork: Artwork?) {
    val saveBrowseHistoryUseCase: SaveBrowseHistoryUseCase = koinInject()
    
    LaunchedEffect(artwork?.id) {
        artwork?.let {
            saveBrowseHistoryUseCase.saveArtworkHistory(
                artworkId = it.id,
                title = it.title,
                thumbnailUrl = it.imageUrls.pages.firstOrNull()?.urls?.squareMedium,
                authorId = it.userId,
                authorName = it.userName,
                isR18 = it.ageLimit != com.projectu.shared.domain.model.AgeLimit.ALL_AGE,
                isAi = it.isAiGenerated,
                isUgoira = it.type == com.projectu.shared.domain.model.ArtworkType.UGOIRA,
                isManga = it.type == com.projectu.shared.domain.model.ArtworkType.MANGA
            )
        }
    }
}

/**
 * 保存小说浏览历史的Composable帮助函数
 */
@Composable
fun SaveNovelHistory(novel: Novel?) {
    val saveBrowseHistoryUseCase: SaveBrowseHistoryUseCase = koinInject()
    
    LaunchedEffect(novel?.id) {
        novel?.let {
            saveBrowseHistoryUseCase.saveNovelHistory(
                novelId = it.id,
                title = it.title,
                thumbnailUrl = it.imageUrl,
                authorId = it.userId,
                authorName = it.userName,
                isR18 = it.ageLimit != com.projectu.shared.domain.model.AgeLimit.ALL_AGE,
                isAi = it.isAiGenerated
            )
        }
    }
}

/**
 * 保存小说系列浏览历史的Composable帮助函数
 */
@Composable
fun SaveNovelSeriesHistory(series: NovelSeries?) {
    val saveBrowseHistoryUseCase: SaveBrowseHistoryUseCase = koinInject()
    
    LaunchedEffect(series?.id) {
        series?.let {
            saveBrowseHistoryUseCase.saveNovelSeriesHistory(
                seriesId = it.id,
                title = it.title,
                thumbnailUrl = it.coverUrl,
                authorId = it.userId,
                authorName = it.userName,
                isR18 = it.ageLimit != com.projectu.shared.domain.model.AgeLimit.ALL_AGE
            )
        }
    }
}

/**
 * 保存漫画系列浏览历史的Composable帮助函数
 */
@Composable
fun SaveMangaSeriesHistory(series: MangaSeries?) {
    val saveBrowseHistoryUseCase: SaveBrowseHistoryUseCase = koinInject()
    
    LaunchedEffect(series?.id) {
        series?.let {
            saveBrowseHistoryUseCase.saveMangaSeriesHistory(
                seriesId = it.id,
                title = it.title,
                thumbnailUrl = it.coverUrl,
                authorId = it.userId,
                authorName = it.userName,
                isR18 = false
            )
        }
    }
}
