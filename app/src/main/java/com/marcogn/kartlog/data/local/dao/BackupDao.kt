package com.marcogn.kartlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.marcogn.kartlog.data.local.entity.CharacterUnlockEntity
import com.marcogn.kartlog.data.local.entity.CollectedMedallionEntity
import com.marcogn.kartlog.data.local.entity.CompletedPSwitchEntity
import com.marcogn.kartlog.data.local.entity.OwnedOutfitEntity
import com.marcogn.kartlog.data.local.entity.RaceResultEntity

/** Lettura/scrittura in blocco dello stato utente per l'export/import di backup (SPEC §4). */
@Dao
interface BackupDao {

    @Query("SELECT * FROM owned_outfits")
    suspend fun ownedOutfits(): List<OwnedOutfitEntity>

    @Query("SELECT * FROM character_unlocks")
    suspend fun characterUnlocks(): List<CharacterUnlockEntity>

    @Query("SELECT * FROM collected_medallions")
    suspend fun collectedMedallions(): List<CollectedMedallionEntity>

    @Query("SELECT * FROM completed_p_switches")
    suspend fun completedPSwitches(): List<CompletedPSwitchEntity>

    @Query("SELECT * FROM race_results")
    suspend fun raceResults(): List<RaceResultEntity>

    // ID validi contro il seed corrente (SPEC §4): l'import li usa per riconoscere le righe
    // sconosciute invece di scartarle in silenzio.
    @Query("SELECT id FROM outfits")
    suspend fun validOutfitIds(): List<String>

    @Query("SELECT id FROM characters")
    suspend fun validCharacterIds(): List<String>

    @Query("SELECT id FROM peach_medallions")
    suspend fun validMedallionIds(): List<String>

    @Query("SELECT id FROM p_switches")
    suspend fun validPSwitchIds(): List<String>

    @Query("SELECT id FROM events")
    suspend fun validEventIds(): List<String>

    @Query("DELETE FROM owned_outfits")
    suspend fun clearOwnedOutfits()

    @Query("DELETE FROM character_unlocks")
    suspend fun clearCharacterUnlocks()

    @Query("DELETE FROM collected_medallions")
    suspend fun clearCollectedMedallions()

    @Query("DELETE FROM completed_p_switches")
    suspend fun clearCompletedPSwitches()

    @Query("DELETE FROM race_results")
    suspend fun clearRaceResults()

    @Insert
    suspend fun insertOwnedOutfits(items: List<OwnedOutfitEntity>)

    @Insert
    suspend fun insertCharacterUnlocks(items: List<CharacterUnlockEntity>)

    @Insert
    suspend fun insertCollectedMedallions(items: List<CollectedMedallionEntity>)

    @Insert
    suspend fun insertCompletedPSwitches(items: List<CompletedPSwitchEntity>)

    @Insert
    suspend fun insertRaceResults(items: List<RaceResultEntity>)

    /**
     * Un import sostituisce interamente lo stato utente con quello del backup (com'è già per il
     * ripristino di ThePatientGamerHelper): "l'import ripristina tutto", non lo unisce a quello
     * presente. Gli outfit di default rientrano da soli al prossimo avvio se mancanti
     * (`SeedRepository.ensureDefaultOutfitsOwned()`, idempotente).
     */
    @Transaction
    suspend fun replaceUserState(
        ownedOutfits: List<OwnedOutfitEntity>,
        characterUnlocks: List<CharacterUnlockEntity>,
        collectedMedallions: List<CollectedMedallionEntity>,
        completedPSwitches: List<CompletedPSwitchEntity>,
        raceResults: List<RaceResultEntity>,
    ) {
        clearOwnedOutfits()
        clearCharacterUnlocks()
        clearCollectedMedallions()
        clearCompletedPSwitches()
        clearRaceResults()
        insertOwnedOutfits(ownedOutfits)
        insertCharacterUnlocks(characterUnlocks)
        insertCollectedMedallions(collectedMedallions)
        insertCompletedPSwitches(completedPSwitches)
        insertRaceResults(raceResults)
    }
}
