package com.marcogn.kartlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.marcogn.kartlog.data.local.entity.BestResultEntity
import com.marcogn.kartlog.data.local.entity.CharacterUnlockEntity
import com.marcogn.kartlog.data.local.entity.CollectedMedallionEntity
import com.marcogn.kartlog.data.local.entity.CompletedPSwitchEntity
import com.marcogn.kartlog.data.local.entity.OwnedOutfitEntity
import com.marcogn.kartlog.domain.model.Cc
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

    /** +1 sul contatore di un bioma: segna il primo "posto" libero (vedi [MedallionsDao.regionCounters]). */
    @Query(
        """
        INSERT OR IGNORE INTO collected_medallions (medallionId)
        SELECT id FROM peach_medallions
        WHERE regionId = :regionId AND id NOT IN (SELECT medallionId FROM collected_medallions)
        ORDER BY "index" ASC LIMIT 1
        """
    )
    suspend fun collectNextMedallion(regionId: String)

    /** -1 sul contatore di un bioma: libera l'ultimo "posto" segnato. */
    @Query(
        """
        DELETE FROM collected_medallions WHERE medallionId = (
            SELECT cm.medallionId FROM collected_medallions cm
            JOIN peach_medallions m ON m.id = cm.medallionId
            WHERE m.regionId = :regionId
            ORDER BY m."index" DESC LIMIT 1
        )
        """
    )
    suspend fun uncollectLastMedallion(regionId: String)

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

    /** Un nuovo miglior risultato per (evento, cilindrata) sostituisce il precedente (SPEC §2.6). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBestResult(entity: BestResultEntity)

    @Query("DELETE FROM best_results WHERE eventId = :eventId AND cc = :cc")
    suspend fun deleteBestResult(eventId: String, cc: Cc)

    @Query("SELECT * FROM best_results WHERE eventId = :eventId")
    fun bestResultsForEvent(eventId: String): Flow<List<BestResultEntity>>

    /** Eventi con almeno un trofeo a una qualsiasi cilindrata, per il contatore della Home. */
    @Query("SELECT COUNT(DISTINCT eventId) FROM best_results WHERE eventId IN (SELECT id FROM events)")
    fun countEventsWithResult(): Flow<Int>

    /** Tutti i migliori risultati, per calcolare `bestRank(E, cc)` (SPEC §6.3) su tutti gli eventi in una volta. */
    @Query("SELECT * FROM best_results")
    fun allBestResults(): Flow<List<BestResultEntity>>
}
