package com.projectu.shared.domain.repository

import com.projectu.shared.domain.model.UpdateCheckResult

/**
 * 更新检查仓储接口
 */
interface UpdateRepository {
    /**
     * 检查更新
     * 
     * @param currentVersionName 当前版本名称 (例如: "1.0.10")
     * @param currentVersionCode 当前版本代码
     * @return UpdateCheckResult 检查结果
     */
    suspend fun checkForUpdate(
        currentVersionName: String,
        currentVersionCode: Int
    ): UpdateCheckResult
}
