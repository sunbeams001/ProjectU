package com.projectu.shared.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * 创建 PixivConfig 数据存储的expect函数
 */
expect fun createPixivConfigDataStore(): DataStore<Preferences>
