package com.hanzi.learner.data

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
