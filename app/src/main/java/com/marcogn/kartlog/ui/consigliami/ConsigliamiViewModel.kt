package com.marcogn.kartlog.ui.consigliami

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcogn.kartlog.data.local.dao.ConsigliamiCharacterRow
import com.marcogn.kartlog.data.local.dao.ConsigliamiDao
import com.marcogn.kartlog.data.local.dao.ConsigliamiOutfitRow
import com.marcogn.kartlog.data.local.dao.CourseFoodRow
import com.marcogn.kartlog.data.local.dao.IdNameIt
import com.marcogn.kartlog.data.local.dao.UserStateDao
import com.marcogn.kartlog.data.local.entity.BestResultEntity
import com.marcogn.kartlog.data.local.entity.EventEntity
import com.marcogn.kartlog.data.local.entity.EventStopEntity
import com.marcogn.kartlog.data.local.entity.OutfitFoodRuleEntity
import com.marcogn.kartlog.domain.consigliami.ConsigliamiCharacter
import com.marcogn.kartlog.domain.consigliami.ConsigliamiEvent
import com.marcogn.kartlog.domain.consigliami.ConsigliamiOutfit
import com.marcogn.kartlog.domain.consigliami.ConsigliamiRule
import com.marcogn.kartlog.domain.consigliami.ConsigliamiStandFood
import com.marcogn.kartlog.domain.consigliami.ConsigliamiUseCase
import com.marcogn.kartlog.domain.consigliami.EventScore
import com.marcogn.kartlog.domain.model.Cc
import com.marcogn.kartlog.domain.model.EventType
import com.marcogn.kartlog.domain.model.TrophyRank
import com.marcogn.kartlog.domain.model.localizedName
import com.marcogn.kartlog.domain.model.relocalizing
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class ConsigliamiEventFilter { CUP, RALLY, BOTH }

data class ConsigliamiUiState(
    /** In ordine di classifica: la posizione è l'indice + 1 (SPEC §6.4). */
    val events: List<EventScore> = emptyList(),
    val eventFilter: ConsigliamiEventFilter = ConsigliamiEventFilter.BOTH,
    val resultsEnabled: Boolean = false,
    val weight: Double = 0.3,
    val referenceCc: Cc = Cc.CC_150,
    /** Trofeo registrato alla cilindrata di riferimento (SPEC §6.3). */
    val bestRankByEvent: Map<String, TrophyRank> = emptyMap(),
    val characterNames: Map<String, String> = emptyMap(),
    /** URL delle immagini (CDN del wiki, scaricate a runtime); assenti = segnaposto con iniziali. */
    val characterImages: Map<String, String> = emptyMap(),
    val eventImages: Map<String, String> = emptyMap(),
    val courseNames: Map<String, String> = emptyMap(),
    val foodGroupNames: Map<String, String> = emptyMap(),
)

private data class RawSeedData(
    val characters: List<ConsigliamiCharacterRow>,
    val outfits: List<ConsigliamiOutfitRow>,
    val rules: List<OutfitFoodRuleEntity>,
    val courseFoods: List<CourseFoodRow>,
    val events: List<EventEntity>,
    val eventStops: List<EventStopEntity>,
    val courseNames: Map<String, String>,
    val foodGroupNames: Map<String, String>,
)

private data class ResultsSettings(val enabled: Boolean, val weight: Double, val cc: Cc, val bestResults: List<BestResultEntity>)

@HiltViewModel
class ConsigliamiViewModel @Inject constructor(
    dao: ConsigliamiDao,
    userStateDao: UserStateDao,
) : ViewModel() {

    private val eventFilter = MutableStateFlow(ConsigliamiEventFilter.BOTH)
    private val resultsEnabled = MutableStateFlow(false)
    private val weight = MutableStateFlow(0.3)
    private val referenceCc = MutableStateFlow(Cc.CC_150)

    private val rawData = combine(
        combine(dao.characters(), dao.outfits(), dao.rules()) { c, o, r -> Triple(c, o, r) },
        combine(dao.courseFoods(), dao.events(), dao.eventStops()) { cf, e, es -> Triple(cf, e, es) },
        combine(dao.courseNames().relocalizing(), dao.foodGroupNames()) { cn, fgn -> cn.toLocalizedNameMap() to fgn.toLocalizedNameMap() },
    ) { (characters, outfits, rules), (courseFoods, events, eventStops), (courseNames, foodGroupNames) ->
        RawSeedData(characters, outfits, rules, courseFoods, events, eventStops, courseNames, foodGroupNames)
    }

    private val resultsSettings = combine(
        resultsEnabled,
        weight,
        referenceCc,
        userStateDao.allBestResults(),
    ) { enabled, w, cc, results -> ResultsSettings(enabled, w, cc, results) }

    val uiState: StateFlow<ConsigliamiUiState> = combine(
        rawData,
        eventFilter,
        resultsSettings,
    ) { raw, filter, results ->
        val eventStopsByEvent = raw.eventStops.groupBy({ it.eventId }, { it.courseId })
        val events = raw.events
            .filter { filter == ConsigliamiEventFilter.BOTH || it.type == filter.toEventType() }
            .map {
                ConsigliamiEvent(it.id, it.type, localizedName(it.name, it.nameIt), it.order, eventStopsByEvent[it.id].orEmpty())
            }

        // bestRank(E, cc) (SPEC §6.3): solo il trofeo registrato alla cilindrata di riferimento,
        // nessun riporto da altre cilindrate (decisione dell'autore, vedi CLAUDE.md).
        val bestRankByEvent = results.bestResults
            .filter { it.cc == results.cc }
            .associate { it.eventId to it.rank }

        val ranked = ConsigliamiUseCase.compute(
            characters = raw.characters.map { ConsigliamiCharacter(it.id, it.rosterOrder, it.unlocked) },
            outfits = raw.outfits.map { ConsigliamiOutfit(it.id, it.characterId, it.owned) },
            rules = raw.rules.map { ConsigliamiRule(it.outfitId, it.foodGroupId) },
            standFoods = raw.courseFoods.map { ConsigliamiStandFood(it.courseId, it.foodGroupId) },
            events = events,
            resultsEnabled = results.enabled,
            weight = results.weight,
            bestRankForEvent = { eventId -> bestRankByEvent[eventId] },
        )

        ConsigliamiUiState(
            events = ranked,
            eventFilter = filter,
            resultsEnabled = results.enabled,
            weight = results.weight,
            referenceCc = results.cc,
            bestRankByEvent = bestRankByEvent,
            characterNames = raw.characters.associate { it.id to localizedName(it.name, it.nameIt) },
            characterImages = raw.characters.mapNotNull { c -> c.imageUrl?.let { c.id to it } }.toMap(),
            eventImages = raw.events.mapNotNull { e -> e.imageUrl?.let { e.id to it } }.toMap(),
            courseNames = raw.courseNames,
            foodGroupNames = raw.foodGroupNames,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConsigliamiUiState())

    fun onEventFilterChanged(filter: ConsigliamiEventFilter) {
        eventFilter.value = filter
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
}

private fun ConsigliamiEventFilter.toEventType(): EventType? = when (this) {
    ConsigliamiEventFilter.CUP -> EventType.CUP
    ConsigliamiEventFilter.RALLY -> EventType.RALLY
    ConsigliamiEventFilter.BOTH -> null
}

private fun List<IdNameIt>.toLocalizedNameMap(): Map<String, String> =
    associate { it.id to localizedName(it.name, it.nameIt) }
