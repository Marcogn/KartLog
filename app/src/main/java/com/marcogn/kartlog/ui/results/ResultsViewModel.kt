package com.marcogn.kartlog.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcogn.kartlog.data.local.dao.ConsigliamiDao
import com.marcogn.kartlog.data.local.dao.UserStateDao
import com.marcogn.kartlog.data.local.entity.BestResultEntity
import com.marcogn.kartlog.domain.model.Cc
import com.marcogn.kartlog.domain.model.EventType
import com.marcogn.kartlog.domain.model.TrophyRank
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Una riga della schermata Risultati alla cilindrata selezionata.
 * [stored] è il trofeo registrato proprio a quella cilindrata; [effective] quello che vale davvero,
 * che può arrivare da una cilindrata superiore ([effectiveFrom], null se è quella selezionata).
 */
data class ResultRow(
    val eventId: String,
    val eventName: String,
    val stored: TrophyRank?,
    val effective: TrophyRank?,
    val effectiveFrom: Cc?,
)

data class ResultsUiState(
    val cc: Cc = Cc.CC_150,
    val cups: List<ResultRow> = emptyList(),
    val rallies: List<ResultRow> = emptyList(),
)

/** Schermata Risultati (SPEC §2.6): miglior trofeo per evento e cilindrata, un solo valore per coppia. */
@HiltViewModel
class ResultsViewModel @Inject constructor(
    dao: ConsigliamiDao,
    private val userStateDao: UserStateDao,
) : ViewModel() {

    private val cc = MutableStateFlow(Cc.CC_150)

    val uiState: StateFlow<ResultsUiState> = combine(
        dao.events(),
        userStateDao.allBestResults(),
        cc,
    ) { events, results, selectedCc ->
        val byEvent: Map<String, List<BestResultEntity>> = results.groupBy { it.eventId }
        val rows = events.sortedBy { it.order }.map { event ->
            val ranksByCc = byEvent[event.id].orEmpty().associate { it.cc to it.rank }
            val stored = ranksByCc[selectedCc]
            // Stesso criterio di effectiveRank(): cilindrate >= quella selezionata. A parità di
            // livello vince la più bassa, così "da 150cc" compare solo se aggiunge qualcosa.
            val source = ranksByCc
                .filterKeys { it.ordinal >= selectedCc.ordinal }
                .entries
                .sortedWith(compareByDescending<Map.Entry<Cc, TrophyRank>> { it.value.level }.thenBy { it.key.ordinal })
                .firstOrNull()
            (event.type to ResultRow(
                eventId = event.id,
                eventName = event.name,
                stored = stored,
                effective = source?.value,
                effectiveFrom = source?.key?.takeIf { it != selectedCc },
            ))
        }
        ResultsUiState(
            cc = selectedCc,
            cups = rows.filter { it.first == EventType.CUP }.map { it.second },
            rallies = rows.filter { it.first == EventType.RALLY }.map { it.second },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResultsUiState())

    fun onCcChanged(value: Cc) {
        cc.value = value
    }

    /** Sostituisce il trofeo registrato per (evento, cilindrata selezionata); null lo cancella. */
    fun onRankChanged(eventId: String, rank: TrophyRank?) {
        val selectedCc = cc.value
        viewModelScope.launch {
            if (rank == null) {
                userStateDao.deleteBestResult(eventId, selectedCc)
            } else {
                userStateDao.upsertBestResult(BestResultEntity(eventId, selectedCc, rank))
            }
        }
    }
}
