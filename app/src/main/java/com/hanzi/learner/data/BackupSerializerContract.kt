package com.hanzi.learner.data

import com.hanzi.learner.data.model.BackupData

interface BackupSerializerContract {
    fun encode(data: BackupData): String
    fun decode(json: String): BackupData
}
