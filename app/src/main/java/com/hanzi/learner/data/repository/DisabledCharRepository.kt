package com.hanzi.learner.data.repository

import com.hanzi.learner.data.local.dao.DisabledCharDao
import com.hanzi.learner.data.local.entity.DisabledCharEntity

class DisabledCharRepository(
    private val dao: DisabledCharDao,
) : DisabledCharRepositoryContract {
    override suspend fun getAllDisabledChars(): List<String> = dao.getAllDisabledChars()

    override suspend fun enable(char: String) {
        dao.enable(char)
    }

    override suspend fun disable(char: String) {
        dao.disable(DisabledCharEntity(char))
    }
}
