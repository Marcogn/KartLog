package com.marcogn.kartlog.ui.panels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcogn.kartlog.data.local.dao.QuestionPanelRow
import com.marcogn.kartlog.data.local.dao.QuestionPanelsDao
import com.marcogn.kartlog.data.local.dao.UserStateDao
import com.marcogn.kartlog.data.local.entity.ActivatedQuestionPanelEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class QuestionPanelsViewModel @Inject constructor(
    panelsDao: QuestionPanelsDao,
    private val userStateDao: UserStateDao,
) : ViewModel() {

    val panels: StateFlow<List<QuestionPanelRow>> =
        panelsDao.allPanels().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onPanelToggled(panelId: String, activated: Boolean) {
        viewModelScope.launch {
            if (activated) {
                userStateDao.markQuestionPanelActivated(ActivatedQuestionPanelEntity(panelId))
            } else {
                userStateDao.markQuestionPanelNotActivated(panelId)
            }
        }
    }
}
