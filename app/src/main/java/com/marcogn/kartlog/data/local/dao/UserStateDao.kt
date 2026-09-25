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

    /** Null = nessuna riga: SPEC §2.3, un personaggio è sbloccato di default. */
    @Query("SELECT unlocked FROM character_unlocks WHERE characterId = :characterId")
    fun observeCharacterUnlock(characterId: String): Flow<Boolean?>

    /**
     * L'outfit di default non è spuntabile ed è sempre posseduto (SPEC §2.3): garantisce che ogni
     * outfit di default abbia una riga in [OwnedOutfitEntity], senza toccare gli altri. Idempotente,
     * va richiamata a ogni avvio (vedi [com.marcogn.kartlog.data.seed.SeedRepository]).
     */
    @Query(
        """
        INSERT OR IGNORE INTO owned_outfits (outfitId)
        SELECT id FROM outfits WHERE isDefault = 1
        """
    )
    suspend fun ensureDefaultOutfitsOwned()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markMedallionCollected(entity: CollectedMedallionEntity)

    @Query("DELETE FROM collected_medallions WHERE medallionId = :medallionId")
    suspend fun markMedallionNotCollected(medallionId: String)

    @Query("SELECT COUNT(*) FROM collected_medallions")
    fun countCollectedMedallions(): Flow<Int>

    /** "Segna tutti" di una regione (SPEC §2.4): non tocca i medaglioni già segnati altrove. */
    @Query(
        """
        INSERT OR IGNORE INTO collected_medallions (medallionId)
        SELECT id FROM peach_medallions WHERE regionId = :regionId
        """
    )
    suspend fun markAllMedallionsCollected(regionId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markPSwitchCompleted(entity: CompletedPSwitchEntity)

    @Query("DELETE FROM completed_p_switches WHERE pSwitchId = :pSwitchId")
    suspend fun markPSwitchNotCompleted(pSwitchId: String)

    @Query("SELECT COUNT(*) FROM completed_p_switches")
    fun countCompletedPSwitches(): Flow<Int>

    /** "Segna tutti" di una regione (SPEC §2.4). */
    @Query(
        """
        INSERT OR IGNORE INTO completed_p_switches (pSwitchId)
        SELECT id FROM p_switches WHERE regionId = :regionId
        """
    )
    suspend fun markAllPSwitchesCompleted(regionId: String)

    @Insert
    suspend fun insertRaceResult(entity: RaceResultEntity): Long

    @Delete
    suspend fun deleteRaceResult(entity: RaceResultEntity)

    @Query("SELECT * FROM race_results WHERE eventId = :eventId ORDER BY timestamp DESC")
    fun raceResultsForEvent(eventId: String): Flow<List<RaceResultEntity>>
}
