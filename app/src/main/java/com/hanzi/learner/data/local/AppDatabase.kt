package com.hanzi.learner.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hanzi.learner.data.local.dao.AppSettingsDao
import com.hanzi.learner.data.local.dao.DisabledCharDao
import com.hanzi.learner.data.local.dao.HanziProgressDao
import com.hanzi.learner.data.local.dao.PhraseOverrideDao
import com.hanzi.learner.data.local.dao.TtsPreferenceDao
import com.hanzi.learner.data.local.entity.AppSettingsEntity
import com.hanzi.learner.data.local.entity.DisabledCharEntity
import com.hanzi.learner.data.local.entity.HanziProgressEntity
import com.hanzi.learner.data.local.entity.PhraseOverrideEntity
import com.hanzi.learner.data.local.entity.TtsPreferenceEntity

@Database(
    entities = [
        HanziProgressEntity::class,
        PhraseOverrideEntity::class,
        DisabledCharEntity::class,
        AppSettingsEntity::class,
        TtsPreferenceEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hanziProgressDao(): HanziProgressDao
    abstract fun phraseOverrideDao(): PhraseOverrideDao
    abstract fun disabledCharDao(): DisabledCharDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun ttsPreferenceDao(): TtsPreferenceDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `phrase_override` (`char` TEXT NOT NULL, `phrasesJson` TEXT NOT NULL, PRIMARY KEY(`char`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `disabled_char` (`char` TEXT NOT NULL, PRIMARY KEY(`char`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `app_settings` (`id` INTEGER NOT NULL, `duePickLimit` INTEGER NOT NULL, `hintAfterMisses` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `app_settings` ADD COLUMN `useExternalDataset` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tts_preference` (`id` INTEGER NOT NULL, `selectedModelId` TEXT, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_hanzi_progress_nextDueDay` ON `hanzi_progress`(`nextDueDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_hanzi_progress_lastStudiedDay` ON `hanzi_progress`(`lastStudiedDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_hanzi_progress_wrongCount` ON `hanzi_progress`(`wrongCount`)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `app_settings` ADD COLUMN `autoReadAloud` INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hanzi_learner.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build().also { instance = it }
            }
        }
    }
}