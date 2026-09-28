package com.marcogn.kartlog.ui.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcogn.kartlog.data.local.dao.MapDao
import com.marcogn.kartlog.data.local.dao.UserStateDao
import com.marcogn.kartlog.data.local.entity.ActivatedQuestionPanelEntity
import com.marcogn.kartlog.data.local.entity.CollectedMedallionEntity
import com.marcogn.kartlog.data.local.entity.CompletedPSwitchEntity
import com.marcogn.kartlog.data.seed.MapImageDto
import com.marcogn.kartlog.data.seed.SeedAssetLoader
import com.marcogn.kartlog.domain.model.MapPointType
import com.marcogn.kartlog.domain.model.relocalizing
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Un punto della mappa: [x]/[y] in frazione (0–1) dell'immagine. */
data class MapPoint(
    val type: MapPointType,
    val id: String,
    val index: Int,
    val x: Float,
    val y: Float,
    /** Pulsanti P: nome della missione (inglese); per gli altri null, il titolo è "tipo + numero". */
    val name: String?,
    val location: String?,
    /** Istruzioni di mkworld-checklist, solo in inglese. */
    val hint: String?,
    val youtubeId: String?,
    val done: Boolean,
)

data class MapCounter(val done: Int, val total: Int)

data class MapUiState(
    /** Solo i punti da disegnare, già filtrati. */
    val points: List<MapPoint> = emptyList(),
    /** Tutti i punti, per il popup (resta aperto anche se il punto viene nascosto dai filtri). */
    val all: List<MapPoint> = emptyList(),
    val counters: Map<MapPointType, MapCounter> = emptyMap(),
    val visibleTypes: Set<MapPointType> = MapPointType.entries.toSet(),
    val showDone: Boolean = true,
)

/** Filtri visivi della mappa: quali tipi di punto mostrare e se tenere quelli già fatti. */
internal fun filterPoints(all: List<MapPoint>, visibleTypes: Set<MapPointType>, showDone: Boolean): List<MapPoint> =
    all.filter { it.type in visibleTypes && (showDone || !it.done) }

@HiltViewModel
class MapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    mapDao: MapDao,
    private val userStateDao: UserStateDao,
    assets: SeedAssetLoader,
) : ViewModel() {

    val image: MapImageDto? = assets.readMapImage()

    /** Punto su cui centrare la mappa all'apertura (da "Mostra sulla mappa" nelle liste). */
    val focusId: String? = savedStateHandle["focusId"]

    private val visibleTypes = MutableStateFlow(
        savedStateHandle.get<String>("type")
            ?.let { name -> MapPointType.entries.firstOrNull { it.name == name } }
            ?.let { setOf(it) }
            ?: MapPointType.entries.toSet(),
    )
    private val showDone = MutableStateFlow(true)

    val uiState: StateFlow<MapUiState> = combine(
        mapDao.allPoints().relocalizing(),
        visibleTypes,
        showDone,
    ) { rows, types, done ->
        val all = rows.map { row ->
            MapPoint(
                type = MapPointType.valueOf(row.type),
                id = row.id,
                index = row.index,
                x = (row.x / 100.0).toFloat(),
                y = (row.y / 100.0).toFloat(),
                name = row.name,
                location = row.locationName,
                hint = row.hint,
                youtubeId = row.youtubeId,
                done = row.done,
            )
        }
        MapUiState(
            points = filterPoints(all, types, done),
            all = all,
            counters = all.groupBy { it.type }.mapValues { (_, list) -> MapCounter(list.count { it.done }, list.size) },
            visibleTypes = types,
            showDone = done,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())

    fun onTypeToggled(type: MapPointType) {
        visibleTypes.value = visibleTypes.value.let { if (type in it) it - type else it + type }
    }

    fun onShowDoneToggled() {
        showDone.value = !showDone.value
    }

    fun onDoneChanged(point: MapPoint, done: Boolean) {
        viewModelScope.launch {
            when (point.type) {
                MapPointType.MEDALLION ->
                    if (done) userStateDao.markMedallionCollected(CollectedMedallionEntity(point.id))
                    else userStateDao.markMedallionNotCollected(point.id)
                MapPointType.P_SWITCH ->
                    if (done) userStateDao.markPSwitchCompleted(CompletedPSwitchEntity(point.id))
                    else userStateDao.markPSwitchNotCompleted(point.id)
                MapPointType.QUESTION_PANEL ->
                    if (done) userStateDao.markQuestionPanelActivated(ActivatedQuestionPanelEntity(point.id))
                    else userStateDao.markQuestionPanelNotActivated(point.id)
            }
        }
    }
}
