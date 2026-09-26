package com.marcogn.kartlog.ui.medallions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcogn.kartlog.data.local.dao.MedallionsDao
import com.marcogn.kartlog.data.local.dao.UserStateDao
import com.marcogn.kartlog.data.seed.SeedAssetLoader
import com.marcogn.kartlog.domain.model.localizedName
import com.marcogn.kartlog.domain.model.relocalizing
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Un contatore per bioma: i medaglioni non hanno dettagli propri, solo quanti ce ne sono per regione. */
data class RegionMedallions(
    val regionId: String,
    val regionName: String,
    val collected: Int,
    val total: Int,
)

data class MedallionsUiState(
    val regions: List<RegionMedallions> = emptyList(),
    val totalCollected: Int = 0,
    val totalCount: Int = 0,
    val guideUrl: String? = null,
)

@HiltViewModel
class MedallionsViewModel @Inject constructor(
    medallionsDao: MedallionsDao,
    private val userStateDao: UserStateDao,
    assets: SeedAssetLoader,
) : ViewModel() {

    // Statico per l'intera sessione (viene da un asset, non cambia finché non c'è un reseed):
    // letto una sola volta invece che a ogni emissione del Flow qui sotto.
    private val guideUrl = assets.readSourceUrl("peach_medallions.json")

    val uiState: StateFlow<MedallionsUiState> = medallionsDao.regionCounters().relocalizing().map { rows ->
        MedallionsUiState(
            regions = rows.map {
                RegionMedallions(it.regionId, localizedName(it.regionName, it.regionNameIt), it.collected, it.total)
            },
            totalCollected = rows.sumOf { it.collected },
            totalCount = rows.sumOf { it.total },
            guideUrl = guideUrl,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedallionsUiState())

    fun onIncrement(regionId: String) {
        viewModelScope.launch { userStateDao.collectNextMedallion(regionId) }
    }

    fun onDecrement(regionId: String) {
        viewModelScope.launch { userStateDao.uncollectLastMedallion(regionId) }
    }

    fun onMarkAllRequested(regionId: String) {
        viewModelScope.launch { userStateDao.markAllMedallionsCollected(regionId) }
    }
}
