package com.hanzi.learner.features.common.extensions

import com.hanzi.learner.data.toPhraseList as dbToPhraseList

/**
 * Re-export for backward compatibility.
 * Canonical implementation lives in [com.hanzi.learner.data.toPhraseList].
 */
fun String.toPhraseList(): List<String> = dbToPhraseList()