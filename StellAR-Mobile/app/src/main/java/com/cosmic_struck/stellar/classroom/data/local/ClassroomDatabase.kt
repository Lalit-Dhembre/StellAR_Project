package com.cosmic_struck.stellar.classroom.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cosmic_struck.stellar.classroom.data.local.converter.QuizListConverter
import com.cosmic_struck.stellar.classroom.data.local.dao.ModuleProcessDao
import com.cosmic_struck.stellar.classroom.data.local.entity.ModuleProcessEntity

/**
 * Room database for classroom module data
 */
@Database(
    entities = [ModuleProcessEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(QuizListConverter::class)
abstract class ClassroomDatabase : RoomDatabase() {
    abstract fun moduleProcessDao(): ModuleProcessDao
}
