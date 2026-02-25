package com.hanzi.learner.feature.admin.repository

import com.hanzi.learner.data.local.entity.AppSettingsEntity
import com.hanzi.learner.features.admin.model.AdminSettings
import com.hanzi.learner.features.admin.repository.toAdminSettings
import com.hanzi.learner.features.admin.repository.toEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class AdminRepositoryMappersTest {

    @Test
    fun toAdminSettings_mapsAllFields() {
        val entity = AppSettingsEntity(
            id = 1,
            duePickLimit = 100,
            hintAfterMisses = 5,
            useExternalDataset = true,
            autoReadAloud = false,
        )

        val adminSettings = entity.toAdminSettings()

        assertEquals(100, adminSettings.duePickLimit)
        assertEquals(5, adminSettings.hintAfterMisses)
        assertEquals(true, adminSettings.useExternalDataset)
        assertEquals(false, adminSettings.autoReadAloud)
    }

    @Test
    fun toEntity_mapsAllFields() {
        val adminSettings = AdminSettings(
            duePickLimit = 100,
            hintAfterMisses = 5,
            useExternalDataset = true,
            autoReadAloud = false,
        )

        val entity = adminSettings.toEntity()

        assertEquals(1, entity.id)
        assertEquals(100, entity.duePickLimit)
        assertEquals(5, entity.hintAfterMisses)
        assertEquals(true, entity.useExternalDataset)
        assertEquals(false, entity.autoReadAloud)
    }

    @Test
    fun roundTrip_preservesAllFields() {
        val original = AdminSettings(
            duePickLimit = 75,
            hintAfterMisses = 3,
            useExternalDataset = true,
            autoReadAloud = false,
        )

        val entity = original.toEntity()
        val restored = entity.toAdminSettings()

        assertEquals(original.duePickLimit, restored.duePickLimit)
        assertEquals(original.hintAfterMisses, restored.hintAfterMisses)
        assertEquals(original.useExternalDataset, restored.useExternalDataset)
        assertEquals(original.autoReadAloud, restored.autoReadAloud)
    }

    @Test
    fun toAdminSettings_defaultValues() {
        val entity = AppSettingsEntity()

        val adminSettings = entity.toAdminSettings()

        assertEquals(50, adminSettings.duePickLimit)
        assertEquals(2, adminSettings.hintAfterMisses)
        assertEquals(false, adminSettings.useExternalDataset)
        assertEquals(true, adminSettings.autoReadAloud)
    }
}
