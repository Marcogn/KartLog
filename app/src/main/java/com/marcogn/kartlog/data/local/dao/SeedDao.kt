package com.marcogn.kartlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.marcogn.kartlog.data.local.entity.AreaEntity
import com.marcogn.kartlog.data.local.entity.CharacterEntity
import com.marcogn.kartlog.data.local.entity.CourseEntity
import com.marcogn.kartlog.data.local.entity.EventEntity
import com.marcogn.kartlog.data.local.entity.EventStopEntity
import com.marcogn.kartlog.data.local.entity.FoodGroupCourseEntity
import com.marcogn.kartlog.data.local.entity.FoodGroupEntity
import com.marcogn.kartlog.data.local.entity.OutfitEntity
import com.marcogn.kartlog.data.local.entity.OutfitFoodRuleEntity
import com.marcogn.kartlog.data.local.entity.PSwitchEntity
import com.marcogn.kartlog.data.local.entity.PeachMedallionEntity
import com.marcogn.kartlog.data.local.entity.RegionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Upsert dei dati seed (SPEC §3). Ogni reseed sostituisce interamente queste tabelle
 * (upsert per contenuto, non per riga: si svuota e si reinserisce dentro una transazione),
 * senza mai toccare le tabelle di stato utente.
 */
@Dao
interface SeedDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacters(items: List<CharacterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutfits(items: List<OutfitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodGroups(items: List<FoodGroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutfitFoodRules(items: List<OutfitFoodRuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodGroupCourses(items: List<FoodGroupCourseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(items: List<CourseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegions(items: List<RegionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAreas(items: List<AreaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeachMedallions(items: List<PeachMedallionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPSwitches(items: List<PSwitchEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(items: List<EventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventStops(items: List<EventStopEntity>)

    @Query("DELETE FROM characters")
    suspend fun clearCharacters()

    @Query("DELETE FROM outfits")
    suspend fun clearOutfits()

    @Query("DELETE FROM food_groups")
    suspend fun clearFoodGroups()

    @Query("DELETE FROM outfit_food_rules")
    suspend fun clearOutfitFoodRules()

    @Query("DELETE FROM food_group_courses")
    suspend fun clearFoodGroupCourses()

    @Query("DELETE FROM courses")
    suspend fun clearCourses()

    @Query("DELETE FROM regions")
    suspend fun clearRegions()

    @Query("DELETE FROM areas")
    suspend fun clearAreas()

    @Query("DELETE FROM peach_medallions")
    suspend fun clearPeachMedallions()

    @Query("DELETE FROM p_switches")
    suspend fun clearPSwitches()

    @Query("DELETE FROM events")
    suspend fun clearEvents()

    @Query("DELETE FROM event_stops")
    suspend fun clearEventStops()

    @Transaction
    suspend fun replaceAll(seed: SeedContent) {
        clearOutfitFoodRules()
        clearFoodGroupCourses()
        clearEventStops()
        clearOutfits()
        clearPSwitches()
        clearAreas()
        clearPeachMedallions()
        clearCourses()
        clearFoodGroups()
        clearEvents()
        clearCharacters()
        clearRegions()

        insertRegions(seed.regions)
        insertCharacters(seed.characters)
        insertEvents(seed.events)
        insertFoodGroups(seed.foodGroups)
        insertCourses(seed.courses)
        insertPeachMedallions(seed.peachMedallions)
        insertAreas(seed.areas)
        insertPSwitches(seed.pSwitches)
        insertOutfits(seed.outfits)
        insertEventStops(seed.eventStops)
        insertFoodGroupCourses(seed.foodGroupCourses)
        insertOutfitFoodRules(seed.outfitFoodRules)
    }

    /** Tutti gli URL delle immagini (personaggi, outfit, eventi), per il prefetch all'avvio. */
    @Query(
        """
        SELECT imageUrl FROM characters WHERE imageUrl IS NOT NULL
        UNION SELECT imageUrl FROM outfits WHERE imageUrl IS NOT NULL
        UNION SELECT imageUrl FROM events WHERE imageUrl IS NOT NULL
        """
    )
    suspend fun allImageUrls(): List<String>

    @Query("SELECT COUNT(*) FROM outfits")
    fun countOutfits(): Flow<Int>

    @Query("SELECT COUNT(*) FROM peach_medallions")
    fun countPeachMedallions(): Flow<Int>

    @Query("SELECT COUNT(*) FROM p_switches")
    fun countPSwitches(): Flow<Int>

    @Query("SELECT COUNT(*) FROM events")
    fun countEvents(): Flow<Int>
}

/** Tutte le tabelle seed insieme, così [SeedDao.replaceAll] è una singola transazione. */
data class SeedContent(
    val characters: List<CharacterEntity>,
    val outfits: List<OutfitEntity>,
    val foodGroups: List<FoodGroupEntity>,
    val outfitFoodRules: List<OutfitFoodRuleEntity>,
    val foodGroupCourses: List<FoodGroupCourseEntity>,
    val courses: List<CourseEntity>,
    val regions: List<RegionEntity>,
    val areas: List<AreaEntity>,
    val peachMedallions: List<PeachMedallionEntity>,
    val pSwitches: List<PSwitchEntity>,
    val events: List<EventEntity>,
    val eventStops: List<EventStopEntity>,
)
