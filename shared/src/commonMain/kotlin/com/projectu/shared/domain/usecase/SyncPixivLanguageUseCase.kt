package com.projectu.shared.domain.usecase

import com.projectu.shared.data.local.PixivConfigStore
import com.projectu.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.collectLatest

/**
 * 同步 Pixiv 语言设置的用例
 * 当用户修改应用中的 Pixiv 语言设置时，自动同步到 PixivConfig
 */
class SyncPixivLanguageUseCase(
    private val settingsRepository: SettingsRepository,
    private val pixivConfigStore: PixivConfigStore
) {
    /**
     * 监听设置变化并自动同步 Pixiv 语言
     */
    suspend fun observeAndSync() {
        settingsRepository.getSettings().collectLatest { settings ->
            pixivConfigStore.syncLanguageFromSettings(settings.pixivLanguage)
        }
    }
    
    /**
     * 立即同步当前设置的 Pixiv 语言
     */
    suspend fun syncNow() {
        val settings = settingsRepository.getCurrentSettings()
        pixivConfigStore.syncLanguageFromSettings(settings.pixivLanguage)
    }
}

