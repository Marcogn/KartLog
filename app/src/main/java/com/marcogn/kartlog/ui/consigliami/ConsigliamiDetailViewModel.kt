package com.marcogn.kartlog.ui.consigliami

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcogn.kartlog.data.local.dao.ConsigliamiDao
import com.marcogn.kartlog.data.local.dao.IdImageUrl
import com.marcogn.kartlog.data.local.dao.IdNameIt
import com.marcogn.kartlog.data.local.dao.UserStateDao
import com.marcogn.kartlog.domain.consigliami.ConsigliamiCharacter
import com.marcogn.kartlog.domain.consigliami.ConsigliamiEvent
import com.marcogn.kartlog.domain.consigliami.ConsigliamiOutfit
import com.marcogn.kartlog.domain.consigliami.ConsigliamiRule
import com.marcogn.kartlog.domain.consigliami.ConsigliamiStandFood
import com.marcogn.kartlog.domain.consigliami.ConsigliamiUseCase
import com.marcogn.kartlog.domain.model.Cc
import com.marcogn.kartlog.domain.model.TrophyRank
import com.marcogn.kartlog.domain.model.localizedName
import com.marcogn.kartlog.domain.model.relocalizing
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ConsigliamiDetailUiState(
    val eventName: String = "",
    val eventImageUrl: String? = null,
    val characters: List<DetailCharacterUi> = emptyList(),
    /** Sola lettura (SPEC §2.5): trofeo registrato per ogni cilindrata. Si registra in Risultati. */
    val bestRankByCc: Map<Cc, TrophyRank> = emptyMap(),
)

@HiltViewModel
class ConsigliamiDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    dao: ConsigliamiDao,
    userStateDao: UserStateDao,
) : ViewModel() {

    private val eventId: String = checkNotNull(savedStateHandle["eventId"])

    val uiState: StateFlow<ConsigliamiDetailUiState> = combine(
        combine(dao.characters().relocalizing(), dao.outfits(), dao.rules()) { c, o, r -> Triple(c, o, r) },
        combine(dao.courseFoods(), dao.events(), dao.eventStops()) { cf, e, es -> Triple(cf, e, es) },
        combine(dao.outfitNames(), dao.courseNames(), dao.foodGroupNames(), dao.foodGroupImages()) { o, c, f, fi -> Names(o, c, f, fi) },
        userStateDao.bestResultsForEvent(eventId),
    ) { (characters, outfits, rules), (courseFoods, events, eventStops), names, results ->
        val eventEntity = events.firstOrNull { it.id == eventId }
        val courseIds = eventStops.filter { it.eventId == eventId }.map { it.courseId }
        val details = eventEntity?.let { entity ->
            ConsigliamiUseCase.detailFor(
                event = ConsigliamiEvent(entity.id, entity.type, localizedName(entity.name, entity.nameIt), entity.order, courseIds),
                characters = characters.map { ConsigliamiCharacter(it.id, it.rosterOrder, it.unlocked) },
                outfits = outfits.map { ConsigliamiOutfit(it.id, it.characterId, it.owned) },
                rules = rules.map { ConsigliamiRule(it.outfitId, it.foodGroupId) },
                standFoods = courseFoods.map { ConsigliamiStandFood(it.courseId, it.foodGroupId) },
            )
        }.orEmpty()

        ConsigliamiDetailUiState(
            eventName = eventEntity?.let { localizedName(it.name, it.nameIt) }.orEmpty(),
            eventImageUrl = eventEntity?.imageUrl,
            characters = detailCharactersUi(
                details = details,
                characterNames = characters.associate { it.id to localizedName(it.name, it.nameIt) },
                characterImages = characters.mapNotNull { c -> c.imageUrl?.let { c.id to it } }.toMap(),
                outfitNames = names.outfits.toLocalizedMap(),
                foodGroupNames = names.foodGroups.toLocalizedMap(),
                foodGroupImages = names.foodGroupImages.mapNotNull { f -> f.imageUrl?.let { f.id to it } }.toMap(),
                courseNames = names.courses.toLocalizedMap(),
            ),
            bestRankByCc = results.associate { it.cc to it.rank },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConsigliamiDetailUiState())
}

private data class Names(
    val outfits: List<IdNameIt>,
    val courses: List<IdNameIt>,
    val foodGroups: List<IdNameIt>,
    val foodGroupImages: List<IdImageUrl>,
)

private fun List<IdNameIt>.toLocalizedMap(): Map<String, String> = associate { it.id to localizedName(it.name, it.nameIt) }
