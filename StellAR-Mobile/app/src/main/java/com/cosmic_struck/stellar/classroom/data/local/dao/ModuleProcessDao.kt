package com.cosmic_struck.stellar.classroom.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cosmic_struck.stellar.classroom.data.local.entity.ModuleProcessEntity

/**
 * DAO for module process data (quiz and summary)
 */
@Dao
interface ModuleProcessDao {
    
    @Query("SELECT * FROM module_process_data WHERE moduleId = :moduleId")
    suspend fun getByModuleId(moduleId: Long): ModuleProcessEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ModuleProcessEntity)
    
    @Query("DELETE FROM module_process_data WHERE moduleId = :moduleId")
    suspend fun deleteByModuleId(moduleId: Long)
    
    @Query("DELETE FROM module_process_data")
    suspend fun deleteAll()
    
    @Query("SELECT EXISTS(SELECT 1 FROM module_process_data WHERE moduleId = :moduleId)")
    suspend fun exists(moduleId: Long): Boolean
}
