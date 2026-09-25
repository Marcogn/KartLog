package com.marcogn.kartlog.ui.pswitches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcogn.kartlog.data.local.dao.PSwitchesDao
import com.marcogn.kartlog.data.local.dao.UserStateDao
import com.marcogn.kartlog.data.local.entity.CompletedPSwitchEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PSwitchRow(val id: String, val name: String, val completed: Boolean)

data class LocationGroup(val locationName: String, val pSwitches: List<PSwitchRow>)

data class RegionPSwitches(
    val regionId: String,
    val regionName: String,
    val regionOrder: Int,
    val completed: Int,
    val total: Int,
    val locations: List<LocationGroup>,
)

data class PSwitchesUiState(
    val regions: List<RegionPSwitches> = emptyList(),
    val totalCompleted: Int = 0,
    val totalCount: Int = 0,
    val query: String = "",
    /** SPEC §2.4: "Dati non ancora disponibili" se p_switches.json manca dal seed. */
    val dataAvailable: Boolean = true,
)

@HiltViewModel
class PSwitchesViewModel @Inject constructor(
    pSwitchesDao: PSwitchesDao,
    private val userStateDao: UserStateDao,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<PSwitchesUiState> = combine(pSwitchesDao.allPSwitches(), query) { all, q ->
        val filtered = if (q.isBlank()) all else all.filter { it.name.contains(q, ignoreCase = true) }
        val regions = filtered.groupBy { it.regionId }.map { (_, regionRows) ->
            val first = regionRows.first()
            val locations = regionRows.groupBy { it.locationName }.map { (locationName, rows) ->
                LocationGroup(
                    locationName = locationName,
                    pSwitches = rows.sortedBy { it.index }.map { PSwitchRow(it.pSwitchId, it.name, it.completed) },
                )
            }.sortedBy { it.locationName }
            RegionPSwitches(
                regionId = first.regionId,
                regionName = first.regionName,
                regionOrder = first.regionOrder,
                completed = regionRows.count { it.completed },
                total = regionRows.size,
                locations = locations,
            )
        }.sortedBy { it.regionOrder }
        PSwitchesUiState(
            regions = regions,
            // Conteggio globale sempre sul totale reale, non sui risultati filtrati dalla ricerca.
            totalCompleted = all.count { it.completed },
            totalCount = all.size,
            query = q,
            dataAvailable = all.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PSwitchesUiState())

    fun onQueryChanged(query: String) {
        this.query.value = query
    }

    fun onPSwitchToggled(pSwitchId: String, completed: Boolean) {
        viewModelScope.launch {
            if (completed) {
                userStateDao.markPSwitchCompleted(CompletedPSwitchEntity(pSwitchId))
            } else {
                userStateDao.markPSwitchNotCompleted(pSwitchId)
            }
        }
    }

    fun onMarkAllRequested(regionId: String) {
        viewModelScope.launch { userStateDao.markAllPSwitchesCompleted(regionId) }
    }
}
