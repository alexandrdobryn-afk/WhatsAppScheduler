package com.example.wascheduler.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.wascheduler.core.accessibility.WhatsAppUiAdapter
import com.example.wascheduler.core.accessibility.WhatsAppUiAdapterImpl
import com.example.wascheduler.core.automation.RetryPolicy
import com.example.wascheduler.core.scheduler.DataStoreScheduleTimeZoneProvider
import com.example.wascheduler.core.scheduler.ScheduleTimeZoneProvider
import com.example.wascheduler.data.dao.ExecutionDao
import com.example.wascheduler.data.dao.RuleDao
import com.example.wascheduler.data.database.AppDatabase
import com.example.wascheduler.data.repository.ExecutionRepositoryImpl
import com.example.wascheduler.data.repository.RuleRepositoryImpl
import com.example.wascheduler.data.repository.SettingsRepository
import com.example.wascheduler.domain.repository.ExecutionRepository
import com.example.wascheduler.domain.repository.RuleRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE rules ADD COLUMN startDate TEXT NOT NULL DEFAULT '1970-01-01'")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE rules ADD COLUMN scheduleType TEXT NOT NULL DEFAULT 'WEEKLY'")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `rule_dates` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `ruleId` INTEGER NOT NULL,
                    `localDate` TEXT NOT NULL,
                    FOREIGN KEY(`ruleId`) REFERENCES `rules`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_rule_dates_ruleId` ON `rule_dates` (`ruleId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_rule_dates_ruleId_localDate` ON `rule_dates` (`ruleId`, `localDate`)")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE rule_times ADD COLUMN localDate TEXT DEFAULT NULL")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_rule_times_ruleId_localDate` ON `rule_times` (`ruleId`, `localDate`)")
            db.execSQL(
                """
                INSERT INTO `rule_times` (
                    `ruleId`, `localTime`, `localDate`,
                    `monday`, `tuesday`, `wednesday`, `thursday`, `friday`, `saturday`, `sunday`,
                    `enabled`
                )
                SELECT
                    t.`ruleId`, t.`localTime`, d.`localDate`,
                    t.`monday`, t.`tuesday`, t.`wednesday`, t.`thursday`, t.`friday`, t.`saturday`, t.`sunday`,
                    t.`enabled`
                FROM `rule_times` t
                INNER JOIN `rules` r ON r.`id` = t.`ruleId`
                INNER JOIN `rule_dates` d ON d.`ruleId` = t.`ruleId`
                WHERE r.`scheduleType` = 'MULTIPLE_DATES'
                  AND t.`localDate` IS NULL
                """.trimIndent()
            )
            db.execSQL(
                """
                DELETE FROM `rule_times`
                WHERE `localDate` IS NULL
                  AND `ruleId` IN (
                      SELECT r.`id`
                      FROM `rules` r
                      WHERE r.`scheduleType` = 'MULTIPLE_DATES'
                        AND EXISTS (
                            SELECT 1 FROM `rule_dates` d WHERE d.`ruleId` = r.`id`
                        )
                  )
                """.trimIndent()
            )
        }
    }

    @Provides
    fun provideRuleDao(db: AppDatabase): RuleDao = db.ruleDao()

    @Provides
    fun provideExecutionDao(db: AppDatabase): ExecutionDao = db.executionDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindRuleRepository(impl: RuleRepositoryImpl): RuleRepository

    @Binds
    abstract fun bindExecutionRepository(impl: ExecutionRepositoryImpl): ExecutionRepository

    @Binds
    abstract fun bindWhatsAppUiAdapter(impl: WhatsAppUiAdapterImpl): WhatsAppUiAdapter

    @Binds
    abstract fun bindScheduleTimeZoneProvider(impl: DataStoreScheduleTimeZoneProvider): ScheduleTimeZoneProvider
}

@Module
@InstallIn(SingletonComponent::class)
object MiscModule {
    @Provides
    @Singleton
    fun provideRetryPolicy(settingsRepository: SettingsRepository): RetryPolicy =
        RetryPolicy(maxAttemptsProvider = { settingsRepository.maxRetryAttempts.first() })
}
