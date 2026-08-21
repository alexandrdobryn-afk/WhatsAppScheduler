package com.example.wascheduler.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.wascheduler.data.dao.ExecutionDao
import com.example.wascheduler.data.dao.RuleDao
import com.example.wascheduler.data.entity.ExecutionEntity
import com.example.wascheduler.data.entity.RuleEntity
import com.example.wascheduler.data.entity.RuleTimeEntity

@Database(
    entities = [RuleEntity::class, RuleTimeEntity::class, ExecutionEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
    abstract fun executionDao(): ExecutionDao

    companion object {
        const val DATABASE_NAME = "wascheduler.db"
    }
}
