package com.hanzi.learner.data

interface BackupVersionValidator {
    fun validate(json: String): Int
}

object DefaultBackupVersionValidator : BackupVersionValidator {
    private val versionRegex = Regex("\"version\"\\s*:\\s*(\\d+)")
    private val supportedVersions = setOf(1)

    override fun validate(json: String): Int {
        val version = versionRegex.find(json)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        if (version !in supportedVersions) {
            throw IllegalArgumentException("不支持的备份版本: $version")
        }
        return version
    }
}
