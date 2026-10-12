package com.marcogn.kartlog.ui.update

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcogn.kartlog.data.update.UpdateManager
import com.marcogn.kartlog.domain.update.UpdateCheckResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UpdateCheckUiState(
    val isChecking: Boolean = false,
    /**
     * Outcome of the last "Check for updates" tap; `null` before the first one. Resta il risultato, non
     * un testo: lo traduce la schermata con la lingua corrente (il ViewModel sopravvive al cambio lingua).
     */
    val result: UpdateCheckResult? = null,
)

@HiltViewModel
class UpdateCheckViewModel @Inject constructor(
    private val updateManager: UpdateManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UpdateCheckUiState())
    val uiState: StateFlow<UpdateCheckUiState> = _uiState.asStateFlow()

    /**
     * Ignores the configured frequency (works with Never too). An available update also opens the
     * app-wide dialog, since `UpdateManager` moves to `Available`; here only the line below the button.
     */
    fun onCheckNow() {
        if (_uiState.value.isChecking) return
        _uiState.update { it.copy(isChecking = true, result = null) }
        viewModelScope.launch {
            val result = updateManager.checkNow()
            if (result is UpdateCheckResult.Failed) Log.w("UpdateCheckViewModel", "Update check failed: ${result.failure}")
            _uiState.update { it.copy(isChecking = false, result = result) }
        }
    }
}
