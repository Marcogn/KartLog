package com.marcogn.kartlog.domain.consigliami

import com.marcogn.kartlog.domain.model.EventType

// Input puri (SPEC §6): niente dipendenze Android/Room, solo i fatti che servono all'algoritmo.

data class ConsigliamiCharacter(val id: String, val rosterOrder: Int, val unlocked: Boolean)

/** Solo outfit non default (SPEC §6.2, "missing(C) = outfit di C non default e non posseduti"). */
data class ConsigliamiOutfit(val id: String, val characterId: String, val owned: Boolean)

data class ConsigliamiRule(val outfitId: String, val foodGroupId: String)

/**
 * Un cibo di uno stand Yoshi's (SPEC §6.1). [courseId] null = stand su una strada tra i percorsi:
 * non appartiene a nessun evento e non dà mai gain (decisione dell'autore del 05/10/2026).
 */
data class ConsigliamiStandFood(val courseId: String?, val foodGroupId: String)

data class ConsigliamiEvent(val id: String, val type: EventType, val name: String, val order: Int, val courseIds: List<String>)

// Output.

data class CharacterGain(val characterId: String, val gain: Int, val missingTotal: Int, val rosterOrder: Int)

/** Un cibo che potrebbe esserci su un percorso dell'evento: c'è uno stand nell'area, non per forza sul tracciato. */
data class RelevantFood(val foodGroupId: String, val courseId: String)

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

/**
 * Dettaglio evento (SPEC §2.5, "tap sulla card apre il dettaglio"): un personaggio con gain > 0 e
 * gli outfit che potrebbe sbloccare, ciascuno con i cibi e i percorsi dove si trovano (righe
 * "outfit – cibo – percorso" della fase C4).
 */
data class CharacterDetail(val characterId: String, val gain: Int, val outfits: List<UnlockableOutfit>) {
    val unlockableOutfitIds: List<String> get() = outfits.map { it.outfitId }
}

/** Un outfit mancante e dove potrebbe sbloccarsi in quell'evento: [sources] in ordine di tappa, poi di cibo. */
data class UnlockableOutfit(val outfitId: String, val sources: List<RelevantFood>)
