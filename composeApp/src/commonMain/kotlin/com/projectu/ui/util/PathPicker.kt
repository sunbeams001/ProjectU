package com.projectu.ui.util

/**
 * 路径选择器接口
 * 提供跨平台的目录选择功能
 */
interface PathPicker {
    /**
     * 启动目录选择器
     * @param initialPath 初始路径（可选）
     * @param onPathSelected 路径选择回调，返回选中的路径，null表示取消
     */
    fun pickDirectory(
        initialPath: String? = null,
        onPathSelected: (String?) -> Unit
    )
}
