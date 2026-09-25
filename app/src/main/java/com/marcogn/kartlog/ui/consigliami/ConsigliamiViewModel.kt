package com.marcogn.kartlog.ui.consigliami

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcogn.kartlog.data.local.dao.ConsigliamiCharacterRow
import com.marcogn.kartlog.data.local.dao.ConsigliamiDao
import com.marcogn.kartlog.data.local.dao.ConsigliamiOutfitRow
import com.marcogn.kartlog.data.local.dao.IdName
import com.marcogn.kartlog.data.local.dao.UserStateDao
import com.marcogn.kartlog.data.local.entity.EventEntity
import com.marcogn.kartlog.data.local.entity.EventStopEntity
import com.marcogn.kartlog.data.local.entity.FoodGroupCourseEntity
import com.marcogn.kartlog.data.local.entity.OutfitFoodRuleEntity
import com.marcogn.kartlog.data.local.entity.RaceResultEntity
import com.marcogn.kartlog.domain.consigliami.ConsigliamiCharacter
import com.marcogn.kartlog.domain.consigliami.ConsigliamiEvent
import com.marcogn.kartlog.domain.consigliami.ConsigliamiFoodCourse
import com.marcogn.kartlog.domain.consigliami.ConsigliamiOutfit
import com.marcogn.kartlog.domain.consigliami.ConsigliamiRule
import com.marcogn.kartlog.domain.consigliami.ConsigliamiUseCase
import com.marcogn.kartlog.domain.consigliami.RecommendationGroup
import com.marcogn.kartlog.domain.model.Cc
import com.marcogn.kartlog.domain.model.EventType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ConsigliamiEventFilter { CUP, RALLY, BOTH }

data class ConsigliamiUiState(
    val groups: List<RecommendationGroup> = emptyList(),
    val eventFilter: ConsigliamiEventFilter = ConsigliamiEventFilter.BOTH,
    val includeNearby: Boolean = false,
    val onlyUseful: Boolean = true,
    val resultsEnabled: Boolean = false,
    val weight: Double = 0.3,
    val referenceCc: Cc = Cc.CC_150,
    val bestResultByEvent: Map<String, RaceResultEntity> = emptyMap(),
    val characterNames: Map<String, String> = emptyMap(),
    val courseNames: Map<String, String> = emptyMap(),
    val foodGroupNames: Map<String, String> = emptyMap(),
    val allEvents: List<EventPickerItem> = emptyList(),
)

private data class RawSeedData(
    val characters: List<ConsigliamiCharacterRow>,
    val outfits: List<ConsigliamiOutfitRow>,
    val rules: List<OutfitFoodRuleEntity>,
    val foodCourses: List<FoodGroupCourseEntity>,
    val events: List<EventEntity>,
    val eventStops: List<EventStopEntity>,
    val courseNames: Map<String, String>,
    val foodGroupNames: Map<String, String>,
)

private data class ResultsSettings(val enabled: Boolean, val weight: Double, val cc: Cc, val raceResults: List<RaceResultEntity>)

