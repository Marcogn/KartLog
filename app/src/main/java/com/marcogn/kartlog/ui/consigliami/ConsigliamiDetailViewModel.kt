package com.marcogn.kartlog.ui.consigliami

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcogn.kartlog.data.local.dao.ConsigliamiDao
import com.marcogn.kartlog.domain.consigliami.CharacterDetail
import com.marcogn.kartlog.domain.consigliami.ConsigliamiCharacter
import com.marcogn.kartlog.domain.consigliami.ConsigliamiEvent
import com.marcogn.kartlog.domain.consigliami.ConsigliamiFoodCourse
import com.marcogn.kartlog.domain.consigliami.ConsigliamiOutfit
import com.marcogn.kartlog.domain.consigliami.ConsigliamiRule
import com.marcogn.kartlog.domain.consigliami.ConsigliamiUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ConsigliamiDetailUiState(
    val eventName: String = "",
    val details: List<CharacterDetail> = emptyList(),
    val characterNames: Map<String, String> = emptyMap(),
    val outfitNames: Map<String, String> = emptyMap(),
)

@HiltViewModel
class ConsigliamiDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    dao: ConsigliamiDao,
) : ViewModel() {

    private val eventId: String = checkNotNull(savedStateHandle["eventId"])
    private val includeNearby: Boolean = checkNotNull(savedStateHandle["includeNearby"])

    val uiState: StateFlow<ConsigliamiDetailUiState> = combine(
        combine(dao.characters(), dao.outfits(), dao.rules()) { c, o, r -> Triple(c, o, r) },
        combine(dao.foodCourses(), dao.events(), dao.eventStops()) { fc, e, es -> Triple(fc, e, es) },
        dao.outfitNames(),
    ) { (characters, outfits, rules), (foodCourses, events, eventStops), outfitNames ->
        val eventEntity = events.firstOrNull { it.id == eventId }
        val courseIds = eventStops.filter { it.eventId == eventId }.map { it.courseId }
        val details = eventEntity?.let { entity ->
            ConsigliamiUseCase.detailFor(
                event = ConsigliamiEvent(entity.id, entity.type, entity.name, entity.order, courseIds),
                characters = characters.map { ConsigliamiCharacter(it.id, it.rosterOrder, it.unlocked) },
                outfits = outfits.map { ConsigliamiOutfit(it.id, it.characterId, it.owned) },
                rules = rules.map { ConsigliamiRule(it.outfitId, it.foodGroupId) },
                foodCourses = foodCourses.map { ConsigliamiFoodCourse(it.foodGroupId, it.courseId, it.presence) },
                includeNearby = includeNearby,
            )
        }.orEmpty()

        ConsigliamiDetailUiState(
            eventName = eventEntity?.name.orEmpty(),
            details = details,
            characterNames = characters.associate { it.id to it.name },
            outfitNames = outfitNames.associate { it.id to it.name },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConsigliamiDetailUiState())
}
