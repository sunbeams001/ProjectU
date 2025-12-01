package com.projectu.ui.util

import android.util.Log

actual object AppLogger {
    actual fun d(tag: String, message: String) {
        Log.d(tag, message)
    }
    
    actual fun e(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
}
