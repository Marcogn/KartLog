package com.marcogn.kartlog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.marcogn.kartlog.data.local.dao.BackupDao
import com.marcogn.kartlog.data.local.dao.ConsigliamiDao
import com.marcogn.kartlog.data.local.dao.MedallionsDao
import com.marcogn.kartlog.data.local.dao.PSwitchesDao
import com.marcogn.kartlog.data.local.dao.SeedDao
import com.marcogn.kartlog.data.local.dao.SeedMetaDao
import com.marcogn.kartlog.data.local.dao.SkinDao
import com.marcogn.kartlog.data.local.dao.UserStateDao
import com.marcogn.kartlog.data.local.entity.AreaEntity
import com.marcogn.kartlog.data.local.entity.BestResultEntity
import com.marcogn.kartlog.data.local.entity.CharacterEntity
import com.marcogn.kartlog.data.local.entity.CharacterUnlockEntity
import com.marcogn.kartlog.data.local.entity.CollectedMedallionEntity
import com.marcogn.kartlog.data.local.entity.CompletedPSwitchEntity
import com.marcogn.kartlog.data.local.entity.CourseEntity
import com.marcogn.kartlog.data.local.entity.EventEntity
import com.marcogn.kartlog.data.local.entity.EventStopEntity
import com.marcogn.kartlog.data.local.entity.FoodGroupCourseEntity
import com.marcogn.kartlog.data.local.entity.FoodGroupEntity
import com.marcogn.kartlog.data.local.entity.OutfitEntity
import com.marcogn.kartlog.data.local.entity.OutfitFoodRuleEntity
import com.marcogn.kartlog.data.local.entity.OwnedOutfitEntity
import com.marcogn.kartlog.data.local.entity.PSwitchEntity
import com.marcogn.kartlog.data.local.entity.PeachMedallionEntity
import com.marcogn.kartlog.data.local.entity.RegionEntity
import com.marcogn.kartlog.data.local.entity.SeedMetaEntity

const val DATABASE_NAME = "kartlog.db"

@Database(
    entities = [
        CharacterEntity::class,
        OutfitEntity::class,
        FoodGroupEntity::class,
        OutfitFoodRuleEntity::class,
        FoodGroupCourseEntity::class,
        CourseEntity::class,
        RegionEntity::class,
        AreaEntity::class,
        PeachMedallionEntity::class,
        PSwitchEntity::class,
        EventEntity::class,
        EventStopEntity::class,
        SeedMetaEntity::class,
        OwnedOutfitEntity::class,
        CharacterUnlockEntity::class,
        CollectedMedallionEntity::class,
        CompletedPSwitchEntity::class,
        BestResultEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class KartLogDatabase : RoomDatabase() {
    abstract fun seedDao(): SeedDao
    abstract fun seedMetaDao(): SeedMetaDao
    abstract fun userStateDao(): UserStateDao
    abstract fun skinDao(): SkinDao
    abstract fun medallionsDao(): MedallionsDao
    abstract fun pSwitchesDao(): PSwitchesDao
    abstract fun consigliamiDao(): ConsigliamiDao
    abstract fun backupDao(): BackupDao
}
