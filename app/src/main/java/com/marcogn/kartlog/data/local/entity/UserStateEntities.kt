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

@Entity(tableName = "activated_question_panels")
data class ActivatedQuestionPanelEntity(
    @PrimaryKey val panelId: String,
)

/**
 * Avvisi da mostrare una volta sola, scritti da una migrazione (es. [com.marcogn.kartlog.data.local.MIGRATION_6_7]:
 * quante monete segnate per bioma non si sono potute convertire). Si cancella la riga quando l'utente
 * chiude l'avviso.
 */
@Entity(tableName = "pending_notices")
data class PendingNoticeEntity(
    @PrimaryKey val id: String,
    val count: Int,
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
