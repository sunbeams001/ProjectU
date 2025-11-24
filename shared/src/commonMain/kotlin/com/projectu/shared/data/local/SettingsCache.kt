package com.projectu.shared.data.local

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 应用设置缓存管理器
 * 
 * 统一管理所有需要高频访问的配置项，从 SettingsStore 订阅变化并缓存在内存中
 * 
 * 设计模式：
 * - 单一职责：只负责配置的内存缓存，不负责持久化
 * - 响应式更新：自动订阅 SettingsStore 的 Flow，配置变更时自动更新缓存
 * - 高性能访问：提供同步的 getter 方法，避免数据库查询
 * 
 * 使用场景：
 * - 需要在业务逻辑中频繁访问的配置项
 * - 例如：R18 阈值判定、语言偏好、过滤规则等
 * 
 * @param settingsStore 设置数据源
 */
class SettingsCache(
    private val settingsStore: SettingsStore
) {
    // ==================== 缓存字段 ====================
    
    /**
     * Pixiv API 语言偏好缓存
     * 用于 API 请求时快速获取语言代码
     */
    private val _pixivLanguage = MutableStateFlow(PixivLanguage.SIMPLIFIED_CHINESE)
    val pixivLanguage: StateFlow<PixivLanguage> = _pixivLanguage.asStateFlow()
    
    /**
     * R18 Sanity Level 阈值缓存
     * 用于作品年龄限制判定时快速获取阈值
     */
    private val _r18SanityLevelThreshold = MutableStateFlow(6)
    val r18SanityLevelThreshold: StateFlow<Int> = _r18SanityLevelThreshold.asStateFlow()
    
    // TODO: 后续添加更多配置项缓存
    // private val _someOtherConfig = MutableStateFlow(defaultValue)
    // val someOtherConfig: StateFlow<Type> = _someOtherConfig.asStateFlow()
    
    init {
        // 在后台协程中监听设置变化，自动更新所有缓存
        CoroutineScope(Dispatchers.Default).launch {
            settingsStore.settings.collect { settings ->
                // 更新所有缓存字段
                _pixivLanguage.value = settings.pixivLanguage
                _r18SanityLevelThreshold.value = settings.r18SanityLevelThreshold
                
                // TODO: 后续添加更多字段的同步
                // _someOtherConfig.value = settings.someOtherConfig
            }
        }
    }
    
    // ==================== 同步访问方法 ====================
    
    /**
     * 获取当前 Pixiv API 语言偏好（同步方法，使用内存缓存）
     */
    fun getPixivLanguage(): PixivLanguage {
        return _pixivLanguage.value
    }
    
    /**
     * 获取当前 Pixiv API 语言代码（同步方法，使用内存缓存）
     */
    fun getPixivLanguageCode(): String {
        return _pixivLanguage.value.code
    }
    
    /**
     * 获取当前 R18 Sanity Level 阈值（同步方法，使用内存缓存）
     */
    fun getR18SanityLevelThreshold(): Int {
        return _r18SanityLevelThreshold.value
    }
    
    // TODO: 后续添加更多配置项的 getter
    // fun getSomeOtherConfig(): Type {
    //     return _someOtherConfig.value
    // }
}
