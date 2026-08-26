package com.quranicwords.app.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.quranicwords.app.core.data.local.dao.AchievementDao
import com.quranicwords.app.core.data.local.dao.ChapterDao
import com.quranicwords.app.core.data.local.dao.DailyPracticeDao
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

// This fork starts its own schema history at version 1 - no installed base to migrate from, so
// there's no value in pretending continuity with the source app's version 4. Real hand-written
// Migration objects (see the source app's Migrations.kt for the pattern) are reserved for changes
// made after this fork's own first release.
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
    version = 4,
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

    companion object {
        const val DATABASE_NAME = "quranicwords.db"

        @Volatile
        private var INSTANCE: QwDatabase? = null

        fun getInstance(context: android.content.Context): QwDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    QwDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
