package com.marcogn.kartlog.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.marcogn.kartlog.domain.model.Cc
import com.marcogn.kartlog.domain.model.TrophyRank

/**
 * Stato utente (SPEC §3): mai cancellato o toccato dal reseed dei dati di gioco
 * (vedi [com.marcogn.kartlog.data.seed.SeedRepository]).
 */
@Entity(tableName = "owned_outfits")
data class OwnedOutfitEntity(
    @PrimaryKey val outfitId: String,
)

@Entity(tableName = "character_unlocks")
data class CharacterUnlockEntity(
    @PrimaryKey val characterId: String,
    val unlocked: Boolean,
)

@Entity(tableName = "collected_medallions")
data class CollectedMedallionEntity(
    @PrimaryKey val medallionId: String,
)

@Entity(tableName = "completed_p_switches")
data class CompletedPSwitchEntity(
    @PrimaryKey val pSwitchId: String,
)

/**
 * Miglior risultato per evento e cilindrata (SPEC §2.6): una sola riga per coppia, un nuovo
 * inserimento sostituisce il precedente. Sostituisce lo storico `race_results` dello schema v2
 * (vedi [com.marcogn.kartlog.data.local.MIGRATION_2_3]).
 */
@Entity(tableName = "best_results", primaryKeys = ["eventId", "cc"])
data class BestResultEntity(
    val eventId: String,
    val cc: Cc,
    val rank: TrophyRank,
)
