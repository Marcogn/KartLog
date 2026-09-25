package com.marcogn.kartlog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.marcogn.kartlog.data.local.entity.CharacterUnlockEntity
import com.marcogn.kartlog.data.local.entity.CollectedMedallionEntity
import com.marcogn.kartlog.data.local.entity.CompletedPSwitchEntity
import com.marcogn.kartlog.data.local.entity.OwnedOutfitEntity
import com.marcogn.kartlog.data.local.entity.RaceResultEntity
import kotlinx.coroutines.flow.Flow

/** Stato utente (SPEC §3): mai toccato dal reseed. */
@Dao
interface UserStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markOutfitOwned(entity: OwnedOutfitEntity)

    @Query("DELETE FROM owned_outfits WHERE outfitId = :outfitId")
    suspend fun markOutfitNotOwned(outfitId: String)

    @Query("SELECT COUNT(*) FROM owned_outfits")
    fun countOwnedOutfits(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setCharacterUnlock(entity: CharacterUnlockEntity)

    @Query("SELECT unlocked FROM character_unlocks WHERE characterId = :characterId")
    suspend fun isCharacterUnlocked(characterId: String): Boolean?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markMedallionCollected(entity: CollectedMedallionEntity)

    @Query("DELETE FROM collected_medallions WHERE medallionId = :medallionId")
    suspend fun markMedallionNotCollected(medallionId: String)

    @Query("SELECT COUNT(*) FROM collected_medallions")
    fun countCollectedMedallions(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markPSwitchCompleted(entity: CompletedPSwitchEntity)

    @Query("DELETE FROM completed_p_switches WHERE pSwitchId = :pSwitchId")
    suspend fun markPSwitchNotCompleted(pSwitchId: String)

    @Query("SELECT COUNT(*) FROM completed_p_switches")
    fun countCompletedPSwitches(): Flow<Int>

    @Insert
    suspend fun insertRaceResult(entity: RaceResultEntity): Long

    @Delete
    suspend fun deleteRaceResult(entity: RaceResultEntity)

    @Query("SELECT * FROM race_results WHERE eventId = :eventId ORDER BY timestamp DESC")
    fun raceResultsForEvent(eventId: String): Flow<List<RaceResultEntity>>
}
