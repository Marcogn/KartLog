package com.marcogn.kartlog.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcogn.kartlog.data.local.dao.SeedDao
import com.marcogn.kartlog.data.local.dao.UserStateDao
import com.marcogn.kartlog.domain.model.Cc
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val ownedOutfits: Int = 0,
    val totalOutfits: Int = 0,
    val completedPSwitches: Int = 0,
    val totalPSwitches: Int = 0,
    /** Coppie (evento, cilindrata) con un trofeo, su eventi × cilindrate. */
    val trophies: Int = 0,
    val totalTrophies: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    seedDao: SeedDao,
    userStateDao: UserStateDao,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        combine(seedDao.countOutfits(), userStateDao.countOwnedOutfits()) { total, owned -> total to owned },
        combine(seedDao.countPSwitches(), userStateDao.countCompletedPSwitches()) { total, owned -> total to owned },
        combine(seedDao.countEvents(), userStateDao.countTrophies()) { events, trophies -> events * Cc.entries.size to trophies },
    ) { outfits, pSwitches, trophies ->
        HomeUiState(
            ownedOutfits = outfits.second,
            totalOutfits = outfits.first,
            completedPSwitches = pSwitches.second,
            totalPSwitches = pSwitches.first,
            trophies = trophies.second,
            totalTrophies = trophies.first,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
