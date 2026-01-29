package com.cosmic_struck.stellar.common.di

import android.content.Context
import androidx.room.Room
import com.cosmic_struck.stellar.classroom.data.local.ClassroomDatabase
import com.cosmic_struck.stellar.classroom.data.local.dao.ModuleProcessDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideClassroomDatabase(
        @ApplicationContext context: Context
    ): ClassroomDatabase {
        return Room.databaseBuilder(
            context,
            ClassroomDatabase::class.java,
            "classroom_database"
        ).build()
    }
    
    @Provides
    @Singleton
    fun provideModuleProcessDao(database: ClassroomDatabase): ModuleProcessDao {
        return database.moduleProcessDao()
    }
}
