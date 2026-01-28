package com.projectu.shared.domain.usecase

import com.projectu.shared.domain.model.UpdateCheckResult
import com.projectu.shared.domain.repository.UpdateRepository

/**
 * 检查更新用例
 */
class CheckUpdateUseCase(
    private val updateRepository: UpdateRepository
) {
    /**
     * 执行更新检查
     * 
     * @param currentVersionName 当前版本名称
     * @param currentVersionCode 当前版本代码
     * @return UpdateCheckResult 检查结果
     */
    suspend operator fun invoke(
        currentVersionName: String,
        currentVersionCode: Int
    ): UpdateCheckResult {
        return updateRepository.checkForUpdate(currentVersionName, currentVersionCode)
    }
}
