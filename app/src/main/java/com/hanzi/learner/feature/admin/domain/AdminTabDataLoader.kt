package com.hanzi.learner.feature.admin.domain

import com.hanzi.learner.feature.admin.model.AdminProgress
import com.hanzi.learner.hanzi.data.CharIndexItem

interface AdminTabDataLoader<T> {
    suspend fun load(): T
}

interface AdminIndexDataLoader : AdminTabDataLoader<List<CharIndexItem>>

interface AdminDashboardDataLoader : AdminTabDataLoader<AdminDashboardSnapshot>

interface AdminCharacterDataLoader : AdminTabDataLoader<AdminCharacterData>

interface AdminLearningDataLoader : AdminTabDataLoader<AdminLearningData>

data class AdminCharacterData(
    val indexItems: List<CharIndexItem>,
    val disabledChars: Set<String>,
    val allProgress: Map<String, AdminProgress>,
)

data class AdminLearningData(
    val indexItems: List<CharIndexItem>,
    val allProgress: Map<String, AdminProgress>,
    val disabledChars: Set<String>,
)
