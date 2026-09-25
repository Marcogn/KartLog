package com.marcogn.kartlog.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcogn.kartlog.data.local.dao.SeedDao
import com.marcogn.kartlog.data.local.dao.UserStateDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val ownedOutfits: Int = 0,
    val totalOutfits: Int = 0,
    val collectedMedallions: Int = 0,
    val totalMedallions: Int = 0,
    val completedPSwitches: Int = 0,
    val totalPSwitches: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    seedDao: SeedDao,
    userStateDao: UserStateDao,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        combine(seedDao.countOutfits(), userStateDao.countOwnedOutfits()) { total, owned -> total to owned },
        combine(seedDao.countPeachMedallions(), userStateDao.countCollectedMedallions()) { total, owned -> total to owned },
        combine(seedDao.countPSwitches(), userStateDao.countCompletedPSwitches()) { total, owned -> total to owned },
    ) { outfits, medallions, pSwitches ->
        HomeUiState(
            ownedOutfits = outfits.second,
            totalOutfits = outfits.first,
            collectedMedallions = medallions.second,
            totalMedallions = medallions.first,
            completedPSwitches = pSwitches.second,
            totalPSwitches = pSwitches.first,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
