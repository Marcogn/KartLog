package com.marcogn.kartlog.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.marcogn.kartlog.domain.model.Cc

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

@Entity(tableName = "race_results")
data class RaceResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: String,
    val cc: Cc,
    val stars: Int,
    val placement: Int?,
    val eliminatedAt: Int?,
    val characterId: String?,
    val timestamp: Long,
)
