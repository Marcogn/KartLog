package com.marcogn.kartlog.ui.medallions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcogn.kartlog.data.local.dao.MedallionsDao
import com.marcogn.kartlog.data.local.dao.UserStateDao
import com.marcogn.kartlog.data.local.entity.CollectedMedallionEntity
import com.marcogn.kartlog.data.seed.SeedAssetLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MedallionRow(val id: String, val index: Int, val collected: Boolean)

data class RegionMedallions(
    val regionId: String,
    val regionName: String,
    val regionOrder: Int,
    val collected: Int,
    val total: Int,
    val medallions: List<MedallionRow>,
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

    val uiState: StateFlow<MedallionsUiState> = medallionsDao.allMedallions().map { all ->
        val regions = all.groupBy { it.regionId }.map { (_, rows) ->
            val first = rows.first()
            RegionMedallions(
                regionId = first.regionId,
                regionName = first.regionName,
                regionOrder = first.regionOrder,
                collected = rows.count { it.collected },
                total = rows.size,
                medallions = rows.sortedBy { it.index }.map { MedallionRow(it.medallionId, it.index, it.collected) },
            )
        }.sortedBy { it.regionOrder }
        MedallionsUiState(
            regions = regions,
            totalCollected = all.count { it.collected },
            totalCount = all.size,
            guideUrl = guideUrl,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedallionsUiState())

    fun onMedallionToggled(medallionId: String, collected: Boolean) {
        viewModelScope.launch {
            if (collected) {
                userStateDao.markMedallionCollected(CollectedMedallionEntity(medallionId))
            } else {
                userStateDao.markMedallionNotCollected(medallionId)
            }
        }
    }

    fun onMarkAllRequested(regionId: String) {
        viewModelScope.launch { userStateDao.markAllMedallionsCollected(regionId) }
    }
}
