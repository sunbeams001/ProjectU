package com.projectu.shared.domain.usecase

import com.projectu.shared.domain.model.UgoiraMetadata
import com.projectu.shared.domain.repository.ArtworkRepository

/**
 * 获取Ugoira动图元数据用例
 */
class GetUgoiraUseCase(
    private val artworkRepository: ArtworkRepository
) {
    suspend operator fun invoke(artworkId: String): Result<UgoiraMetadata> {
        return artworkRepository.getUgoiraMetadata(artworkId)
    }
}

