package com.marcogn.kartlog.ui.skin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcogn.kartlog.data.local.dao.CharacterProgress
import com.marcogn.kartlog.data.local.dao.SkinDao
import com.marcogn.kartlog.data.local.dao.UserStateDao
import com.marcogn.kartlog.data.local.entity.CharacterUnlockEntity
import com.marcogn.kartlog.domain.model.localizedName
import com.marcogn.kartlog.domain.model.relocalizing
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SkinSortMode { ROSTER, ALPHABETICAL, COMPLETION }
enum class SkinFilterMode { ALL, INCOMPLETE }

data class SkinListUiState(
    val characters: List<CharacterProgress> = emptyList(),
    val sortMode: SkinSortMode = SkinSortMode.ROSTER,
    val filterMode: SkinFilterMode = SkinFilterMode.ALL,
)

@HiltViewModel
class SkinListViewModel @Inject constructor(
    skinDao: SkinDao,
    private val userStateDao: UserStateDao,
) : ViewModel() {

    private val sortMode = MutableStateFlow(SkinSortMode.ROSTER)
    private val filterMode = MutableStateFlow(SkinFilterMode.ALL)

    val uiState: StateFlow<SkinListUiState> = combine(
        skinDao.charactersWithProgress().relocalizing(),
        sortMode,
        filterMode,
    ) { characters, sort, filter ->
        val ordered = orderCharacters(characters, sort, filter)
        SkinListUiState(characters = ordered, sortMode = sort, filterMode = filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SkinListUiState())

    fun onSortModeSelected(mode: SkinSortMode) {
        sortMode.value = mode
    }

    fun onFilterModeSelected(mode: SkinFilterMode) {
        filterMode.value = mode
    }

    /** Sblocco di un pilota da sbloccare, dal popup (quelli di base sono sempre sbloccati). */
    fun onUnlockToggled(characterId: String, unlocked: Boolean) {
        viewModelScope.launch {
            userStateDao.setCharacterUnlock(CharacterUnlockEntity(characterId = characterId, unlocked = unlocked))
        }
    }
}

/** Ordine e filtro della lista Personaggi (funzione pura, testata in `SkinOrderTest`). */
internal fun orderCharacters(
    characters: List<CharacterProgress>,
    sort: SkinSortMode,
    filter: SkinFilterMode,
): List<CharacterProgress> {
    // Chi ha completato quello che c'era da fare va in fondo, qualunque sia l'ordinamento
    // (SPEC §2.3); con "Incompleti" sparisce. I piloti di base senza outfit non hanno nulla da
    // fare: stanno nel gruppo in alto, ordinati come gli altri (prima finivano tutti in fondo,
    // segnalazione dell'autore), e "Incompleti" non li mostra.
    val (open, done) = characters.partition { !it.isDone }
    val comparator = comparatorFor(sort)
    return if (filter == SkinFilterMode.INCOMPLETE) {
        open.filter { it.hasProgress }.sortedWith(comparator)
    } else {
        open.sortedWith(comparator) + done.sortedWith(comparator)
    }
}

private fun comparatorFor(mode: SkinSortMode): Comparator<CharacterProgress> = when (mode) {
    SkinSortMode.ROSTER -> compareBy { it.rosterOrder }
    SkinSortMode.ALPHABETICAL -> compareBy { localizedName(it.name, it.nameIt) }
    SkinSortMode.COMPLETION -> compareByDescending<CharacterProgress> {
        when {
            it.hasOutfits -> it.ownedOutfits.toDouble() / it.totalOutfits
            !it.isUnlockable -> -1.0  // nulla da completare: dopo chi ha un avanzamento
            it.unlocked -> 1.0
            else -> 0.0
        }
    }.thenBy { it.rosterOrder }
}
