package com.hanzi.learner.db

interface BackupSerializerContract {
    fun encode(data: BackupData): String
    fun decode(json: String): BackupData
}