@HiltViewModel
class ConsigliamiViewModel @Inject constructor(
    dao: ConsigliamiDao,
    private val userStateDao: UserStateDao,
) : ViewModel() {

    private val eventFilter = MutableStateFlow(ConsigliamiEventFilter.BOTH)
    private val includeNearby = MutableStateFlow(false)
    private val onlyUseful = MutableStateFlow(true)
    private val resultsEnabled = MutableStateFlow(false)
    private val weight = MutableStateFlow(0.3)
    private val referenceCc = MutableStateFlow(Cc.CC_150)

    private val rawData = combine(
        combine(dao.characters(), dao.outfits(), dao.rules()) { c, o, r -> Triple(c, o, r) },
        combine(dao.foodCourses(), dao.events(), dao.eventStops()) { fc, e, es -> Triple(fc, e, es) },
        combine(dao.courseNames(), dao.foodGroupNames()) { cn, fgn -> cn.toNameMap() to fgn.toNameMap() },
    ) { (characters, outfits, rules), (foodCourses, events, eventStops), (courseNames, foodGroupNames) ->
        RawSeedData(characters, outfits, rules, foodCourses, events, eventStops, courseNames, foodGroupNames)
    }

    private val resultsSettings = combine(
        resultsEnabled,
        weight,
        referenceCc,
        userStateDao.allRaceResults(),
    ) { enabled, w, cc, results -> ResultsSettings(enabled, w, cc, results) }

    val uiState: StateFlow<ConsigliamiUiState> = combine(
        rawData,
        eventFilter,
        includeNearby,
        onlyUseful,
        resultsSettings,
    ) { raw, filter, nearby, useful, results ->
        val eventStopsByEvent = raw.eventStops.groupBy({ it.eventId }, { it.courseId })
        val events = raw.events
            .filter { filter == ConsigliamiEventFilter.BOTH || it.type == filter.toEventType() }
            .map { ConsigliamiEvent(it.id, it.type, it.name, it.order, eventStopsByEvent[it.id].orEmpty()) }

        // bestStars(E, cc) (SPEC §6.3): solo i risultati alla cilindrata di riferimento.
        val resultsAtCc = results.raceResults.filter { it.cc == results.cc }
        val bestStarsByEvent = resultsAtCc.groupBy { it.eventId }.mapValues { (_, rows) -> rows.maxOf { it.stars } }
        val bestResultByEvent = resultsAtCc.groupBy { it.eventId }.mapValues { (_, rows) ->
            rows.sortedWith(compareByDescending<RaceResultEntity> { it.stars }.thenBy { it.placement ?: Int.MAX_VALUE }).first()
        }

        var groups = ConsigliamiUseCase.compute(
            characters = raw.characters.map { ConsigliamiCharacter(it.id, it.rosterOrder, it.unlocked) },
            outfits = raw.outfits.map { ConsigliamiOutfit(it.id, it.characterId, it.owned) },
            rules = raw.rules.map { ConsigliamiRule(it.outfitId, it.foodGroupId) },
            foodCourses = raw.foodCourses.map { ConsigliamiFoodCourse(it.foodGroupId, it.courseId, it.presence) },
            events = events,
            includeNearby = nearby,
            resultsEnabled = results.enabled,
            weight = results.weight,
            bestStarsForEvent = { eventId -> bestStarsByEvent[eventId] },
        )

        // Filtro "Solo utili" (SPEC §6.3, default on): nasconde gli eventi a punteggio 0, mai i
        // gruppi che restano con almeno un evento utile.
        if (useful) {
            groups = groups.mapNotNull { group ->
                val kept = group.events.filter { it.score > 0 }
                if (kept.isEmpty()) null else group.copy(events = kept)
            }
        }

        ConsigliamiUiState(
            groups = groups,
            eventFilter = filter,
            includeNearby = nearby,
            onlyUseful = useful,
            resultsEnabled = results.enabled,
            weight = results.weight,
            referenceCc = results.cc,
            bestResultByEvent = bestResultByEvent,
            characterNames = raw.characters.associate { it.id to it.name },
            courseNames = raw.courseNames,
            foodGroupNames = raw.foodGroupNames,
            // Non filtrata da eventFilter/"Solo utili": la registrazione risultato deve poter
            // raggiungere qualunque evento, anche quelli nascosti al momento dalla lista.
            allEvents = raw.events.sortedBy { it.order }.map { EventPickerItem(it.id, it.name, it.type) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConsigliamiUiState())

    fun onEventFilterChanged(filter: ConsigliamiEventFilter) {
        eventFilter.value = filter
    }

    fun onIncludeNearbyChanged(value: Boolean) {
        includeNearby.value = value
    }

    fun onOnlyUsefulChanged(value: Boolean) {
        onlyUseful.value = value
    }

    fun onResultsEnabledChanged(value: Boolean) {
        resultsEnabled.value = value
    }

    fun onWeightChanged(value: Double) {
        weight.value = value
    }

    fun onReferenceCcChanged(value: Cc) {
        referenceCc.value = value
    }

    /** Punto di ingresso globale (SPEC §2.6): registra un risultato per un evento a scelta, non solo quello sotto una card visibile. */
    fun onResultSaved(eventId: String, cc: Cc, stars: Int, placement: Int?, eliminatedAt: Int?, characterId: String?) {
        viewModelScope.launch {
            userStateDao.insertRaceResult(
                RaceResultEntity(
                    eventId = eventId,
                    cc = cc,
                    stars = stars,
                    placement = placement,
                    eliminatedAt = eliminatedAt,
                    characterId = characterId,
                    timestamp = System.currentTimeMillis(),
                )
            )
        }
    }
}

private fun ConsigliamiEventFilter.toEventType(): EventType? = when (this) {
    ConsigliamiEventFilter.CUP -> EventType.CUP
    ConsigliamiEventFilter.RALLY -> EventType.RALLY
    ConsigliamiEventFilter.BOTH -> null
}

private fun List<IdName>.toNameMap(): Map<String, String> = associate { it.id to it.name }
