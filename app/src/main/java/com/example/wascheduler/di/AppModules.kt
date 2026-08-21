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
            .addMigrations(MIGRATION_1_2)
            .build()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE rules ADD COLUMN startDate TEXT NOT NULL DEFAULT '1970-01-01'")
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
