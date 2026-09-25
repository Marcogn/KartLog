package com.marcogn.kartlog.data.backup

import kotlinx.serialization.Serializable

/**
 * Formato di backup dello stato utente (SPEC §4) — mai dei dati di gioco, quelli vengono da
 * `seed/`. Versionato fin da subito: un cambio di forma incrementa [BackupPayload.backupVersion].
 */
@Serializable
data class BackupPayload(
    val backupVersion: Int = 1,
    val exportedAt: Long,
    val ownedOutfitIds: List<String> = emptyList(),
    val characterUnlocks: List<CharacterUnlockDto> = emptyList(),
    val collectedMedallionIds: List<String> = emptyList(),
    val completedPSwitchIds: List<String> = emptyList(),
    val raceResults: List<RaceResultDto> = emptyList(),
)

@Serializable
data class CharacterUnlockDto(val characterId: String, val unlocked: Boolean)

@Serializable
data class RaceResultDto(
    val eventId: String,
    /** Nome dell'enum [com.marcogn.kartlog.domain.model.Cc] (es. "CC_150"). */
    val cc: String,
    val stars: Int,
    val placement: Int? = null,
    val eliminatedAt: Int? = null,
    val characterId: String? = null,
    val timestamp: Long,
)

/**
 * Esito di un import (SPEC §8, "fatto quando"): gli ID sconosciuti si riportano, non si scartano
 * in silenzio. Le chiavi sono etichette leggibili (non i nomi delle tabelle).
 */
data class ImportResult(
    val appliedCounts: Map<String, Int>,
    val unknownIds: Map<String, List<String>>,
)
