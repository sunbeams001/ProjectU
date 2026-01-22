package com.projectu.shared.data.backup.serializer

import java.security.MessageDigest

/**
 * 校验和计算器
 */
object ChecksumCalculator {
    
    /**
     * 计算数据的SHA-256校验和
     */
    fun calculateChecksum(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.toHexString()
    }
    
    /**
     * 计算字符串的SHA-256校验和
     */
    fun calculateChecksum(string: String): String {
        return calculateChecksum(string.toByteArray())
    }
    
    /**
     * 计算整体校验和（用于多个文件）
     */
    fun calculateOverallChecksum(checksums: Map<String, String>): String {
        val combined = checksums.entries
            .sortedBy { it.key }
            .joinToString("") { it.value }
        return calculateChecksum(combined)
    }
    
    /**
     * 验证校验和
     */
    fun verifyChecksum(data: ByteArray, expectedChecksum: String): Boolean {
        val actualChecksum = calculateChecksum(data)
        return actualChecksum == expectedChecksum
    }
    
    /**
     * ByteArray转十六进制字符串
     */
    private fun ByteArray.toHexString(): String {
        return joinToString("") { "%02x".format(it) }
    }
}
