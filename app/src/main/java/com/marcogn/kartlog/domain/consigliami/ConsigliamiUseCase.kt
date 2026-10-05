package com.marcogn.kartlog.domain.consigliami

import com.marcogn.kartlog.domain.model.EventType
import com.marcogn.kartlog.domain.model.TrophyRank
import kotlin.math.abs

/**
 * Algoritmo Consigliami (SPEC §6). Con i risultati disattivati (default, fase 6) il punteggio è
 * sempre `score(E) = gain(E, best(E))`; con i risultati attivati (fase 7) entra il peso `w` (§6.3).
 *
 * I cibi di un evento sono quelli degli stand Yoshi's dei suoi percorsi (fase C1): stand nell'area
 * del percorso, non per forza sul tracciato di gara, quindi ogni guadagno è una possibilità.
 */
object ConsigliamiUseCase {

    private const val SCORE_EPSILON = 1e-9

    private data class RawEventScore(
        val event: ConsigliamiEvent,
        val total: Int,
        val best: CharacterGain?,
        val runnersUp: List<CharacterGain>,
        val relevantStops: Int,
        val relevantFoods: List<RelevantFood>,
        val improvement: Double,
    )

    /**
     * @param bestRankForEvent miglior trofeo registrato per un evento alla cilindrata di
     * riferimento scelta in Consigliami; null se nessun risultato (SPEC §6.3,
     * "nessun risultato -> improvement 1"). Ignorato se [resultsEnabled] è false.
     */
    fun compute(
        characters: List<ConsigliamiCharacter>,
        outfits: List<ConsigliamiOutfit>,
        rules: List<ConsigliamiRule>,
        standFoods: List<ConsigliamiStandFood>,
        events: List<ConsigliamiEvent>,
        resultsEnabled: Boolean = false,
        weight: Double = 0.3,
        bestRankForEvent: (eventId: String) -> TrophyRank? = { null },
    ): List<RecommendationGroup> {
        val courseFoods = courseFoods(standFoods)
        val rulesByOutfit: Map<String, Set<String>> =
            rules.groupBy({ it.outfitId }, { it.foodGroupId }).mapValues { it.value.toSet() }
        val missingByCharacter: Map<String, List<ConsigliamiOutfit>> =
            outfits.filterNot { it.owned }.groupBy { it.characterId }
        val unlocked = characters.filter { it.unlocked }

        val raw = events.map { event ->
            scoreEventRaw(event, unlocked, missingByCharacter, rulesByOutfit, courseFoods, bestRankForEvent)
        }
        val maxGain = raw.maxOfOrNull { it.best?.gain ?: 0 } ?: 0

        val scored = raw.map { r ->
            val rawGain = r.best?.gain ?: 0
            val score = if (!resultsEnabled) {
                rawGain.toDouble()
            } else {
                val normGain = if (maxGain == 0) 0.0 else rawGain.toDouble() / maxGain
                (1 - weight) * normGain + weight * r.improvement
            }
            EventScore(r.event, score, r.total, r.best, r.runnersUp, r.relevantStops, r.relevantFoods, r.improvement)
        }

        var comparator = compareByDescending<EventScore> { it.score }
            .thenByDescending { it.total }
            .thenByDescending { it.relevantStops }
        if (resultsEnabled) {
            comparator = comparator.thenByDescending { it.improvement } // worstFirst, SPEC §6.4
        }
        comparator = comparator.thenBy { typeRank(it.event.type) }.thenBy { it.event.order }

        return assignGroups(scored.sortedWith(comparator))
    }

