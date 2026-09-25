package com.marcogn.kartlog.domain.consigliami

import com.marcogn.kartlog.domain.model.EventType
import com.marcogn.kartlog.domain.model.Presence

// Input puri (SPEC §6): niente dipendenze Android/Room, solo i fatti che servono all'algoritmo.

data class ConsigliamiCharacter(val id: String, val rosterOrder: Int, val unlocked: Boolean)

/** Solo outfit non default (SPEC §6.2, "missing(C) = outfit di C non default e non posseduti"). */
data class ConsigliamiOutfit(val id: String, val characterId: String, val owned: Boolean)

data class ConsigliamiRule(val outfitId: String, val foodGroupId: String)

data class ConsigliamiFoodCourse(val foodGroupId: String, val courseId: String, val presence: Presence)

data class ConsigliamiEvent(val id: String, val type: EventType, val name: String, val order: Int, val courseIds: List<String>)

// Output.

data class CharacterGain(val characterId: String, val gain: Int, val missingTotal: Int, val rosterOrder: Int)

data class RelevantFood(val foodGroupId: String, val courseId: String, val presence: Presence)

data class EventScore(
    val event: ConsigliamiEvent,
    val score: Double,
    val total: Int,
    val best: CharacterGain?,
    val runnersUp: List<CharacterGain>,
    val relevantStops: Int,
    val relevantFoods: List<RelevantFood>,
    /** SPEC §6.3/§6.4: `1 - bestRank.level/6` (1 se nessun risultato). Spareggio solo a risultati attivi. */
    val improvement: Double,
)

/** Un gruppo di eventi a pari merito (SPEC §6.4): `commonCourseIds` è vuoto se [events] ha un solo elemento. */
data class RecommendationGroup(val position: Int, val events: List<EventScore>, val commonCourseIds: Set<String>)

/** Dettaglio evento (SPEC §2.5, "tap sulla card apre il dettaglio"): un personaggio con gain > 0 e i suoi outfit specifici. */
data class CharacterDetail(val characterId: String, val gain: Int, val unlockableOutfitIds: List<String>)
