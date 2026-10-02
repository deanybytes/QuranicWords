package com.quranicwords.app.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.quranicwords.app.core.data.local.migration.Migrations
import com.quranicwords.app.core.data.local.migration.PreMigrationBackup
import com.quranicwords.app.core.data.local.dao.AchievementDao
import com.quranicwords.app.core.data.local.dao.ChapterDao
import com.quranicwords.app.core.data.local.dao.DailyPracticeDao
import com.quranicwords.app.core.data.local.dao.LegacyMigrationDao
import com.quranicwords.app.core.data.local.dao.ExerciseAttemptDao
import com.quranicwords.app.core.data.local.dao.ExerciseDao
import com.quranicwords.app.core.data.local.dao.LessonDao
import com.quranicwords.app.core.data.local.dao.SectionDao
import com.quranicwords.app.core.data.local.dao.UserProgressDao
import com.quranicwords.app.core.data.local.dao.UserStatsDao
import com.quranicwords.app.core.data.local.dao.WordFrequencyDao
import com.quranicwords.app.core.data.local.entity.AchievementEntity
import com.quranicwords.app.core.data.local.entity.ChapterEntity
import com.quranicwords.app.core.data.local.entity.DailyPracticeEntity
import com.quranicwords.app.core.data.local.entity.ExerciseAttemptEntity
import com.quranicwords.app.core.data.local.entity.ExerciseEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.SectionEntity
import com.quranicwords.app.core.data.local.entity.UserProgressEntity
import com.quranicwords.app.core.data.local.entity.UserStatsEntity
import com.quranicwords.app.core.data.local.entity.WordFrequencyEntity

// Schema v5 shipped in v1.0.0 and v1.0.1, so learner data now has a real installed base: every
// later version needs a hand-written Migration in migration/Migrations.kt. Destructive fallback is
// allowed only from versions 1-4, which never left development.
@Database(
    entities = [
        WordFrequencyEntity::class,
        ChapterEntity::class,
        SectionEntity::class,
        LessonEntity::class,
        ExerciseEntity::class,
        UserProgressEntity::class,
        UserStatsEntity::class,
        ExerciseAttemptEntity::class,
        AchievementEntity::class,
        DailyPracticeEntity::class
    ],
    version = QwDatabase.VERSION,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class QwDatabase : RoomDatabase() {
    abstract fun wordFrequencyDao(): WordFrequencyDao
    abstract fun chapterDao(): ChapterDao
    abstract fun sectionDao(): SectionDao
    abstract fun lessonDao(): LessonDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun exerciseAttemptDao(): ExerciseAttemptDao
    abstract fun achievementDao(): AchievementDao
    abstract fun dailyPracticeDao(): DailyPracticeDao
    abstract fun legacyMigrationDao(): LegacyMigrationDao

    companion object {
        const val DATABASE_NAME = "quranicwords.db"
        const val VERSION = 6

        @Volatile
        private var INSTANCE: QwDatabase? = null

        fun getInstance(context: android.content.Context): QwDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    PreMigrationBackup.snapshotIfUpgrading(context.applicationContext, DATABASE_NAME, VERSION)
                    androidx.room.Room.databaseBuilder(
                        context.applicationContext,
                        QwDatabase::class.java,
                        DATABASE_NAME
                    )
                        .addMigrations(*Migrations.ALL)
                        .fallbackToDestructiveMigrationFrom(true, 1, 2, 3, 4)
                        .build()
                        .also { INSTANCE = it }
                }
            }
        }
    }
}
