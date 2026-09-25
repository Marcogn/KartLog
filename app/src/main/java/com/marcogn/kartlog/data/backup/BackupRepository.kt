package com.marcogn.kartlog.data.backup

import android.content.Context
import android.net.Uri
import com.marcogn.kartlog.data.local.dao.BackupDao
import com.marcogn.kartlog.data.local.entity.BestResultEntity
import com.marcogn.kartlog.data.local.entity.CharacterUnlockEntity
import com.marcogn.kartlog.data.local.entity.CollectedMedallionEntity
import com.marcogn.kartlog.data.local.entity.CompletedPSwitchEntity
import com.marcogn.kartlog.data.local.entity.OwnedOutfitEntity
import com.marcogn.kartlog.domain.model.Cc
import com.marcogn.kartlog.domain.model.TrophyRank
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.serialization.json.Json

/** Export/import dello stato utente tramite Storage Access Framework (SPEC §4). */
class BackupRepository @Inject constructor(
    private val dao: BackupDao,
    @ApplicationContext private val context: Context,
) {
    // encodeDefaults: `backupVersion` va scritto anche quando coincide col default, altrimenti il file non lo porta.
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun export(destination: Uri) {
        val payload = BackupPayload(
            exportedAt = System.currentTimeMillis(),
            ownedOutfitIds = dao.ownedOutfits().map { it.outfitId },
            characterUnlocks = dao.characterUnlocks().map { CharacterUnlockDto(it.characterId, it.unlocked) },
            collectedMedallionIds = dao.collectedMedallions().map { it.medallionId },
            completedPSwitchIds = dao.completedPSwitches().map { it.pSwitchId },
            bestResults = dao.bestResults().map { BestResultDto(it.eventId, it.cc.name, it.rank.name) },
        )
        val text = json.encodeToString(BackupPayload.serializer(), payload)
        val stream = context.contentResolver.openOutputStream(destination)
            ?: throw IOException("Impossibile aprire $destination in scrittura")
        stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    suspend fun import(source: Uri): ImportResult {
        val stream = context.contentResolver.openInputStream(source)
            ?: throw IOException("Impossibile aprire $source in lettura")
        val text = stream.use { it.bufferedReader().readText() }
        val payload = json.decodeFromString(BackupPayload.serializer(), text)

        val validOutfitIds = dao.validOutfitIds().toSet()
        val validCharacterIds = dao.validCharacterIds().toSet()
        val validMedallionIds = dao.validMedallionIds().toSet()
        val validPSwitchIds = dao.validPSwitchIds().toSet()
        val validEventIds = dao.validEventIds().toSet()

        val (validOutfits, unknownOutfits) = payload.ownedOutfitIds.partition { it in validOutfitIds }
        val (validUnlocks, unknownUnlocks) = payload.characterUnlocks.partition { it.characterId in validCharacterIds }
        val (validMedallions, unknownMedallions) = payload.collectedMedallionIds.partition { it in validMedallionIds }
        val (validPSwitches, unknownPSwitches) = payload.completedPSwitchIds.partition { it in validPSwitchIds }
        val (validResults, unknownResults) = bestResultsOf(payload).partition { it.eventId in validEventIds }

        dao.replaceUserState(
            ownedOutfits = validOutfits.map { OwnedOutfitEntity(it) },
            characterUnlocks = validUnlocks.map { CharacterUnlockEntity(it.characterId, it.unlocked) },
            collectedMedallions = validMedallions.map { CollectedMedallionEntity(it) },
            completedPSwitches = validPSwitches.map { CompletedPSwitchEntity(it) },
            bestResults = validResults,
        )

        return ImportResult(
            appliedCounts = mapOf(
                "outfits" to validOutfits.size,
                "characterUnlocks" to validUnlocks.size,
                "medallions" to validMedallions.size,
                "pSwitches" to validPSwitches.size,
                "bestResults" to validResults.size,
            ),
            unknownIds = buildMap {
                if (unknownOutfits.isNotEmpty()) put("outfits", unknownOutfits)
                if (unknownUnlocks.isNotEmpty()) put("characterUnlocks", unknownUnlocks.map { it.characterId })
                if (unknownMedallions.isNotEmpty()) put("medallions", unknownMedallions)
                if (unknownPSwitches.isNotEmpty()) put("pSwitches", unknownPSwitches)
                if (unknownResults.isNotEmpty()) put("bestResults", unknownResults.map { it.eventId })
            },
        )
    }

    /**
     * Risultati del backup nel modello attuale. Un backup v1 porta ancora lo storico `raceResults`:
     * si converte con la stessa regola della migrazione Room v2 -> v3 (`TrophyRank.fromLegacy`),
     * tenendo per ogni (evento, cilindrata) il trofeo più alto. Le corse senza trofeo non generano righe.
     */
    private fun bestResultsOf(payload: BackupPayload): List<BestResultEntity> {
        val current = payload.bestResults.map { BestResultEntity(it.eventId, Cc.valueOf(it.cc), TrophyRank.valueOf(it.rank)) }
        val legacy = payload.raceResults.mapNotNull { dto ->
            TrophyRank.fromLegacy(dto.placement, dto.stars)?.let { BestResultEntity(dto.eventId, Cc.valueOf(dto.cc), it) }
        }
        return (current + legacy)
            .groupBy { it.eventId to it.cc }
            .map { (_, rows) -> rows.maxBy { it.rank.level } }
    }
}
