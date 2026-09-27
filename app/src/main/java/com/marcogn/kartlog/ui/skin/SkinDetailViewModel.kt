package com.marcogn.kartlog.ui.skin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcogn.kartlog.data.local.dao.OutfitProgress
import com.marcogn.kartlog.data.local.dao.SkinDao
import com.marcogn.kartlog.data.local.dao.UserStateDao
import com.marcogn.kartlog.data.local.entity.CharacterUnlockEntity
import com.marcogn.kartlog.data.local.entity.OwnedOutfitEntity
import com.marcogn.kartlog.domain.model.localizedName
import com.marcogn.kartlog.domain.model.relocalizing
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SkinDetailUiState(
    val characterName: String = "",
    val characterImageUrl: String? = null,
    val ownedCount: Int = 0,
    val totalCount: Int = 0,
    val unlocked: Boolean = true,
    /** Solo i piloti da sbloccare hanno l'interruttore: quelli di base lo sono sempre. */
    val canToggleUnlock: Boolean = false,
    /** Come si sblocca, solo per i piloti non di base. */
    val unlockCriteria: String? = null,
    val outfits: List<OutfitProgress> = emptyList(),
)

@HiltViewModel
class SkinDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userStateDao: UserStateDao,
    skinDao: SkinDao,
) : ViewModel() {

    private val characterId: String = checkNotNull(savedStateHandle["characterId"])

    val uiState: StateFlow<SkinDetailUiState> = combine(
        skinDao.character(characterId).relocalizing(),
        skinDao.outfitsForCharacter(characterId),
        // Stato di sblocco scelto dall'utente, se c'è (SPEC §2.3).
        userStateDao.observeCharacterUnlock(characterId),
    ) { character, outfits, unlockedOrNull ->
        SkinDetailUiState(
            characterName = character?.let { localizedName(it.name, it.nameIt) } ?: "",
            characterImageUrl = character?.imageUrl,
            unlockCriteria = character?.unlockCriteria?.let { localizedName(it, character.unlockCriteriaIt) },
            ownedCount = outfits.count { it.owned },
            totalCount = outfits.size,
            // Piloti di base sempre sbloccati (come in SkinDao); gli altri: scelta dell'utente o bloccati.
            unlocked = character?.starter != false || unlockedOrNull == true,
            canToggleUnlock = character?.starter == false,
            outfits = outfits,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SkinDetailUiState())

    /** L'outfit di default non passa di qui: non è una riga spuntabile (SPEC §2.3). */
    fun onOutfitToggled(outfitId: String, owned: Boolean) {
        viewModelScope.launch {
            if (owned) userStateDao.markOutfitOwned(OwnedOutfitEntity(outfitId)) else userStateDao.markOutfitNotOwned(outfitId)
        }
    }

    fun onUnlockToggled(unlocked: Boolean) {
        viewModelScope.launch {
            userStateDao.setCharacterUnlock(CharacterUnlockEntity(characterId = characterId, unlocked = unlocked))
        }
    }
}
