package com.marcogn.kartlog.ui.skin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcogn.kartlog.data.local.dao.CharacterProgress
import com.marcogn.kartlog.data.local.dao.SkinDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class SkinSortMode { ROSTER, ALPHABETICAL, COMPLETION }
enum class SkinFilterMode { ALL, INCOMPLETE }

data class SkinListUiState(
    val characters: List<CharacterProgress> = emptyList(),
    val sortMode: SkinSortMode = SkinSortMode.ROSTER,
    val filterMode: SkinFilterMode = SkinFilterMode.ALL,
)

@HiltViewModel
class SkinListViewModel @Inject constructor(skinDao: SkinDao) : ViewModel() {

    private val sortMode = MutableStateFlow(SkinSortMode.ROSTER)
    private val filterMode = MutableStateFlow(SkinFilterMode.ALL)

    val uiState: StateFlow<SkinListUiState> = combine(
        skinDao.charactersWithProgress(),
        sortMode,
        filterMode,
    ) { characters, sort, filter ->
        // Chi ha ottenuto tutti gli outfit si attenua e va in fondo, qualunque sia
        // l'ordinamento scelto (SPEC §2.3); con il filtro "Incompleti" sparisce del tutto.
        val (incomplete, complete) = characters.partition { !it.isComplete }
        val comparator = comparatorFor(sort)
        val ordered = incomplete.sortedWith(comparator) +
            if (filter == SkinFilterMode.INCOMPLETE) emptyList() else complete.sortedWith(comparator)
        SkinListUiState(characters = ordered, sortMode = sort, filterMode = filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SkinListUiState())

    fun onSortModeSelected(mode: SkinSortMode) {
        sortMode.value = mode
    }

    fun onFilterModeSelected(mode: SkinFilterMode) {
        filterMode.value = mode
    }

    private fun comparatorFor(mode: SkinSortMode): Comparator<CharacterProgress> = when (mode) {
        SkinSortMode.ROSTER -> compareBy { it.rosterOrder }
        SkinSortMode.ALPHABETICAL -> compareBy { it.name }
        SkinSortMode.COMPLETION -> compareByDescending<CharacterProgress> {
            if (it.totalOutfits == 0) 0.0 else it.ownedOutfits.toDouble() / it.totalOutfits
        }.thenBy { it.rosterOrder }
    }
}
