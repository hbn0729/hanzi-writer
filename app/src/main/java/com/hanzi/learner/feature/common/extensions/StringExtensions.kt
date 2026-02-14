package com.hanzi.learner.feature.common.extensions

import com.hanzi.learner.db.toPhraseList as dbToPhraseList

/**
 * Re-export for backward compatibility.
 * Canonical implementation lives in [com.hanzi.learner.db.toPhraseList].
 */
fun String.toPhraseList(): List<String> = dbToPhraseList()