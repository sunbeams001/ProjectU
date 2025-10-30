package com.projectu.shared.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath

/**
 * Desktop平台的DataStore实现
 */
actual fun createPixivConfigDataStore(): DataStore<Preferences> {
    val userHome = System.getProperty("user.home")
    val datastorePath = "$userHome/.projectu/pixiv_config.preferences_pb"
    
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { datastorePath.toPath() }
    )
}
