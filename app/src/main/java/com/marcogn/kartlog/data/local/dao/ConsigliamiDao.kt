package com.marcogn.kartlog.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.marcogn.kartlog.data.local.entity.EventEntity
import com.marcogn.kartlog.data.local.entity.EventStopEntity
import com.marcogn.kartlog.data.local.entity.FoodGroupCourseEntity
import com.marcogn.kartlog.data.local.entity.OutfitFoodRuleEntity
import kotlinx.coroutines.flow.Flow

/** Dati grezzi per l'algoritmo Consigliami (SPEC §6): un [Flow] per tabella, combinati nel ViewModel. */
@Dao
interface ConsigliamiDao {

    @Query(
        """
        SELECT c.id AS id, c.name AS name, c.nameIt AS nameIt, c.rosterOrder AS rosterOrder, c.imageUrl AS imageUrl,
               COALESCE(cu.unlocked, 1) AS unlocked
        FROM characters c
        LEFT JOIN character_unlocks cu ON cu.characterId = c.id
        """
    )
    fun characters(): Flow<List<ConsigliamiCharacterRow>>

    @Query("SELECT id, name, nameIt FROM courses")
    fun courseNames(): Flow<List<IdNameIt>>

    @Query("SELECT id, name FROM food_groups")
    fun foodGroupNames(): Flow<List<IdName>>

    /** Solo outfit non default: hanno sempre un nome (SPEC §3, validato in fase 3). */
    @Query("SELECT id, name, nameIt FROM outfits WHERE isDefault = 0")
    fun outfitNames(): Flow<List<IdNameIt>>

    /** Solo outfit non default: SPEC §6.2, "missing(C) = outfit di C non default e non posseduti". */
    @Query(
        """
        SELECT o.id AS id, o.characterId AS characterId, (oo.outfitId IS NOT NULL) AS owned
        FROM outfits o
        LEFT JOIN owned_outfits oo ON oo.outfitId = o.id
        WHERE o.isDefault = 0
        """
    )
    fun outfits(): Flow<List<ConsigliamiOutfitRow>>

    @Query("SELECT * FROM outfit_food_rules")
    fun rules(): Flow<List<OutfitFoodRuleEntity>>

    @Query("SELECT * FROM food_group_courses")
    fun foodCourses(): Flow<List<FoodGroupCourseEntity>>

    @Query("SELECT * FROM events")
    fun events(): Flow<List<EventEntity>>

    @Query("SELECT * FROM event_stops ORDER BY eventId, position")
    fun eventStops(): Flow<List<EventStopEntity>>
}

data class ConsigliamiCharacterRow(
    val id: String,
    val name: String,
    val nameIt: String?,
    val rosterOrder: Int,
    val imageUrl: String?,
    val unlocked: Boolean,
)

data class ConsigliamiOutfitRow(val id: String, val characterId: String, val owned: Boolean)

data class IdName(val id: String, val name: String)

data class IdNameIt(val id: String, val name: String, val nameIt: String?)
