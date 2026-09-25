package com.marcogn.kartlog.data.backup

import kotlinx.serialization.Serializable

/**
 * Formato di backup dello stato utente (SPEC §4) — mai dei dati di gioco, quelli vengono da
 * `seed/`. Versionato fin da subito: un cambio di forma incrementa [BackupPayload.backupVersion].
 */
@Serializable
data class BackupPayload(
    val backupVersion: Int = CURRENT_BACKUP_VERSION,
    val exportedAt: Long,
    val ownedOutfitIds: List<String> = emptyList(),
    val characterUnlocks: List<CharacterUnlockDto> = emptyList(),
    val collectedMedallionIds: List<String> = emptyList(),
    val completedPSwitchIds: List<String> = emptyList(),
    /** Solo backup v1 (storico con stelle e posizione): letto all'import e convertito, mai più scritto. */
    val raceResults: List<RaceResultDto> = emptyList(),
    /** Dalla v2: miglior risultato per evento e cilindrata (SPEC §2.6). */
    val bestResults: List<BestResultDto> = emptyList(),
)

/** v2: `raceResults` (storico) sostituito da `bestResults` (un trofeo per evento e cilindrata). */
const val CURRENT_BACKUP_VERSION = 2

@Serializable
data class CharacterUnlockDto(val characterId: String, val unlocked: Boolean)

@Serializable
data class BestResultDto(
    val eventId: String,
    /** Nome dell'enum [com.marcogn.kartlog.domain.model.Cc] (es. "CC_150"). */
    val cc: String,
    /** Nome dell'enum [com.marcogn.kartlog.domain.model.TrophyRank] (es. "GOLD_2_STARS"). */
    val rank: String,
)

/** Formato v1, solo in lettura: convertito con `TrophyRank.fromLegacy`. */
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
