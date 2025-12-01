package com.projectu.ui.util

actual object AppLogger {
    actual fun d(tag: String, message: String) {
        println("D/$tag: $message")
    }
    
    actual fun e(tag: String, message: String, throwable: Throwable?) {
        println("E/$tag: $message")
        throwable?.printStackTrace()
    }
}
