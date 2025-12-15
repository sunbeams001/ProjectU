package com.projectu.shared.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.projectu.shared.data.local.database.ContextHolder
import okio.Path.Companion.toPath

/**
 * Android平台的DataStore实现
 */
actual fun createPixivConfigDataStore(): DataStore<Preferences> {
    val context = ContextHolder.getContext()
    val dataStoreFile = context.filesDir.resolve("pixiv_config.preferences_pb")
    
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { dataStoreFile.absolutePath.toPath() }
    )
}

actual fun createSearchHistoryDataStore(): DataStore<Preferences> {
    val context = ContextHolder.getContext()
    val dataStoreFile = context.filesDir.resolve("search_history.preferences_pb")
    
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { dataStoreFile.absolutePath.toPath() }
    )
}
