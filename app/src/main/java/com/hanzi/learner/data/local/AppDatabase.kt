package com.hanzi.learner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        HanziProgressEntity::class,
        PhraseOverrideEntity::class,
        DisabledCharEntity::class,
        AppSettingsEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hanziProgressDao(): HanziProgressDao
    abstract fun phraseOverrideDao(): PhraseOverrideDao
    abstract fun disabledCharDao(): DisabledCharDao
    abstract fun appSettingsDao(): AppSettingsDao

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

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hanzi_learner.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
        }
    }
}
