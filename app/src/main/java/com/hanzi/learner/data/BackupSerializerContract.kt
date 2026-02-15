package com.hanzi.learner.data

interface BackupSerializerContract {
    fun encode(data: BackupData): String
    fun decode(json: String): BackupData
}
