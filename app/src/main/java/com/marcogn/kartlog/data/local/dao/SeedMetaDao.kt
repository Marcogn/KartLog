package com.marcogn.kartlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.marcogn.kartlog.data.local.entity.SeedMetaEntity

@Dao
interface SeedMetaDao {

    @Query("SELECT seedVersion FROM seed_meta WHERE id = 0")
    suspend fun getSeedVersion(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSeedVersion(entity: SeedMetaEntity)
}
