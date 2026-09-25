package com.marcogn.kartlog.domain.consigliami

import com.marcogn.kartlog.domain.model.EventType
import com.marcogn.kartlog.domain.model.Presence
import kotlin.math.abs

/**
 * Algoritmo Consigliami (SPEC §6), senza registrazione risultati (fase 7): il punteggio è sempre
 * `score(E) = gain(E, best(E))`, i risultati sono considerati sempre disattivati (§6.3), quindi
 * `worstFirst` non entra mai negli spareggi (§6.4).
 */
object ConsigliamiUseCase {

    private const val SCORE_EPSILON = 1e-9

    fun compute(
        characters: List<ConsigliamiCharacter>,
        outfits: List<ConsigliamiOutfit>,
        rules: List<ConsigliamiRule>,
        foodCourses: List<ConsigliamiFoodCourse>,
        events: List<ConsigliamiEvent>,
        includeNearby: Boolean,
    ): List<RecommendationGroup> {
        val presences = if (includeNearby) setOf(Presence.ON_COURSE, Presence.NEARBY) else setOf(Presence.ON_COURSE)
        val rulesByOutfit: Map<String, Set<String>> =
            rules.groupBy({ it.outfitId }, { it.foodGroupId }).mapValues { it.value.toSet() }
        val missingByCharacter: Map<String, List<ConsigliamiOutfit>> =
            outfits.filterNot { it.owned }.groupBy { it.characterId }
        val unlocked = characters.filter { it.unlocked }

        val scored = events.map { event -> scoreEvent(event, unlocked, missingByCharacter, rulesByOutfit, foodCourses, presences) }

        val sorted = scored.sortedWith(
            compareByDescending<EventScore> { it.score }
                .thenByDescending { it.total }
                .thenByDescending { it.relevantStops }
                .thenBy { typeRank(it.event.type) }
                .thenBy { it.event.order }
        )

        return assignGroups(sorted)
    }

    private fun scoreEvent(
        event: ConsigliamiEvent,
        unlocked: List<ConsigliamiCharacter>,
        missingByCharacter: Map<String, List<ConsigliamiOutfit>>,
        rulesByOutfit: Map<String, Set<String>>,
        foodCourses: List<ConsigliamiFoodCourse>,
        presences: Set<Presence>,
    ): EventScore {
        // foods(E) (SPEC §6.1): un cibo presente su più corsi dello stesso evento resta un solo
        // elemento dell'insieme, quindi conta una sola volta per outfit (Set, non lista).
        val foods = foodCourses
            .asSequence()
            .filter { it.courseId in event.courseIds && it.presence in presences }
            .map { it.foodGroupId }
            .toSet()

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
        val score = best?.gain?.toDouble() ?: 0.0

        val relevantFoods = if (best != null) {
            val bestMissingIds = missingByCharacter[best.characterId].orEmpty().map { it.id }.toSet()
            val usefulFoodGroups = rulesByOutfit
                .filterKeys { it in bestMissingIds }
                .values
                .flatten()
                .filter { it in foods }
                .toSet()
            foodCourses
                .filter { it.courseId in event.courseIds && it.presence in presences && it.foodGroupId in usefulFoodGroups }
                .map { RelevantFood(it.foodGroupId, it.courseId, it.presence) }
                .distinct()
        } else {
            emptyList()
        }
        val relevantStops = relevantFoods.map { it.courseId }.distinct().size

        return EventScore(event, score, total, best, runnersUp, relevantStops, relevantFoods)
    }

    /**
     * Dettaglio di un evento (SPEC §2.5): tutti i personaggi sbloccati con gain > 0, con gli
     * outfit specifici che quell'evento permette di ottenere. Ordinato per gain decrescente.
     */
    fun detailFor(
        event: ConsigliamiEvent,
        characters: List<ConsigliamiCharacter>,
        outfits: List<ConsigliamiOutfit>,
        rules: List<ConsigliamiRule>,
        foodCourses: List<ConsigliamiFoodCourse>,
        includeNearby: Boolean,
    ): List<CharacterDetail> {
        val presences = if (includeNearby) setOf(Presence.ON_COURSE, Presence.NEARBY) else setOf(Presence.ON_COURSE)
        val foods = foodCourses
            .filter { it.courseId in event.courseIds && it.presence in presences }
            .map { it.foodGroupId }
            .toSet()
        val rulesByOutfit = rules.groupBy({ it.outfitId }, { it.foodGroupId }).mapValues { it.value.toSet() }
        val missingByCharacter = outfits.filterNot { it.owned }.groupBy { it.characterId }

        return characters.filter { it.unlocked }.mapNotNull { character ->
            val unlockable = missingByCharacter[character.id].orEmpty()
                .filter { outfit -> rulesByOutfit[outfit.id].orEmpty().any { it in foods } }
            if (unlockable.isEmpty()) null else CharacterDetail(character.id, unlockable.size, unlockable.map { it.id })
        }.sortedByDescending { it.gain }
    }

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
