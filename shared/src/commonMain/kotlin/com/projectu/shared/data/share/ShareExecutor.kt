package com.projectu.shared.data.share

import com.projectu.shared.domain.model.ShareData
import com.projectu.shared.domain.model.ShareResult
import com.projectu.shared.domain.model.ShareTarget

/**
 * 平台特定的分享执行器接口
 * 
 * 该接口由各平台实现，负责执行实际的分享操作：
 * - Android: 使用 Intent.ACTION_SEND
 * - Desktop: 复制到剪贴板或保存文件
 * 
 * 设计原则：
 * - 接口定义在 commonMain，由各平台在 androidMain/desktopMain 实现
 * - 接收平台无关的 ShareData，执行平台特定的操作
 * - 返回统一的 ShareResult
 */
interface ShareExecutor {
    
    /**
     * 执行分享操作
     * 
     * @param shareData 准备好的分享数据
     * @return 分享结果（成功/失败/取消）
     */
    suspend fun executeShare(shareData: ShareData): ShareResult
    
    /**
     * 检查当前平台是否支持分享功能
     * 
     * @return true 如果支持分享
     */
    fun isShareSupported(): Boolean
    
    /**
     * 获取可用的分享目标列表（平台特定）
     * 
     * Android: 通常返回空列表，使用系统分享面板
     * Desktop: 返回 ["复制到剪贴板", "保存文件"] 等选项
     * 
     * @return 可用的分享目标列表
     */
    suspend fun getAvailableShareTargets(): List<ShareTarget>
}
