package com.marcogn.kartlog.ui.medallions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcogn.kartlog.data.local.NOTICE_MEDALLION_COUNTS_RESET
import com.marcogn.kartlog.data.local.dao.MedallionRow
import com.marcogn.kartlog.data.local.dao.MedallionsDao
import com.marcogn.kartlog.data.local.dao.UserStateDao
import com.marcogn.kartlog.data.local.entity.CollectedMedallionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MedallionsUiState(
    val medallions: List<MedallionRow> = emptyList(),
    val totalCollected: Int = 0,
    /**
     * Monete segnate per bioma prima della mappa, che la migrazione v6 -> v7 non ha potuto convertire
     * (vedi `MIGRATION_6_7`): se non è null si mostra l'avviso, una volta sola.
     */
    val resetNoticeCount: Int? = null,
)

@HiltViewModel
class MedallionsViewModel @Inject constructor(
    medallionsDao: MedallionsDao,
    private val userStateDao: UserStateDao,
) : ViewModel() {

    val uiState: StateFlow<MedallionsUiState> = combine(
        medallionsDao.allMedallions(),
        userStateDao.observeNotice(NOTICE_MEDALLION_COUNTS_RESET),
    ) { rows, notice ->
        MedallionsUiState(medallions = rows, totalCollected = rows.count { it.collected }, resetNoticeCount = notice)
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

    fun onResetNoticeDismissed() {
        viewModelScope.launch { userStateDao.dismissNotice(NOTICE_MEDALLION_COUNTS_RESET) }
    }
}