    private fun scoreEventRaw(
        event: ConsigliamiEvent,
        unlocked: List<ConsigliamiCharacter>,
        missingByCharacter: Map<String, List<ConsigliamiOutfit>>,
        rulesByOutfit: Map<String, Set<String>>,
        courseFoods: List<RelevantFood>,
        bestRankForEvent: (eventId: String) -> TrophyRank?,
    ): RawEventScore {
        // foods(E) (SPEC §6.1): un cibo presente su più corsi dello stesso evento resta un solo
        // elemento dell'insieme, quindi conta una sola volta per outfit (Set, non lista).
        val foods = foodsOf(event, courseFoods)

        val gains = unlocked.map { character ->
            val missing = missingByCharacter[character.id].orEmpty()
            val gain = missing.count { outfit -> rulesByOutfit[outfit.id].orEmpty().any { it in foods } }
            CharacterGain(character.id, gain, missing.size, character.rosterOrder)
        }.sortedWith(
            compareByDescending<CharacterGain> { it.gain }
                .thenByDescending { it.missingTotal }
                .thenBy { it.rosterOrder }
        )

        val best = gains.firstOrNull()?.takeIf { it.gain > 0 }
        val runnersUp = gains.drop(1).take(2)
        val total = gains.sumOf { it.gain }

        val relevantFoods = if (best != null) {
            val bestMissingIds = missingByCharacter[best.characterId].orEmpty().map { it.id }.toSet()
            val usefulFoodGroups = rulesByOutfit
                .filterKeys { it in bestMissingIds }
                .values
                .flatten()
                .filter { it in foods }
                .toSet()
            courseFoods.filter { it.courseId in event.courseIds && it.foodGroupId in usefulFoodGroups }
        } else {
            emptyList()
        }
        val relevantStops = relevantFoods.map { it.courseId }.distinct().size
        // SPEC §6.3: bronzo..oro 3 stelle = livelli 1..6, nessun trofeo = 0 -> improvement 1.
        val improvement = 1.0 - (bestRankForEvent(event.id)?.level ?: 0).toDouble() / TrophyRank.MAX_LEVEL

        return RawEventScore(event, total, best, runnersUp, relevantStops, relevantFoods, improvement)
    }

    /**
     * Dettaglio di un evento (SPEC §2.5): tutti i personaggi sbloccati con gain > 0, con gli
     * outfit che quell'evento potrebbe far ottenere e, per ciascuno, i cibi e i percorsi dove
     * si trovano. Ordinato per gain decrescente.
     */
    fun detailFor(
        event: ConsigliamiEvent,
        characters: List<ConsigliamiCharacter>,
        outfits: List<ConsigliamiOutfit>,
        rules: List<ConsigliamiRule>,
        standFoods: List<ConsigliamiStandFood>,
    ): List<CharacterDetail> {
        val eventFoods = courseFoods(standFoods)
            .filter { it.courseId in event.courseIds }
            .sortedBy { event.courseIds.indexOf(it.courseId) }
        val foods = eventFoods.map { it.foodGroupId }.toSet()
        val rulesByOutfit = rules.groupBy({ it.outfitId }, { it.foodGroupId }).mapValues { it.value.toSet() }
        val missingByCharacter = outfits.filterNot { it.owned }.groupBy { it.characterId }

        return characters.filter { it.unlocked }.mapNotNull { character ->
            val unlockable = missingByCharacter[character.id].orEmpty()
                .filter { outfit -> rulesByOutfit[outfit.id].orEmpty().any { it in foods } }
                .map { outfit ->
                    val outfitFoods = rulesByOutfit[outfit.id].orEmpty()
                    UnlockableOutfit(outfit.id, eventFoods.filter { it.foodGroupId in outfitFoods })
                }
            if (unlockable.isEmpty()) null else CharacterDetail(character.id, unlockable.size, unlockable)
        }.sortedByDescending { it.gain }
    }

    /**
     * Coppie (corso, cibo) distinte dagli stand dei percorsi. Gli stand sulle strade ([ConsigliamiStandFood.courseId]
     * null) restano fuori: nessuna fonte dice in quale tratto di un rally si incontrano.
     */
    private fun courseFoods(standFoods: List<ConsigliamiStandFood>): List<RelevantFood> =
        standFoods.mapNotNull { s -> s.courseId?.let { RelevantFood(s.foodGroupId, it) } }.distinct()

    private fun foodsOf(event: ConsigliamiEvent, courseFoods: List<RelevantFood>): Set<String> =
        courseFoods.asSequence().filter { it.courseId in event.courseIds }.map { it.foodGroupId }.toSet()

    private fun typeRank(type: EventType): Int = if (type == EventType.CUP) 0 else 1

    /** Numerazione "competition ranking" (1, 1, 1, 4...): pari merito = stesso score (tolleranza) e stesso total. */
    private fun assignGroups(sorted: List<EventScore>): List<RecommendationGroup> {
        val groups = mutableListOf<RecommendationGroup>()
        var index = 0
        var position = 1
        while (index < sorted.size) {
            val head = sorted[index]
            var end = index
            while (end < sorted.size && scoreEquals(sorted[end].score, head.score) && sorted[end].total == head.total) end++
            val members = sorted.subList(index, end)
            val commonCourseIds = if (members.size > 1) {
                members.map { it.relevantFoods.map { food -> food.courseId }.toSet() }.reduce { a, b -> a intersect b }
            } else {
                emptySet()
            }
            groups += RecommendationGroup(position, members, commonCourseIds)
            position += members.size
            index = end
        }
        return groups
    }

    private fun scoreEquals(a: Double, b: Double): Boolean = abs(a - b) < SCORE_EPSILON
}
